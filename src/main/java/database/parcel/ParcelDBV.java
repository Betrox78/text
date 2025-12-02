package database.parcel;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import database.boardingpass.BoardingPassDBV;
import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.configs.GeneralConfigDBV;
import database.customers.CustomerDBV;
import database.money.PaybackDBV;
import database.money.PaymentDBV;
import database.parcel.enums.PACKAGE_STATUS;
import database.parcel.enums.PARCELPACKAGETRACKING_STATUS;
import database.parcel.enums.PARCEL_STATUS;
import database.parcel.handlers.GuiappDBV.Exchange;
import database.parcel.handlers.GuiappDBV.ExchangeV2;
import database.parcel.handlers.GuiappDBV.ExchangeV3;
import database.parcel.handlers.ParcelDBV.*;
import database.promos.PromosDBV;
import database.promos.enums.SERVICES;
import database.routes.ScheduleRouteDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import net.sf.jasperreports.engine.*;
import org.json.JSONObject;
import service.commons.Constants;
import utils.UtilsDate;
import utils.UtilsID;
import utils.UtilsMoney;
import utils.UtilsValidation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static database.boardingpass.BoardingPassDBV.SCHEDULE_ROUTE_DESTINATION_ID;
import static database.configs.GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD;
import static database.promos.PromosDBV.DISCOUNT;
import static database.promos.PromosDBV.*;
import static java.util.stream.Collectors.toList;
import static service.commons.Constants.*;
import static utils.UtilsDate.*;
import static utils.UtilsValidation.MISSING_REQUIRED_VALUE;

public class ParcelDBV extends DBVerticle {

    public enum CANCEL_TYPE {
        FAST_CANCEL,
        END_CANCEL,
        REWORK,
        RETURN
    }

    public enum CANCEL_RESPONSABLE {
        CUSTOMER,
        COMPANY,
        OTHERS
    }

    public static final String REGISTER = "ParcelDBV.register";
    public static final String DELIVER = "ParcelDBV.deliver";
    public static final String CHECK_TRACKING_CODE = "ParcelDBV.checkTrackingCode";
    public static final String GET_PARCEL_BY_TRACKING_CODE = "ParcelsPackagesDBV.getParcelByTrackingCode";
    public static final String CANCEL_PARCEL_PACKAGE_BY_ID = "ParcelsPackagesDBV.cancelParcelById";
    public static final String WAS_PRINTED = "ParcelDBV.updatePrinted";
    public static final String GET_PACKAGES = "ParcelDBV.getPackages";
    public static final String GET_PACKAGES_SUMMARY = "ParcelDBV.getPackagesSummary";
    public static final String GET_PETSSIZES = "ParcelDBV.getSizes";
    public static final String SCANNING_PACKAGES_REPORT = "ParcelDBV.scanningPackagesReport";
    public static final String PARCEL_ADVANCED_SEARCH = "ParcelDBV.advancedSearch";
    public static final String UNREGISTERED_PACKAGES_REPORT = "ParcelDBV.unregisteredPackageReport";
    public static final String PARCEL_STOCK_REPORT = "ParcelDBV.stockReport";
    public static final String TRANSIT_PACKAGES_REPORT = "ParcelDBV.transitPackageReport";
    public static final String TRANSIT_PACKAGES_REPORT_TOTALS = "ParcelDBV.transitTotalsPackageReport";
    public static final String PARCEL_SALES_REPORT = "ParcelDBV.salesReport";
    public static final String PARCEL_SALES_REPORT_FECHA_INGRESOS = "ParcelDBV.salesReport_Fecha_Ingresos"; //movdev
    public static final String CONTINGENCY_REPORT = "ParcelDBV.contingencyReport";
    public static final String CANCEL_PARCEL_REPORT = "ParcelDBV.cancelParcelReport";
    public static final String PARCEL_PRINT = "ParcelDBV.getParcelPrint";
    public static final String PARCEL_ARRIVAL_CONTINGENCY = "ParcelDBV.arrivalContingency";
    public static final String PARCEL_SALES_MONTH = "ParcelDBV.salesMonth"; //WEBDEV
    public static final String PARCEL_COMMERCIAL_PROMISE_REPORT = "ParcelDBV.commercialPromiseReport";
    public static final String PARCEL_TIMELY_DELIVERY_DETAILS_REPORT = "ParcelDBV.timelyDeliveryDetailsReport";
    public static final String PARCEL_GLOBAL_DEMEANOR_COMMERCIAL_PROMISE_REPORT = "ParcelDBV.globalDemeanorCommercialPromiseReport";
    public static final String PARCEL_COBRANZA_REPORT = "ParcelDBV.salesCobranzaGen";
    public static final String PARCEL_SALES_REPORT_GEN = "ParcelDBV.salesReportGen";
    public static final String REGISTER_POSTAL_CODE = "ParcelDBV.registerPostalCode";
    public static final String GET_VALID_POSTAL_CODES = "ParcelDBV.searchValidCodes";
    public static final String GET_BRANCHOFFICES_W_PARCEL_COVERAGE = "ParcelDBV.branchofficesWParcelCoverage";
    public static final String GET_VALID_POSTAL_CODES_V2 = "ParcelDBV.searchValidCodesV2";
    public static final String ZIP_CODE_IS_ON_COVERAGE = "ParcelDBV.zipCodeIsOnCoverage";
    public static final String GENERATE_PDF = "ParcelDBV.generatePDF";
    public static final String GENERATE_PDF_PREPAID_SITE = "ParcelDBV.generatePDFPrepaidSite";
    public static final String ACTION_UPDATE_POSTAL_CODE = "ParcelDBV.updatePostalCode";
    public static final String ACCUMULATED_PARCEL_BY_ADVISER_REPORT = "ParcelDBV.accumulatedParcelByAdviserReport";
    public static final String ACCUMULATED_PARCEL_BY_ADVISER_REPORT_DETAIL = "ParcelDBV.accumulatedParcelByAdviserReportDetail";
    public static final String REGISTER_V2 = "ParcelDBV.registerV2";
    public static final String REGISTER_PARTNER_V2 = "ParcelDBV.registerPartnerV2";
    public static final String EXCHANGE = "ParcelDBV.exchange";
    public static final String ACTION_GET_PROMISE_DELIVERY_DATE = "ParcelDBV.getPromiseDeliveryDate";
    public static final String ACTION_GET_PENDING_COLLECT = "ParcelDBV.getPendingCollect";
    public static final String ACTION_TO_COLLECTING = "ParcelDBV.toCollecting";
    public static final String ACTION_TO_COLLECTED = "ParcelDBV.toCollected";
    public static final String ACTION_TO_DOCUMENTED = "ParcelDBV.toDocumented";
    public static final String ACTION_GET_EXTENDED_TRACKING = "ParcelDBV.getExtendedTracking";
    public static final String ACTION_ORIGIN_ARRIVAL_CONTINGENCY = "ParcelDBV.originArrivalContingency";
    public static final String ACTION_EXCHANGE_V2 = "ParcelDBV.exchangeV2";
    public static final String ACTION_REGISTER_V3 = "ParcelDBV.registerV3";
    public static final String ACTION_EXCHANGE_V3 = "ParcelDBV.exchangeV3";
    public static final String ACTION_SIGNATURE_DELIVERIES_REPORT = "ParcelDBV.signatureDeliveryReport";
    public static final String ACTION_SIGNATURE_DELIVERIES_DETAIL_REPORT = "ParcelDBV.signatureDeliveryReportDetail";

    public static final String PAYS_SENDER = "pays_sender";
    public static final String PARENT_ID = "parent_id";
    public static final String PARCELS_CANCEL_REASON_ID = "parcels_cancel_reason_id";
    public static final String PAYMENT_CONDITION = "payment_condition";
    public static final String PARCEL_PACKAGES = "parcel_packages";
    public static final String PARCEL_PACKINGS = "parcel_packings";
    public static final String PARCEL_ID = "parcel_id";
    public static final String TICKET_ID = "ticket_id";
    public static final String REWORK_SCHEDULE_ROUTE_DESTINATION_ID = "rework_schedule_route_destination_id";
    public static final String REISSUE = "reissue";
    public static final String REWORK = "rework";
    public static final String WITHOUT_FREIGHT = "without_freight";
    public static final String CUMULATIVE_COST = "cumulative_cost";
    public static final String CANCEL_CODE = "cancel_code";
    public static final String ADDRESSEE_ID = "addressee_id";
    public static final String ADDRESSEE_NAME = "addressee_name";
    public static final String ADDRESSEE_LAST_NAME = "addressee_last_name";
    public static final String ADDRESSEE_PHONE = "addressee_phone";
    public static final String ADDRESSEE_EMAIL = "addressee_email";
    public static final String ADDRESSEE_ZIP_CODE = "addressee_zip_code";
    public static final String ADDRESSEE_ADDRESS_ID = "addressee_address_id";
    public static final String WAYBILL = "waybill";
    public static final String DELIVERED_AT = "delivered_at";
    public static final String PROMISE_DELIVERY_DATE = "promise_delivery_date";
    private static final Integer MAX_LIMIT = 100;
    public static final String GET_LETTER_PORTE_COMPLEMENT = "ParcelDBV.getLettersPorteComplement";
    public static final String ZIP_CODE = "zip_code";
    @Override
    public String getTableName() {
        return "parcels";
    }

    RegisterV2 registerV2;
    RegisterV3 registerV3;
    Exchange exchange;
    Cancel cancel;
    Deliver deliver;
    PromiseDeliveryDate promiseDeliveryDate;
    PendingCollect pendingCollect;
    Collecting collecting;
    Collected collected;
    Documented documented;
    ExtendedTracking extendedTracking;
    OriginArrivalContingency originArrivalContingency;
    CommercialPromiseReport commercialPromiseReport;
    TimeDeliveryDetailsReport timeDeliveryDetailsReport;
    ExchangeV2 exchangeV2;
    ExchangeV3 exchangeV3;
    GlobalCommercialPromiseReport globalCommercialPromiseReport;
    ArrivalContingency arrivalContingency;
    SignatureDeliveriesReport signatureDeliveriesReport;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        this.registerV2 = new RegisterV2(this);
        this.registerV3 = new RegisterV3(this);
        this.exchange = new Exchange(this);
        this.cancel = new Cancel(this);
        this.deliver = new Deliver(this);
        this.promiseDeliveryDate = new PromiseDeliveryDate(this);
        this.pendingCollect = new PendingCollect(this);
        this.collecting = new Collecting(this);
        this.collected = new Collected(this);
        this.documented = new Documented(this);
        this.extendedTracking = new ExtendedTracking(this);
        this.originArrivalContingency = new OriginArrivalContingency(this);
        this.commercialPromiseReport = new CommercialPromiseReport(this);
        this.timeDeliveryDetailsReport = new TimeDeliveryDetailsReport(this);
        this.exchangeV2 = new ExchangeV2(this);
        this.globalCommercialPromiseReport = new GlobalCommercialPromiseReport(this);
        this.arrivalContingency = new ArrivalContingency(this);
        this.exchangeV3 = new ExchangeV3(this);
        this.signatureDeliveriesReport = new SignatureDeliveriesReport(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case REGISTER:
                this.register(message);
                break;
            case DELIVER:
                this.deliver.handle(message);
                break;
            case CHECK_TRACKING_CODE:
                this.checkTrackingCode(message);
                break;
            case GET_PARCEL_BY_TRACKING_CODE :
                getParcelByTrackingCode(message);
                break;
            case CANCEL_PARCEL_PACKAGE_BY_ID:
                cancel.handle(message);
                break;
            case WAS_PRINTED:
                updatePrinted(message);
                break;
            case GET_PACKAGES:
                getPackages(message);
                break;
            case GET_PACKAGES_SUMMARY:
                getPackagesSummary(message);
                break;
            case GET_PETSSIZES:
                this.getSizes(message);
                break;
            case SCANNING_PACKAGES_REPORT:
                this.scanningPackagesReport(message);
                break;
            case PARCEL_ADVANCED_SEARCH:
                this.advancedSearch(message);
                break;
            case UNREGISTERED_PACKAGES_REPORT:
                this.unregisteredPackageReport(message);
                break;
            case PARCEL_STOCK_REPORT:
                this.stockReport(message);
                break;
            case PARCEL_SALES_REPORT_FECHA_INGRESOS: //movdev
                this.salesReport_Fecha_Ingresos(message);
                break;
            case TRANSIT_PACKAGES_REPORT:
                this.transitPackageReport(message);
                break;
            case PARCEL_SALES_REPORT:
                this.salesReport(message);
                break;
            case TRANSIT_PACKAGES_REPORT_TOTALS:
                this.transitTotalsPackageReport(message);
                break;
            case CANCEL_PARCEL_REPORT:
                this.cancelParcelReport(message);
                break;
            case PARCEL_PRINT:
                this.getParcelPrint(message);
                break;
            case PARCEL_ARRIVAL_CONTINGENCY:
                this.arrivalContingency.handle(message);
                break;
            case PARCEL_SALES_MONTH: //webdev
                this.salesMonth(message);
                break;
            case CONTINGENCY_REPORT:
                this.contingencyReport(message);
                break;
            case PARCEL_COMMERCIAL_PROMISE_REPORT:
                this.commercialPromiseReport.handle(message);
                break;
            case PARCEL_TIMELY_DELIVERY_DETAILS_REPORT:
                this.timeDeliveryDetailsReport.handle(message);
                break;
            case PARCEL_COBRANZA_REPORT:
                this.salesCobranzaGen(message);
                break;
            case PARCEL_SALES_REPORT_GEN:
                this.salesReportGen(message);
                break;
            case PARCEL_GLOBAL_DEMEANOR_COMMERCIAL_PROMISE_REPORT:
                this.globalCommercialPromiseReport.handle(message);
                break;
            case REGISTER_POSTAL_CODE:
                this.registerPostalCode(message);
                break;
            case GET_VALID_POSTAL_CODES:
                this.searchValidCodes(message);
                break;
            case GET_BRANCHOFFICES_W_PARCEL_COVERAGE:
                this.branchofficesWParcelCoverage(message);
                break;
            case GET_VALID_POSTAL_CODES_V2:
                this.searchValidCodesV2(message);
                break;
            case ACTION_UPDATE_POSTAL_CODE:
                this.updatePostalCode(message);
                break;
            case GET_LETTER_PORTE_COMPLEMENT:
                this.getLettersPorteComplement(message);
                break;
            case GENERATE_PDF:
                this.generatePDF(message);
                break;
            case GENERATE_PDF_PREPAID_SITE:
                this.generatePDFPrepaidSite(message);
                break;
            case ACCUMULATED_PARCEL_BY_ADVISER_REPORT:
                this.accumulatedParcelByAdviserReport(message);
                break;
            case ACCUMULATED_PARCEL_BY_ADVISER_REPORT_DETAIL:
                this.accumulatedParcelByAdviserReportDetail(message);
                break;
            case REGISTER_V2:
            case REGISTER_PARTNER_V2:
                this.registerV2.handle(message);
                break;
            case EXCHANGE:
                this.exchange.handle(message);
                break;
            case ZIP_CODE_IS_ON_COVERAGE:
                this.zipCodeIsOnCoverage(message);
                break;
            case ACTION_GET_PROMISE_DELIVERY_DATE:
                this.promiseDeliveryDate.handle(message);
                break;
            case ACTION_GET_PENDING_COLLECT:
                this.pendingCollect.handle(message);
                break;
            case ACTION_TO_COLLECTING:
                this.collecting.handle(message);
                break;
            case ACTION_TO_COLLECTED:
                this.collected.handle(message);
                break;
            case ACTION_TO_DOCUMENTED:
                this.documented.handle(message);
                break;
            case ACTION_GET_EXTENDED_TRACKING:
                this.extendedTracking.handle(message);
                break;
            case ACTION_ORIGIN_ARRIVAL_CONTINGENCY:
                this.originArrivalContingency.handle(message);
                break;
            case ACTION_EXCHANGE_V2:
                this.exchangeV2.handle(message);
                break;
            case ACTION_REGISTER_V3:
                this.registerV3.handle(message);
                break;
            case ACTION_EXCHANGE_V3:
                this.exchangeV3.handle(message);
                break;
            case ACTION_SIGNATURE_DELIVERIES_REPORT:
                this.signatureDeliveriesReport.handle(message);
                break;
            case ACTION_SIGNATURE_DELIVERIES_DETAIL_REPORT:
                this.signatureDeliveriesReport.detail(message);
                break;
        }
    }

    private void getSizes(Message<JsonObject> message){
        JsonObject o = message.body();
        JsonArray params = new JsonArray().add(o.getInteger("id"));
        this.dbClient.queryWithParams(QUERY_GET_PETSSIZES, params, handler->{
            try{
                if (handler.failed()){
                    throw handler.cause();
                }
                if(handler.result().getNumRows()>0){
                    message.reply(new JsonArray().add(handler.result().getRows().get(0)));
                }else{
                    message.reply(null);
                }
            } catch (Throwable t){
                reportQueryError(message, t);
            }
        });
    }

    private CompletableFuture<JsonObject>  validCodeGuiapp(SQLConnection conn, JsonObject parcel,JsonObject guiapp){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            JsonObject packageGuiapp=parcel;
            JsonArray params = new JsonArray().add(guiapp.getString("guiapp_code"));

            String query=GET_CODE_GUIAPP;
            conn.queryWithParams(query,params, reply ->{
                try{
                    if(reply.succeeded()){
                        List<JsonObject> result = reply.result().getRows();
                        future.complete(new JsonObject().put("data",result));

                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch(Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    @Deprecated
    private void register(Message<JsonObject> message) {
        // Nota: Falta validar tanto rango de paquetes, como vlidacion de codigo de guiapp que se canjean y descontar las guias usadas  fecha:11/11/2021
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject parcel = message.body().copy();
            JsonArray codeGuiapp;
            if(parcel.containsKey("isGuiappCanje")){
                parcel.remove(SERVICE);
                if(parcel.getBoolean("isGuiappCanje")){
                    try {
                        codeGuiapp = parcel.getJsonArray("codesGuiapp");
                        List<CompletableFuture<JsonObject>> validCodeGuiappTasks = new ArrayList<>();
                        int parcel_packages_size= parcel.getJsonArray("parcel_packages").size();
                        for (int i = 0; i < codeGuiapp.size(); i++) {
                            for (int x = 0; x <parcel_packages_size ; x++) {
                                if(parcel.getJsonArray("parcel_packages").getJsonObject(x).containsKey("guiapp_code")){
                                    if (parcel.getJsonArray("parcel_packages").getJsonObject(x).getString("guiapp_code").toUpperCase().equals( codeGuiapp.getJsonObject(i).getString("guiapp_code").toUpperCase())) {
                                        validCodeGuiappTasks.add(validCodeGuiapp(conn, parcel.getJsonArray("parcel_packages").getJsonObject(x), codeGuiapp.getJsonObject(i)));
                                        parcel.getJsonArray("parcel_packages").getJsonObject(x).remove("guiapp_code");
                                    }
                                }
                            }
                        }
                        CompletableFuture.allOf(validCodeGuiappTasks.toArray(new CompletableFuture[codeGuiapp.size()])).whenComplete((ps, pt) -> {
                            try {
                                if (pt != null) {
                                    throw pt;
                                }
                                parcel.remove("codesGuiapp");
                                this.register(conn, parcel).whenComplete((resultRegister, errorRegister) -> {
                                    try {
                                        if (errorRegister != null){
                                            throw errorRegister;
                                        }
                                        // restar
                                        JsonObject guiapp = new JsonObject().put("guiapp",codeGuiapp);
                                        guiapp.put("id_parcels",resultRegister.getInteger("id"));
                                        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, GuiappDBV.UPDATE_PARCELS_STATUS_PREPAID_DETAILS);
                                        vertx.eventBus().send(GuiappDBV.class.getSimpleName(), guiapp, options, (AsyncResult<Message<JsonObject>> replyCredit) -> {
                                            try {
                                                if (replyCredit.failed()) {
                                                    throw new Exception(replyCredit.cause());
                                                }
                                             //   Message<JsonObject> customerCreditDataMsg = replyCredit.result();
                                               // JsonObject customerCreditData = customerCreditDataMsg.body();
                                                this.commit(conn, message, resultRegister);

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                this.rollback(conn, e, message);
                                            }
                                        });

                                        ///

                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        this.rollback(conn, t, message);
                                    }
                                });
                            } catch (Throwable t) {
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }

                }
            }
            else {
                SERVICES parcelService = SERVICES.valueOf((String) parcel.remove(SERVICE));
                switch (parcelService) {
                    case parcel:
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
                        break;
                    case parcel_inhouse:
                        this.registerInHouse(conn, parcel).whenComplete((resultRegister, errorRegister) -> {
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
                        break;
                    default:
                        break;
                }
            }

        });
    }

    private CompletableFuture<JsonObject> register(SQLConnection conn, JsonObject parcel) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Boolean internalCustomer = (Boolean) parcel.remove(INTERNAL_CUSTOMER);
            Boolean isInternalParcel = parcel.containsKey("is_internal_parcel") ? (Boolean) parcel.getBoolean("is_internal_parcel") : false;
            Boolean paysSender = null;
            Boolean isGuiappCanje= parcel.containsKey("isGuiappCanje")?true:false;
            parcel.remove("isGuiappCanje");
            Double guiappExcess = (Double)  parcel.remove("guiapp_excess");
            if(isGuiappCanje){
                if(parcel.containsKey("parcel_rad")){
                    parcel.put("parcel_rad",0.0);
                }
                parcel.put("iva_percent",0.0).put("parcel_iva",0.0);
            }
            parcel.remove("isGuiappCanje");
            if(isGuiappCanje){
                if(parcel.containsKey("parcel_rad")){
                    parcel.put("parcel_rad",0.0);
                }
                parcel.put("iva_percent",0.0).put("parcel_iva",0.0);
            }
            try {
                paysSender = parcel.getBoolean(PAYS_SENDER);
            } catch (Exception e){
                paysSender = parcel.getInteger(PAYS_SENDER).equals(1);
            }
            parcel.put(PAYS_SENDER, paysSender);
            Boolean reissue = parcel.containsKey(REISSUE) ? (Boolean) parcel.remove(REISSUE) : false;
            Boolean rework = reissue && parcel.containsKey(REWORK) ? (Boolean) parcel.remove(REWORK) : false;
            Integer reworkScheduleRouteDestinationId = rework ? (Integer) parcel.remove(REWORK_SCHEDULE_ROUTE_DESTINATION_ID) : null;
            Boolean withoutFreight = reissue && parcel.containsKey(WITHOUT_FREIGHT) ? (Boolean) parcel.remove(WITHOUT_FREIGHT) : false;
            Boolean cumulativeCost = reissue && parcel.containsKey(CUMULATIVE_COST) ? (Boolean) parcel.remove(CUMULATIVE_COST) : false;
            Integer cashOutId = (Integer) parcel.remove(CASHOUT_ID);
            Integer cashRegisterId = parcel.getInteger(CASH_REGISTER_ID);
            final boolean flagPromo = parcel.containsKey(FLAG_PROMO) && (boolean) parcel.remove(FLAG_PROMO);
            final boolean flagUserPromo = parcel.containsKey(FLAG_USER_PROMO) && (boolean) parcel.remove(FLAG_USER_PROMO);
            JsonObject promoDiscount = parcel.containsKey(DISCOUNT) ? (JsonObject) parcel.remove(DISCOUNT) : null;
            JsonObject customerCreditData = (JsonObject) parcel.remove("customer_credit_data");

            Boolean finalPaysSender = paysSender;
            this.generateCompuestID("parcel", cashRegisterId,internalCustomer).whenComplete((resultCompuestId, errorCompuestId) -> {
                try {
                    if (errorCompuestId != null) {
                        throw errorCompuestId;
                    }
                    parcel.put("waybill", resultCompuestId);
                    final Boolean is_complement = (Boolean) parcel.containsKey("is_complement") ? ((Boolean) parcel.remove("is_complement")) : false;
                    final Boolean is_credit = (Boolean) parcel.containsKey("is_credit") ? ((Boolean) parcel.remove("is_credit")) : false;
                    JsonArray parcelPackages = (JsonArray) parcel.remove("parcel_packages");
                    JsonArray parcelPackings = (JsonArray) parcel.remove("parcel_packings");
                    JsonArray payments = (JsonArray) parcel.remove("payments");


                    JsonObject serviceRadEad = new JsonObject();
                    String guiapp=promoDiscount!=null?promoDiscount.getString("discount_code"):null;
                    serviceRadEad.put("guiapp",guiapp);
                    serviceRadEad.put("guiapp",guiapp);
                    if(parcel.getBoolean("isRad") != null) {
                        String stringoRad = parcel.toString();
                        JSONObject jObjRad = new JSONObject(stringoRad);
                        Object radObj = jObjRad.get("parcel_rad");
                        serviceRadEad.put("is_rad", (Boolean) parcel.remove("isRad")).put("zip_code", (Integer) parcel.remove("zip_code_rad")).put("id_type_service", (Integer) parcel.remove("id_type_service")).put("service", "RAD");

                        if(radObj instanceof Integer ){
                            serviceRadEad.put("service_amount", (Integer) parcel.remove("parcel_rad"));
                        } else if (radObj instanceof Double ){
                            serviceRadEad.put("service_amount", (Double) parcel.remove("parcel_rad"));
                        }else
                        {
                            parcel.remove("parcel_rad");
                            serviceRadEad.put("service_amount", 0.00);
                        }

                        parcel.put("shipment_type", "RAD/OCU");

                    }

                    if(parcel.getBoolean("isEad") != null){
                        String stringoEad = parcel.toString();
                        JSONObject jObj = new JSONObject(stringoEad);
                        Object aObj = jObj.get("parcel_ead");
                        serviceRadEad.put("is_ead",(Boolean) parcel.remove("isEad")).put("zip_code",(Integer) parcel.remove("zip_code_ead")).put("id_type_service", (Integer) parcel.remove("id_type_service")).put("service", "EAD");
                        if(aObj instanceof Integer){
                            serviceRadEad.put("service_amount", (Integer) parcel.remove("parcel_ead"));
                        }else if (aObj instanceof Double){
                            serviceRadEad.put("service_amount", (Double) parcel.remove("parcel_ead"));
                        }

                        parcel.put("shipment_type", "EAD");
                    }

                    if(parcel.getBoolean("isRadEad") != null){
                        String stringoRadEad = parcel.toString();
                        JSONObject jObjRadEad = new JSONObject(stringoRadEad);
                        Object objRadEad = jObjRadEad.get("parcel_rad_ead");

                        serviceRadEad.put("is_rad_ead",(Boolean) parcel.remove("isRadEad")).put("zip_code",00000).put("id_type_service", (Integer) parcel.remove("id_type_service")).put("service", "RAD-EAD");
                        if(objRadEad instanceof Integer){
                            serviceRadEad.put("service_amount", (Integer) parcel.remove("parcel_rad_ead"));
                        } else if (objRadEad instanceof Double){
                            serviceRadEad.put("service_amount", (Double) parcel.remove("parcel_rad_ead"));
                        }

                        parcel.put("shipment_type", "RAD/EAD");

                    }

                    JsonObject cashChange = (JsonObject) parcel.remove("cash_change");
                    final double ivaPercent = (Double) parcel.remove("iva_percent");
                    final double parcelIvaPercent = (Double) parcel.remove("parcel_iva");
                    final int currencyId = (Integer) parcel.remove("currency_id");
                    final Integer createdBy = parcel.getInteger("created_by");
                    parcel.put("total_packages", parcelPackages.size());
                    Double reissuePaid = (Double) parcel.remove("reissue_paid");
                    Double reissueDebt = (Double) parcel.remove("reissue_debt");

                    this.comapreCityId("addressee", parcel.getInteger("addressee_id"), parcel.getInteger("terminal_destiny_id"), is_complement).whenComplete((resultAddressee, errorAddressee) -> {
                        try {
                            if (errorAddressee != null) {
                                throw errorAddressee;
                            }
                            if (!resultAddressee) {
                                throw new Exception("terminal_destiny_id and addressee city_id compared do not match");
                            }
                            this.comapreCityId("sender", parcel.getInteger("sender_id"), parcel.getInteger("terminal_origin_id"), is_complement).whenComplete((resultSender, errorSender) -> {
                                try {
                                    if (errorSender != null) {
                                        throw errorSender;
                                    }
                                    if (!resultSender) {
                                        throw new Exception("terminal_destiny_id and sender city_id compared do not match");
                                    }

                                    GenericQuery gen = this.generateGenericCreate(parcel);

                                    if (reissue && is_credit){
                                        parcel.put("reissue_paid", reissuePaid);
                                        parcel.put("reissue_debt", reissueDebt);
                                    }

                                    conn.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> parcelReply) -> {
                                        try {
                                            if (parcelReply.failed()) {
                                                throw parcelReply.cause();
                                            }
                                            final int parcelId = parcelReply.result().getKeys().getInteger(0);
                                            final int numPacks = parcelPackages.size();

                                            List<CompletableFuture<JsonObject>> packagesTasks = new ArrayList<>();

                                            parcel.put(ID, parcelId);

                                            if (reissue){
                                                parcel.put(REISSUE, reissue)
                                                        .put(REWORK, rework)
                                                        .put(REWORK_SCHEDULE_ROUTE_DESTINATION_ID, reworkScheduleRouteDestinationId);
                                            }

                                            JsonObject bodyPromo = new JsonObject()
                                                    .put(USER_ID, createdBy)
                                                    .put(FLAG_USER_PROMO, flagUserPromo)
                                                    .put(DISCOUNT, promoDiscount)
                                                    .put(SERVICE, SERVICES.parcel)
                                                    .put(BODY_SERVICE, parcel)
                                                    .put(PRODUCTS, parcelPackages)
                                                    .put(OTHER_PRODUCTS, parcelPackings)
                                                    .put(FLAG_PROMO, flagPromo);
                                            vertx.eventBus().send(PromosDBV.class.getSimpleName(), bodyPromo, new DeliveryOptions().addHeader(ACTION, ACTION_APPLY_PROMO_CODE), (AsyncResult<Message<JsonObject>> replyPromos) -> {
                                                try {
                                                    if(replyPromos.failed()) {
                                                        throw replyPromos.cause();
                                                    }
                                                    JsonObject resultApplyDiscount = replyPromos.result().body();

                                                    JsonObject parcelDiscount = resultApplyDiscount.getJsonObject(SERVICE);
                                                    JsonArray parcelPackagesDiscount = resultApplyDiscount.getJsonArray(PRODUCTS);
                                                    Double distanceKM = parcelPackagesDiscount.getJsonObject(0).getDouble(_DISTANCE_KM);

                                                    //AQUI CALCULO LOS PUNTOS Y EL MONTO DE PAYBACK
                                                    PaybackDBV objPayback = new PaybackDBV();
                                                    objPayback.calculatePointsParcel(conn, distanceKM, parcelPackages.size(), reissue).whenComplete((resultCalculate, error) -> {
                                                        try {
                                                            if (error != null) {
                                                                throw error;
                                                            }

                                                            Double paybackMoney = resultCalculate.getDouble("money");
                                                            Double paybackPoints = resultCalculate.getDouble("points");
                                                            parcel.put("payback", paybackMoney);

                                                            Double totalExcessCost = 0.0;
                                                            for (int i = 0; i < numPacks; i++) {
                                                                totalExcessCost = totalExcessCost + parcelPackagesDiscount.getJsonObject(i).getDouble("excess_cost");
                                                                packagesTasks.add(registerPackagesInParcel(conn, parcelPackagesDiscount.getJsonObject(i), parcelId, finalPaysSender, createdBy, parcel.getInteger("terminal_origin_id"), withoutFreight, internalCustomer, isInternalParcel, parcel.getInteger("terminal_destiny_id"),isGuiappCanje));
                                                            }

                                                            Double finalTotalExcessCost = totalExcessCost;
                                                            CompletableFuture.allOf(packagesTasks.toArray(new CompletableFuture[numPacks])).whenComplete((ps, pt) -> {
                                                                try {
                                                                    if (pt != null) {
                                                                        throw pt;
                                                                    }
                                                                    CompletableFuture.allOf(parcelPackings.stream()
                                                                                    .map(p -> registerPackingsInParcel(conn, (JsonObject) p, parcelId, createdBy))
                                                                                    .toArray(CompletableFuture[]::new))
                                                                            .whenComplete((ks, kt) -> {
                                                                                try {
                                                                                    if (kt != null) {
                                                                                        throw kt;
                                                                                    }

                                                                                    JsonObject parcelTotalAmount = isGuiappCanje? new JsonObject()
                                                                                            .put("amount", 0.0)
                                                                                            .put("discount", 0.0)
                                                                                            .put("total_amount", serviceRadEad.getDouble("service_amount") == null ? 0.00 : serviceRadEad.getDouble("service_amount"))
                                                                                            .put("extra_charges", serviceRadEad.getDouble("service_amount") == null ? 0.00 : serviceRadEad.getDouble("service_amount")): this.getParcelTotalAmountExec(parcelDiscount, parcelPackagesDiscount, parcelPackings, reissue, withoutFreight, cumulativeCost , serviceRadEad, isInternalParcel);

                                                                                    if(isGuiappCanje) {
                                                                                        Double totalAmountPP = (Double) parcelTotalAmount.remove("total_amount");
                                                                                        Double extraChargesPP = (Double) parcelTotalAmount.remove("extra_charges");
                                                                                        parcelTotalAmount.put("total_amount", totalAmountPP +  guiappExcess);
                                                                                        parcelTotalAmount.put("extra_charges", extraChargesPP +  guiappExcess);
                                                                                    }

                                                                                    Future<Message<JsonObject>> f1 = Future.future();
                                                                                    Future<Message<JsonObject>> f2 = Future.future();

                                                                                    vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                                                                                            new JsonObject().put("fieldName", "insurance_percent"),
                                                                                            new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f1.completer());
                                                                                    vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                                                                                            new JsonObject().put("fieldName", "max_insurance_value"),
                                                                                            new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f2.completer());

                                                                                    CompositeFuture.all(f1, f2).setHandler(reply -> {
                                                                                        try {
                                                                                            if (reply.failed()) {
                                                                                                throw reply.cause();
                                                                                            }

                                                                                            Double finalTotalAmount = parcelTotalAmount.getDouble("total_amount");
                                                                                            Double finalExtraCharges = parcelTotalAmount.getDouble("extra_charges") + finalTotalExcessCost;
                                                                                            Double finalAmount = parcelTotalAmount.getDouble("amount");
                                                                                            Double finalDiscount = parcelTotalAmount.getDouble("discount");

                                                                                            JsonObject field1 = reply.result().<Message<JsonObject>>resultAt(0).body();
                                                                                            JsonObject field2 = reply.result().<Message<JsonObject>>resultAt(1).body();

                                                                                            Double insurancePercent = Double.parseDouble(field1.getString("value"));
                                                                                            Integer maxInsuranceValue = Integer.parseInt(field2.getString("value"));

                                                                                            if (parcelDiscount.getDouble("insurance_value") != null && parcelDiscount.getDouble("insurance_value") > 0) {
                                                                                                Double insuranceAmount = UtilsMoney.round(parcelDiscount.getDouble("insurance_value") * (insurancePercent / 100), 2);

                                                                                                if (insuranceAmount <= 0) {
                                                                                                    parcelDiscount.put("has_insurance", false);
                                                                                                } else {
                                                                                                    if (insuranceAmount > maxInsuranceValue) {
                                                                                                        parcelDiscount.put("insurance_amount", maxInsuranceValue);
                                                                                                    } else {

                                                                                                        parcelDiscount.put("insurance_amount", insuranceAmount);
                                                                                                    }
                                                                                                    Double iva = 0.00;
                                                                                                    Double parcelIva = 0.00;
                                                                                                    Double totalAmountWithInsurance = 0.00;
                                                                                                    if (!reissue) {
                                                                                                        if(isInternalParcel) {
                                                                                                            totalAmountWithInsurance = finalTotalAmount;
                                                                                                            finalDiscount += parcelDiscount.getDouble("insurance_amount");
                                                                                                        } else {
                                                                                                            totalAmountWithInsurance = finalTotalAmount + parcelDiscount.getDouble("insurance_amount");
                                                                                                        }
                                                                                                        Double subTotal = totalAmountWithInsurance - this.getIva(totalAmountWithInsurance, ivaPercent - parcelIvaPercent);
                                                                                                        iva = this.getIva(totalAmountWithInsurance, ivaPercent);
                                                                                                        parcelIva = subTotal * (parcelIvaPercent / 100);
                                                                                                    } else {
                                                                                                        if (!withoutFreight || cumulativeCost) {
                                                                                                            if(isInternalParcel) {
                                                                                                                totalAmountWithInsurance = finalTotalAmount;
                                                                                                                finalDiscount += parcelDiscount.getDouble("insurance_amount");
                                                                                                            } else {
                                                                                                                totalAmountWithInsurance = finalTotalAmount + parcelDiscount.getDouble("insurance_amount");
                                                                                                            }
                                                                                                            Double subTotal = totalAmountWithInsurance - this.getIva(totalAmountWithInsurance, ivaPercent - parcelIvaPercent);
                                                                                                            iva = this.getIva(totalAmountWithInsurance, ivaPercent);
                                                                                                            parcelIva = subTotal * (parcelIvaPercent / 100);
                                                                                                        }
                                                                                                    }

                                                                                                    parcelDiscount.put("extra_charges", finalExtraCharges)
                                                                                                            .put("amount", finalAmount)
                                                                                                            .put("discount", finalDiscount)
                                                                                                            .put("iva", iva)
                                                                                                            .put("parcel_iva", parcelIva)
                                                                                                            .put("total_amount", totalAmountWithInsurance)
                                                                                                            .put("updated_by", createdBy)
                                                                                                            .put("updated_at", sdfDataBase(new Date()));
                                                                                                    try {
                                                                                                        String now = format_yyyy_MM_dd(new Date());
                                                                                                        conn.queryWithParams(QUERY_INSURANCE_VALIDATION, new JsonArray().add(now), replyInsValidation -> {
                                                                                                            try {
                                                                                                                if (replyInsValidation.failed()) {
                                                                                                                    throw replyInsValidation.cause();
                                                                                                                }
                                                                                                                if (replyInsValidation.result().getRows().isEmpty()) {
                                                                                                                    throw new Exception("There are no insurance policies available");
                                                                                                                }
                                                                                                                parcelDiscount.put("has_insurance", true);
                                                                                                                parcelDiscount.put("insurance_id", replyInsValidation.result().getRows().get(0).getInteger("id"));
                                                                                                                this.endRegister(conn, cashOutId, parcelDiscount, parcelId, createdBy, payments, is_complement,
                                                                                                                        cashChange, ivaPercent, currencyId, parcelPackagesDiscount, parcelPackings, objPayback,
                                                                                                                        paybackPoints, paybackMoney, internalCustomer, isInternalParcel, is_credit, customerCreditData, reissue , serviceRadEad,isGuiappCanje).whenComplete((resultRegister, errorRegister) -> {
                                                                                                                    try {
                                                                                                                        if (errorRegister != null){
                                                                                                                            throw errorRegister;
                                                                                                                        }
                                                                                                                        future.complete(resultRegister);
                                                                                                                    } catch (Throwable t){
                                                                                                                        future.completeExceptionally(t);
                                                                                                                    }
                                                                                                                });
                                                                                                            } catch (Throwable t) {
                                                                                                                future.completeExceptionally(t);
                                                                                                            }
                                                                                                        });
                                                                                                    } catch (ParseException e) {
                                                                                                        future.completeExceptionally(e);
                                                                                                    }
                                                                                                }
                                                                                            } else {

                                                                                                Double iva = 0.00;
                                                                                                Double parcelIva = 0.00;
                                                                                                if (!reissue) {
                                                                                                    Double subTotal = finalTotalAmount - this.getIva(finalTotalAmount, ivaPercent - parcelIvaPercent);
                                                                                                    iva = this.getIva(finalTotalAmount, ivaPercent);
                                                                                                    parcelIva = subTotal * (parcelIvaPercent / 100);
                                                                                                } else {
                                                                                                    if (!withoutFreight || cumulativeCost) {
                                                                                                        Double subTotal = finalTotalAmount - this.getIva(finalTotalAmount, ivaPercent - parcelIvaPercent);
                                                                                                        iva = this.getIva(finalTotalAmount, ivaPercent);
                                                                                                        parcelIva = subTotal * (parcelIvaPercent / 100);
                                                                                                    }
                                                                                                }

                                                                                                parcelDiscount.put("extra_charges", finalExtraCharges)
                                                                                                    .put("amount", finalAmount)
                                                                                                    .put("discount", finalDiscount)
                                                                                                    .put("iva", iva)
                                                                                                    .put("parcel_iva", parcelIva)
                                                                                                    .put("total_amount", finalTotalAmount)
                                                                                                    .put("updated_by", createdBy)
                                                                                                    .put("updated_at", sdfDataBase(new Date()));
                                                                                                this.endRegister(conn, cashOutId, parcelDiscount, parcelId, createdBy, payments, is_complement,
                                                                                                        cashChange, ivaPercent, currencyId, parcelPackagesDiscount, parcelPackings, objPayback,
                                                                                                        paybackPoints, paybackMoney, internalCustomer, isInternalParcel, is_credit, customerCreditData, reissue , serviceRadEad,isGuiappCanje).whenComplete((resultRegister, errorRegister) -> {
                                                                                                    try {
                                                                                                        if (errorRegister != null){
                                                                                                            throw errorRegister;
                                                                                                        }
                                                                                                        future.complete(resultRegister);
                                                                                                    } catch (Throwable t){
                                                                                                        future.completeExceptionally(t);
                                                                                                    }
                                                                                                });
                                                                                            }
                                                                                        } catch (Throwable t) {
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
                                                            });
                                                        } catch (Throwable t) {
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
                                    });
                                } catch (Throwable t) {
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
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> registerInHouse(SQLConnection conn, JsonObject parcel) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Boolean internalCustomer = (Boolean) parcel.remove(INTERNAL_CUSTOMER);
            Boolean isInternalParcel = parcel.containsKey("is_internal_parcel") ? (Boolean) parcel.getBoolean("is_internal_parcel") : false;
            Boolean paysSender = null;
            Boolean isGuiappCanje= parcel.containsKey("isGuiappCanje")?true:false;
            parcel.remove("isGuiappCanje");
            final Integer guiappExcess = (Integer)  parcel.remove("guiapp_excess");
            if(isGuiappCanje){
                if(parcel.containsKey("parcel_rad")){
                    parcel.put("parcel_rad",0.0);
                }
                parcel.put("iva_percent",0.0).put("parcel_iva",0.0);
            }
            parcel.remove("isGuiappCanje");
            if(isGuiappCanje){
                if(parcel.containsKey("parcel_rad")){
                    parcel.put("parcel_rad",0.0);
                }
                parcel.put("iva_percent",0.0).put("parcel_iva",0.0);
            }
            try {
                paysSender = parcel.getBoolean(PAYS_SENDER);
            } catch (Exception e){
                paysSender = parcel.getInteger(PAYS_SENDER).equals(1);
            }
            parcel.put(PAYS_SENDER, paysSender);
            Boolean reissue = parcel.containsKey(REISSUE) ? (Boolean) parcel.remove(REISSUE) : false;
            Boolean rework = reissue && parcel.containsKey(REWORK) ? (Boolean) parcel.remove(REWORK) : false;
            Integer reworkScheduleRouteDestinationId = rework ? (Integer) parcel.remove(REWORK_SCHEDULE_ROUTE_DESTINATION_ID) : null;
            Boolean withoutFreight = reissue && parcel.containsKey(WITHOUT_FREIGHT) ? (Boolean) parcel.remove(WITHOUT_FREIGHT) : false;
            Boolean cumulativeCost = reissue && parcel.containsKey(CUMULATIVE_COST) ? (Boolean) parcel.remove(CUMULATIVE_COST) : false;
            Integer cashOutId = (Integer) parcel.remove(CASHOUT_ID);
            Integer cashRegisterId = parcel.getInteger(CASH_REGISTER_ID);
            final boolean flagPromo = parcel.containsKey(FLAG_PROMO) && (boolean) parcel.remove(FLAG_PROMO);
            final boolean flagUserPromo = parcel.containsKey(FLAG_USER_PROMO) && (boolean) parcel.remove(FLAG_USER_PROMO);
            JsonObject promoDiscount = parcel.containsKey(DISCOUNT) ? (JsonObject) parcel.remove(DISCOUNT) : null;
            JsonObject customerCreditData = (JsonObject) parcel.remove("customer_credit_data");

            Boolean finalPaysSender = paysSender;
            this.generateCompuestID("parcel", cashRegisterId,internalCustomer).whenComplete((resultCompuestId, errorCompuestId) -> {
                try {
                    if (errorCompuestId != null) {
                        throw errorCompuestId;
                    }
                    parcel.put("waybill", resultCompuestId);
                    final Boolean is_complement = (Boolean) parcel.containsKey("is_complement") ? ((Boolean) parcel.remove("is_complement")) : false;
                    final Boolean is_credit = (Boolean) parcel.containsKey("is_credit") ? ((Boolean) parcel.remove("is_credit")) : false;
                    JsonArray parcelPackages = (JsonArray) parcel.remove("parcel_packages");
                    JsonArray parcelPackings = (JsonArray) parcel.remove("parcel_packings");
                    JsonArray payments = (JsonArray) parcel.remove("payments");


                    JsonObject serviceRadEad = new JsonObject();
                    String guiapp=promoDiscount!=null?promoDiscount.getString("discount_code"):null;
                    serviceRadEad.put("guiapp",guiapp);
                    if(parcel.getBoolean("isRad") != null) {
                        String stringoRad = parcel.toString();
                        JSONObject jObjRad = new JSONObject(stringoRad);
                        Object radObj = jObjRad.get("parcel_rad");
                        serviceRadEad.put("is_rad", (Boolean) parcel.remove("isRad")).put("zip_code", (Integer) parcel.remove("zip_code_rad")).put("id_type_service", (Integer) parcel.remove("id_type_service")).put("service", "RAD");

                        if(radObj instanceof Integer ){
                            serviceRadEad.put("service_amount", (Integer) parcel.remove("parcel_rad"));
                        } else if (radObj instanceof Double ){
                            serviceRadEad.put("service_amount", (Double) parcel.remove("parcel_rad"));
                        }else
                        {
                            parcel.remove("parcel_rad");
                            serviceRadEad.put("service_amount", 0.00);
                        }

                        parcel.put("shipment_type", "RAD/OCU");

                    }

                    if(parcel.getBoolean("isEad") != null){
                        String stringoEad = parcel.toString();
                        JSONObject jObj = new JSONObject(stringoEad);
                        Object aObj = jObj.get("parcel_ead");
                        serviceRadEad.put("is_ead",(Boolean) parcel.remove("isEad")).put("zip_code",(Integer) parcel.remove("zip_code_ead")).put("id_type_service", (Integer) parcel.remove("id_type_service")).put("service", "EAD");
                        if(aObj instanceof Integer){
                            serviceRadEad.put("service_amount", (Integer) parcel.remove("parcel_ead"));
                        }else if (aObj instanceof Double){
                            serviceRadEad.put("service_amount", (Double) parcel.remove("parcel_ead"));
                        }

                        parcel.put("shipment_type", "EAD");
                    }

                    if(parcel.getBoolean("isRadEad") != null){
                        String stringoRadEad = parcel.toString();
                        JSONObject jObjRadEad = new JSONObject(stringoRadEad);
                        Object objRadEad = jObjRadEad.get("parcel_rad_ead");

                        serviceRadEad.put("is_rad_ead",(Boolean) parcel.remove("isRadEad")).put("zip_code",00000).put("id_type_service", (Integer) parcel.remove("id_type_service")).put("service", "RAD-EAD");
                        if(objRadEad instanceof Integer){
                            serviceRadEad.put("service_amount", (Integer) parcel.remove("parcel_rad_ead"));
                        } else if (objRadEad instanceof Double){
                            serviceRadEad.put("service_amount", (Double) parcel.remove("parcel_rad_ead"));
                        }

                        parcel.put("shipment_type", "RAD/EAD");

                    }

                    JsonObject cashChange = (JsonObject) parcel.remove("cash_change");
                    final double ivaPercent = (Double) parcel.remove("iva_percent");
                    final double parcelIvaPercent = (Double) parcel.remove("parcel_iva");
                    final int currencyId = (Integer) parcel.remove("currency_id");
                    final Integer createdBy = parcel.getInteger("created_by");
                    parcel.put("total_packages", parcelPackages.size());
                    Double reissuePaid = (Double) parcel.remove("reissue_paid");
                    Double reissueDebt = (Double) parcel.remove("reissue_debt");

                    this.comapreCityId("addressee", parcel.getInteger("addressee_id"), parcel.getInteger("terminal_destiny_id"), is_complement).whenComplete((resultAddressee, errorAddressee) -> {
                        try {
                            if (errorAddressee != null) {
                                throw errorAddressee;
                            }
                            if (!resultAddressee) {
                                throw new Exception("terminal_destiny_id and addressee city_id compared do not match");
                            }
                            this.comapreCityId("sender", parcel.getInteger("sender_id"), parcel.getInteger("terminal_origin_id"), is_complement).whenComplete((resultSender, errorSender) -> {
                                try {
                                    if (errorSender != null) {
                                        throw errorSender;
                                    }
                                    if (!resultSender) {
                                        throw new Exception("terminal_destiny_id and sender city_id compared do not match");
                                    }

                                    GenericQuery gen = this.generateGenericCreate(parcel);

                                    if (reissue && is_credit){
                                        parcel.put("reissue_paid", reissuePaid);
                                        parcel.put("reissue_debt", reissueDebt);
                                    }

                                    conn.updateWithParams(gen.getQuery(), gen.getParams(), (AsyncResult<UpdateResult> parcelReply) -> {
                                        try {
                                            if (parcelReply.failed()) {
                                                throw parcelReply.cause();
                                            }
                                            final int parcelId = parcelReply.result().getKeys().getInteger(0);
                                            final int numPacks = parcelPackages.size();

                                            List<CompletableFuture<JsonObject>> packagesTasks = new ArrayList<>();

                                            parcel.put(ID, parcelId);

                                            if (reissue){
                                                parcel.put(REISSUE, reissue)
                                                        .put(REWORK, rework)
                                                        .put(REWORK_SCHEDULE_ROUTE_DESTINATION_ID, reworkScheduleRouteDestinationId);
                                            }

                                            JsonObject bodyPromo = new JsonObject()
                                                    .put(USER_ID, createdBy)
                                                    .put(FLAG_USER_PROMO, flagUserPromo)
                                                    .put(DISCOUNT, promoDiscount)
                                                    .put(SERVICE, SERVICES.parcel_inhouse)
                                                    .put(BODY_SERVICE, parcel)
                                                    .put(PRODUCTS, parcelPackages)
                                                    .put(OTHER_PRODUCTS, parcelPackings)
                                                    .put(FLAG_PROMO, flagPromo);
                                            vertx.eventBus().send(PromosDBV.class.getSimpleName(), bodyPromo, new DeliveryOptions().addHeader(ACTION, ACTION_APPLY_PROMO_CODE), (AsyncResult<Message<JsonObject>> replyPromos) -> {
                                                try {
                                                    if(replyPromos.failed()) {
                                                        throw replyPromos.cause();
                                                    }
                                                    JsonObject resultApplyDiscount = replyPromos.result().body();
                                                    JsonObject parcelDiscount = resultApplyDiscount.getJsonObject(SERVICE);
                                                    JsonArray parcelPackagesDiscount = resultApplyDiscount.getJsonArray(PRODUCTS);
                                                    Double distanceKM = parcelPackagesDiscount.getJsonObject(0).getDouble(_DISTANCE_KM);

                                                    //AQUI CALCULO LOS PUNTOS Y EL MONTO DE PAYBACK
                                                    PaybackDBV objPayback = new PaybackDBV();
                                                    objPayback.calculatePointsParcel(conn, distanceKM, parcelPackages.size(), reissue).whenComplete((resultCalculate, error) -> {
                                                        try {
                                                            if (error != null) {
                                                                throw error;
                                                            }

                                                            Double paybackMoney = resultCalculate.getDouble("money");
                                                            Double paybackPoints = resultCalculate.getDouble("points");
                                                            parcel.put("payback", paybackMoney);

                                                            Double totalExcessCost = 0.0;
                                                            for (int i = 0; i < numPacks; i++) {
                                                                totalExcessCost = totalExcessCost + parcelPackagesDiscount.getJsonObject(i).getDouble("excess_cost");
                                                                packagesTasks.add(registerPackagesInParcel(conn, parcelPackagesDiscount.getJsonObject(i), parcelId, finalPaysSender, createdBy, parcel.getInteger("terminal_origin_id"), withoutFreight, internalCustomer, isInternalParcel, parcel.getInteger("terminal_destiny_id"),isGuiappCanje));
                                                            }

                                                            Double finalTotalExcessCost = totalExcessCost;
                                                            CompletableFuture.allOf(packagesTasks.toArray(new CompletableFuture[numPacks])).whenComplete((ps, pt) -> {
                                                                try {
                                                                    if (pt != null) {
                                                                        throw pt;
                                                                    }
                                                                    CompletableFuture.allOf(parcelPackings.stream()
                                                                            .map(p -> registerPackingsInParcel(conn, (JsonObject) p, parcelId, createdBy))
                                                                            .toArray(CompletableFuture[]::new))
                                                                            .whenComplete((ks, kt) -> {
                                                                                try {
                                                                                    if (kt != null) {
                                                                                        throw kt;
                                                                                    }

                                                                                    JsonObject parcelTotalAmount = isGuiappCanje? new JsonObject()
                                                                                            .put("amount", 0.0)
                                                                                            .put("discount", 0.0)
                                                                                            .put("total_amount", serviceRadEad.getDouble("service_amount") == null ? 0.00 : serviceRadEad.getDouble("service_amount"))
                                                                                            .put("extra_charges", serviceRadEad.getDouble("service_amount") == null ? 0.00 : serviceRadEad.getDouble("service_amount")): this.getParcelTotalAmountExec(parcelDiscount, parcelPackagesDiscount, parcelPackings, reissue, withoutFreight, cumulativeCost , serviceRadEad, isInternalParcel);

                                                                                    if(isGuiappCanje) {
                                                                                        Double totalAmountPP = (Double) parcelTotalAmount.remove("total_amount");
                                                                                        Double extraChargesPP = (Double) parcelTotalAmount.remove("extra_charges");
                                                                                        parcelTotalAmount.put("total_amount", totalAmountPP +  guiappExcess);
                                                                                        parcelTotalAmount.put("extra_charges", extraChargesPP +  guiappExcess);
                                                                                    }

                                                                                    Future<Message<JsonObject>> f1 = Future.future();
                                                                                    Future<Message<JsonObject>> f2 = Future.future();

                                                                                    vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                                                                                            new JsonObject().put("fieldName", "insurance_percent_inhouse"),
                                                                                            new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f1.completer());
                                                                                    vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                                                                                            new JsonObject().put("fieldName", "max_insurance_value"),
                                                                                            new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f2.completer());

                                                                                    CompositeFuture.all(f1, f2).setHandler(reply -> {
                                                                                        try {
                                                                                            if (reply.failed()) {
                                                                                                throw reply.cause();
                                                                                            }

                                                                                            Double finalTotalAmount = parcelTotalAmount.getDouble("total_amount");
                                                                                            Double finalExtraCharges = parcelTotalAmount.getDouble("extra_charges") + finalTotalExcessCost;
                                                                                            Double finalAmount = parcelTotalAmount.getDouble("amount");
                                                                                            Double finalDiscount = parcelTotalAmount.getDouble("discount");

                                                                                            JsonObject field1 = reply.result().<Message<JsonObject>>resultAt(0).body();
                                                                                            JsonObject field2 = reply.result().<Message<JsonObject>>resultAt(1).body();

                                                                                            Double insurancePercent = Double.parseDouble(field1.getString("value"));
                                                                                            Integer maxInsuranceValue = Integer.parseInt(field2.getString("value"));

                                                                                            if (parcelDiscount.getDouble("insurance_value") != null && parcelDiscount.getDouble("insurance_value") > 0) {
                                                                                                Double insuranceAmount = UtilsMoney.round(parcelDiscount.getDouble("insurance_value") * (insurancePercent / 100), 2);

                                                                                                if (insuranceAmount <= 0) {
                                                                                                    parcelDiscount.put("has_insurance", false);
                                                                                                } else {
                                                                                                    if (insuranceAmount > maxInsuranceValue) {
                                                                                                        parcelDiscount.put("insurance_amount", maxInsuranceValue);
                                                                                                    } else {

                                                                                                        parcelDiscount.put("insurance_amount", insuranceAmount);
                                                                                                    }
                                                                                                    Double iva = 0.00;
                                                                                                    double parcelIva = 0.00;
                                                                                                    Double totalAmountWithInsurance = 0.00;
                                                                                                    if (!reissue) {
                                                                                                        if(isInternalParcel) {
                                                                                                            totalAmountWithInsurance = finalTotalAmount;
                                                                                                            finalDiscount += parcelDiscount.getDouble("insurance_amount");
                                                                                                        } else {
                                                                                                            totalAmountWithInsurance = finalTotalAmount + parcelDiscount.getDouble("insurance_amount");
                                                                                                        }
                                                                                                        double subTotal = totalAmountWithInsurance - this.getIva(totalAmountWithInsurance, ivaPercent - parcelIvaPercent);
                                                                                                        iva = this.getIva(totalAmountWithInsurance, ivaPercent);
                                                                                                        parcelIva = subTotal * (parcelIvaPercent / 100);
                                                                                                    } else {
                                                                                                        if (!withoutFreight || cumulativeCost) {
                                                                                                            if(isInternalParcel) {
                                                                                                                totalAmountWithInsurance = finalTotalAmount;
                                                                                                                finalDiscount += parcelDiscount.getDouble("insurance_amount");
                                                                                                            } else {
                                                                                                                totalAmountWithInsurance = finalTotalAmount + parcelDiscount.getDouble("insurance_amount");
                                                                                                            }
                                                                                                            double subTotal = totalAmountWithInsurance - this.getIva(totalAmountWithInsurance, ivaPercent - parcelIvaPercent);
                                                                                                            iva = this.getIva(totalAmountWithInsurance, ivaPercent);
                                                                                                            parcelIva = subTotal * (parcelIvaPercent / 100);
                                                                                                        }
                                                                                                    }

                                                                                                    parcelDiscount.put("extra_charges", finalExtraCharges)
                                                                                                            .put("amount", finalAmount)
                                                                                                            .put("discount", finalDiscount)
                                                                                                            .put("iva", iva)
                                                                                                            .put("parcel_iva", parcelIva)
                                                                                                            .put("total_amount", totalAmountWithInsurance)
                                                                                                            .put("updated_by", createdBy)
                                                                                                            .put("updated_at", sdfDataBase(new Date()));
                                                                                                    try {
                                                                                                        String now = format_yyyy_MM_dd(new Date());
                                                                                                        conn.queryWithParams(QUERY_INSURANCE_VALIDATION, new JsonArray().add(now), replyInsValidation -> {
                                                                                                            try {
                                                                                                                if (replyInsValidation.failed()) {
                                                                                                                    throw replyInsValidation.cause();
                                                                                                                }
                                                                                                                if (replyInsValidation.result().getRows().isEmpty()) {
                                                                                                                    throw new Exception("There are no insurance policies available");
                                                                                                                }
                                                                                                                parcelDiscount.put("has_insurance", true);
                                                                                                                parcelDiscount.put("insurance_id", replyInsValidation.result().getRows().get(0).getInteger("id"));
                                                                                                                this.endRegister(conn, cashOutId, parcelDiscount, parcelId, createdBy, payments, is_complement,
                                                                                                                        cashChange, ivaPercent, currencyId, parcelPackagesDiscount, parcelPackings, objPayback,
                                                                                                                        paybackPoints, paybackMoney, internalCustomer, isInternalParcel, is_credit, customerCreditData, reissue , serviceRadEad,isGuiappCanje).whenComplete((resultRegister, errorRegister) -> {
                                                                                                                    try {
                                                                                                                        if (errorRegister != null){
                                                                                                                            throw errorRegister;
                                                                                                                        }
                                                                                                                        future.complete(resultRegister);
                                                                                                                    } catch (Throwable t){
                                                                                                                        future.completeExceptionally(t);
                                                                                                                    }
                                                                                                                });
                                                                                                            } catch (Throwable t) {
                                                                                                                future.completeExceptionally(t);
                                                                                                            }
                                                                                                        });
                                                                                                    } catch (ParseException e) {
                                                                                                        future.completeExceptionally(e);
                                                                                                    }
                                                                                                }
                                                                                            } else {

                                                                                                Double iva = 0.00;
                                                                                                Double parcelIva = 0.00;
                                                                                                if (!reissue) {
                                                                                                    Double subTotal = finalTotalAmount - this.getIva(finalTotalAmount, ivaPercent - parcelIvaPercent);
                                                                                                    iva = this.getIva(finalTotalAmount, ivaPercent);
                                                                                                    parcelIva = subTotal * (parcelIvaPercent / 100);
                                                                                                } else {
                                                                                                    if (!withoutFreight || cumulativeCost) {
                                                                                                        Double subTotal = finalTotalAmount - this.getIva(finalTotalAmount, ivaPercent - parcelIvaPercent);
                                                                                                        iva = this.getIva(finalTotalAmount, ivaPercent);
                                                                                                        parcelIva = subTotal * (parcelIvaPercent / 100);
                                                                                                    }
                                                                                                }

                                                                                                parcelDiscount.put("extra_charges", finalExtraCharges)
                                                                                                        .put("amount", finalAmount)
                                                                                                        .put("discount", finalDiscount)
                                                                                                        .put("iva", iva)
                                                                                                        .put("parcel_iva", parcelIva)
                                                                                                        .put("total_amount", finalTotalAmount)
                                                                                                        .put("updated_by", createdBy)
                                                                                                        .put("updated_at", sdfDataBase(new Date()));
                                                                                                this.endRegister(conn, cashOutId, parcelDiscount, parcelId, createdBy, payments, is_complement,
                                                                                                        cashChange, ivaPercent, currencyId, parcelPackagesDiscount, parcelPackings, objPayback,
                                                                                                        paybackPoints, paybackMoney, internalCustomer, isInternalParcel, is_credit, customerCreditData, reissue , serviceRadEad,isGuiappCanje).whenComplete((resultRegister, errorRegister) -> {
                                                                                                    try {
                                                                                                        if (errorRegister != null){
                                                                                                            throw errorRegister;
                                                                                                        }
                                                                                                        future.complete(resultRegister);
                                                                                                    } catch (Throwable t){
                                                                                                        future.completeExceptionally(t);
                                                                                                    }
                                                                                                });
                                                                                            }
                                                                                        } catch (Throwable t) {
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
                                                            });
                                                        } catch (Throwable t) {
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
                                    });
                                } catch (Throwable t) {
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
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private JsonObject getParcelTotalAmountExec(JsonObject parcel, JsonArray parcelPackages, JsonArray parcelPackings, Boolean reissue, Boolean withoutFreight, Boolean cumulativeCost , JsonObject rad, boolean isInternalParcel){
        JsonObject withoutFreightReturn = new JsonObject()
                .put("amount", 0.00)
                .put("discount", 0.00)
                .put("total_amount", 0.00)
                .put("extra_charges", 0.00);
        if (!reissue){
            return this.getParcelTotalAmount(parcel, parcelPackages, parcelPackings, cumulativeCost , rad, isInternalParcel);
        } else {
            if (!withoutFreight || cumulativeCost){
                return this.getParcelTotalAmount(parcel, parcelPackages, parcelPackings, cumulativeCost , rad, isInternalParcel);
            } else {
                return withoutFreightReturn;
            }
        }
    }

    private JsonObject getParcelTotalAmount(JsonObject parcel, JsonArray parcelPackages, JsonArray parcelPackings, Boolean cumulativeCost , JsonObject serviceObject, boolean isInternalParcel){

        Double amount = 0.00;
        Double discount = 0.00;
        Double totalAmount = 0.00;
        Double extraCharges = 0.00;

        if (parcel.containsKey(AMOUNT) && parcel.containsKey(DISCOUNT) && parcel.containsKey(TOTAL_AMOUNT)) {
            amount = parcel.getDouble("amount");
            discount = parcel.getDouble("discount");
            totalAmount = parcel.getDouble("total_amount");
        } else {
            for (int i = 0; i < parcelPackages.size(); i++) {
                JsonObject parcelPackage = parcelPackages.getJsonObject(i);
                amount += parcelPackage.containsKey("amount") ? parcelPackage.getDouble("amount") : parcelPackage.getDouble("cost");
                discount += parcelPackage.getDouble("discount");
                totalAmount += parcelPackage.getDouble("total_amount");
            }
        }

        for (int i = 0; i < parcelPackings.size(); i++) {
            JsonObject parcelPacking = parcelPackings.getJsonObject(i);
            if (isInternalParcel) {
                discount += parcelPacking.getDouble("total_amount", 0.0);
            }
            extraCharges += parcelPacking.getDouble("total_amount", 0.0);

        }

        if (cumulativeCost){
            amount += parcel.getDouble("amount");
            discount += parcel.getDouble("discount", 0.00);
            totalAmount += parcel.getDouble("total_amount");
        }
        boolean isRadEad=false;
        if((serviceObject.getBoolean("is_rad") != null  || serviceObject.getBoolean("is_ead") != null || serviceObject.getBoolean("is_rad_ead") != null)) {
            isRadEad=true;
            if (isInternalParcel) {
                discount += serviceObject.getDouble("service_amount");
            }
            extraCharges += serviceObject.getDouble("service_amount");
        }

        String promoGuipp=serviceObject.getString("guiapp")!=null?serviceObject.getString("guiapp"):"";
        if ((promoGuipp.equals("GUIAPPALL") ||  promoGuipp.equals("GUIASPP501KM") )  && isRadEad ) {
            discount+=serviceObject.getDouble("service_amount");
            //amount+=serviceObject.getDouble("service_amount");
            totalAmount -= serviceObject.getDouble("service_amount");
        }

        if (!isInternalParcel) {
            totalAmount += extraCharges;
        } else {
            discount = amount + extraCharges;
            totalAmount = 0.0;
        }

        return new JsonObject()
                .put("amount", amount)
                .put("discount", discount)
                .put("total_amount", totalAmount)
                .put("extra_charges", extraCharges);
    }

    protected CompletableFuture<JsonObject> endRegister(SQLConnection conn, Integer cashOutId, JsonObject parcel, Integer parcelId, Integer createdBy,
                                                        JsonArray payments, Boolean is_complement, JsonObject cashChange, Double ivaPercent, Integer currencyId, JsonArray parcelPackages,
                                                        JsonArray parcelPackings, PaybackDBV objPayback, Double paybackPoints, Double paybackMoney, Boolean internalCustomer,  Boolean isInternalParcel, Boolean is_credit, JsonObject customerCreditData, boolean reissue, JsonObject radObject,Boolean isGuiappCanje){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.internalCustomerValidation(internalCustomer,isInternalParcel, parcel).whenComplete((resultInternalCustomer, errorInternalCustomer) -> {
            try {
                if (errorInternalCustomer != null){
                    throw new Exception(errorInternalCustomer);
                }
                parcel.mergeIn(resultInternalCustomer, true);
                if (internalCustomer && isInternalParcel){
                    parcel.put(PAYS_SENDER, true);
                }

                String parcelAction = is_credit || isGuiappCanje ? "voucher" : "purchase";

                boolean paysSender = parcel.getBoolean(PAYS_SENDER);
                parcel.put(PROMISE_DELIVERY_DATE, UtilsDate.sdfDataBase(this.getParcelPromiseDeliveryDate()));
                Double totalAmount = parcel.getDouble(TOTAL_AMOUNT);
                Double innerTotalAmount = UtilsMoney.round(totalAmount, 2);

                double creditBalance = 0;
                double debt = 0;

                if(is_credit){

                    if (reissue){

                        Double previousDebt = (Double) parcel.remove("reissue_debt");
                        Double reissuePaid = parcel.containsKey("reissue_paid") ? (Double) parcel.remove("reissue_paid") : 0.00;
                        boolean reissueHasDebt = (reissuePaid > 0 && reissuePaid < innerTotalAmount) || previousDebt > 0;
                        if (reissueHasDebt){
                            debt = innerTotalAmount - reissuePaid;
                        } else {
                            creditBalance = reissuePaid - innerTotalAmount;
                        }

                    } else {
                        debt = innerTotalAmount;
                    }

                    parcel.put("debt", debt);
                }

                GenericQuery update = this.generateGenericUpdate("parcels", parcel);

                double finalCreditBalance = creditBalance;
                double finalDebt = debt;
                if (!paysSender) {
                    conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                        try {
                            if (replyUpdate.failed()){
                                throw replyUpdate.cause();
                            }
                            if (is_credit) {

                                this.updateCustomerCredit(conn, customerCreditData, finalDebt, createdBy, false, reissue, finalCreditBalance)
                                        .whenComplete((replyCustomer, errorCustomer) -> {
                                            try {
                                                if (errorCustomer != null) {
                                                    throw new Exception(errorCustomer);
                                                }
                                                this.insertService(conn, parcel , createdBy , radObject , parcelId).whenComplete((replyService , errorService) -> {
                                                    try{
                                                        if (errorService != null){
                                                            throw errorService;
                                                        }

                                                        JsonObject result = new JsonObject().put("id", parcelId);
                                                        result.put("tracking_code", parcel.getString("parcel_tracking_code"));
                                                        future.complete(result);

                                                    } catch (Throwable t){
                                                        t.printStackTrace();
                                                        future.completeExceptionally(t);
                                                    }
                                                });

                                            } catch (Throwable t) {
                                                t.printStackTrace();
                                                future.completeExceptionally(t);
                                            }
                                        });
                            } else {
                                JsonObject result = new JsonObject().put("id", parcelId);
                                result.put("tracking_code", parcel.getString("parcel_tracking_code"));
                                this.insertService(conn, parcel , createdBy , radObject , parcelId).whenComplete((replyService , errorService) -> {
                                    try{
                                        if (errorService != null){
                                            throw errorService;
                                        }

                                        future.complete(result);

                                    } catch (Throwable t){
                                        t.printStackTrace();
                                        future.completeExceptionally(t);
                                    }
                                });

                            }
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                } else {
                    Double totalPayments = 0.0;
                    if(payments == null && !is_complement && !internalCustomer && !isInternalParcel){
                        throw new Exception("No payment object was found");
                    }
                    if (payments == null && is_complement){
                        JsonObject result = new JsonObject().put("id", parcelId);
                        result.put("tracking_code", parcel.getString("parcel_tracking_code"));
                        future.complete(result);
                    } else {
                        final int pLen = payments == null ? 0 : payments.size();

                        for (int i = 0; i < pLen; i++) {
                            JsonObject payment = payments.getJsonObject(i);
                            Double paymentAmount = payment.getDouble("amount");
                            if (paymentAmount == null || paymentAmount < 0.0) {
                                throw new Exception("Invalid payment amount: " + paymentAmount);
                            }
                            totalPayments += UtilsMoney.round(paymentAmount, 2);
                        }

                        if(!is_credit && !isGuiappCanje) {
                            if (totalPayments > innerTotalAmount) {
                                throw new Exception("The payment " + totalPayments + " is greater than the total " + innerTotalAmount);
                            }
                            if (totalPayments < innerTotalAmount) {
                                throw new Exception("The payment " + totalPayments + " is lower than the total " + innerTotalAmount);
                            }
                        } else if(isGuiappCanje){
                                innerTotalAmount=totalPayments > 0 ? totalPayments : 0.0;
                        }
                        // Insert ticket
                        //double finalCreditBalance = creditBalance;
                        this.insertTicket(conn, cashOutId, parcelId, innerTotalAmount, cashChange, createdBy, ivaPercent, parcelAction).whenComplete((JsonObject ticket, Throwable ticketError) -> {
                            try {
                                if (ticketError != null) {
                                    throw ticketError;
                                }

                                // Insert ticket details
                                this.insertTicketDetail(conn, ticket.getInteger("id"), createdBy, parcelPackages, parcelPackings, parcel, internalCustomer , radObject).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                    try {
                                        if (dError != null) {
                                            throw dError;
                                        }

                                        if (is_credit) {
                                            conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                try {
                                                    if (replyUpdate.failed()) {
                                                        throw replyUpdate.cause();
                                                    }
                                                    JsonObject paramMovPayback = new JsonObject()
                                                            .put("customer_id", parcel.getInteger("sender_id"))
                                                            .put("points", paybackPoints)
                                                            .put("money", paybackMoney)
                                                            .put("type_movement", "I")
                                                            .put("motive", "Envio de paquetería(sender)")
                                                            .put("id_parent", parcelId)
                                                            .put("employee_id", createdBy);
                                                    objPayback.generateMovementPayback(conn, paramMovPayback).whenComplete((movementPayback, errorMP) -> {
                                                        try {
                                                            if (errorMP != null) {
                                                                throw errorMP;
                                                            }

                                                            this.updateCustomerCredit(conn, customerCreditData, finalDebt, createdBy, false, reissue, finalCreditBalance)
                                                                   .whenComplete((replyCustomer, errorCustomer) -> {
                                                               try{
                                                                   if (errorCustomer != null) {
                                                                       throw new Exception(errorCustomer);
                                                                   }

                                                                   //JsonObject result = new JsonObject().put("id", parcelId);
                                                                   //result.put("ticket_id", ticket.getInteger("id"))
                                                                   //        .put("tracking_code", parcel.getString("parcel_tracking_code"));
                                                                   // future.complete(result);
                                                                   this.insertService(conn, parcel , createdBy , radObject , parcelId).whenComplete((replyService , errorService) -> {
                                                                       try{
                                                                           if (errorService != null){
                                                                               throw errorService;
                                                                           }

                                                                           JsonObject result = new JsonObject().put("id", parcelId);
                                                                           result.put("ticket_id", ticket.getInteger("id"))
                                                                                   .put("tracking_code", parcel.getString("parcel_tracking_code"));
                                                                            future.complete(result);

                                                                       } catch (Throwable t){
                                                                           t.printStackTrace();
                                                                           future.completeExceptionally(t);
                                                                       }
                                                                   });

                                                               } catch (Throwable t) {
                                                                   t.printStackTrace();
                                                                   future.completeExceptionally(t);
                                                               }
                                                           });
                                                        } catch (Throwable t) {
                                                            t.printStackTrace();
                                                            future.completeExceptionally(t);
                                                        }
                                                    });
                                                } catch (Throwable t) {
                                                    t.printStackTrace();
                                                    future.completeExceptionally(t);
                                                }
                                            });
                                        } else {
                                            // insert payments
                                            List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                                            for (int i = 0; i < pLen; i++) {
                                                JsonObject payment = payments.getJsonObject(i);
                                                payment.put("ticket_id", ticket.getInteger("id"));
                                                pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, parcelId, cashOutId, createdBy, is_credit));
                                            }
                                            CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pLen]));

                                            allPayments.whenComplete((s, tt) -> {
                                                try {
                                                    if (tt != null) {
                                                        throw tt;
                                                    }
                                                    conn.updateWithParams(update.getQuery(), update.getParams(), replyUpdate -> {
                                                        try {
                                                            if (replyUpdate.failed()) {
                                                                throw replyUpdate.cause();
                                                            }
                                                            JsonObject paramMovPayback = new JsonObject()
                                                                    .put("customer_id", parcel.getInteger(Constants.CUSTOMER_ID))
                                                                    .put("points", paybackPoints)
                                                                    .put("money", paybackMoney)
                                                                    .put("type_movement", "I")
                                                                    .put("motive", "Envio de paquetería(sender)")
                                                                    .put("id_parent", parcelId)
                                                                    .put("employee_id", createdBy);
                                                            objPayback.generateMovementPayback(conn, paramMovPayback).whenComplete((movementPayback, errorMP) -> {
                                                                try {
                                                                    if (errorMP != null) {
                                                                        throw errorMP;
                                                                    }

                                                                    this.insertService(conn, parcel , createdBy , radObject , parcelId).whenComplete((replyService , errorService) -> {
                                                                        try{
                                                                            if (errorService != null){
                                                                                throw errorService;
                                                                            }

                                                                            JsonObject result = new JsonObject().put("id", parcelId);
                                                                            result.put("ticket_id", ticket.getInteger("id"))
                                                                                   .put("tracking_code", parcel.getString("parcel_tracking_code"));
                                                                             future.complete(result);

                                                                        } catch (Throwable t){
                                                                            t.printStackTrace();
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
                                            });
                                        }
                                    } catch (Throwable t){
                                        future.completeExceptionally(t);
                                    }
                                });
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
                    }
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * Calculate the delivery promise date according to the business rules
     * @return promise delivery date of parcel
     */
    private Date getParcelPromiseDeliveryDate() throws ParseException{
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(UtilsDate.timezone);
        cal.setTime(getDateConvertedTimeZone(UtilsDate.timezone, new Date()));
        cal.add(Calendar.DAY_OF_WEEK, 1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return getDateConvertedTimeZone(serverTimezone, cal.getTime());
    }

    private CompletableFuture<Boolean> comapreCityId(String typeCustomer, Integer customerId, Integer terminalId, Boolean is_complement){

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            if(is_complement){
                future.complete(true);
                return future;
            }
            String QUERY_GET_CITY = null;
            if (typeCustomer.equals("sender")){
                QUERY_GET_CITY = QUERY_GET_PARCEL_CITY_SENDER;
            } else if (typeCustomer.equals("addressee")){
                QUERY_GET_CITY = QUERY_GET_PARCEL_CITY_ADDRESSEE;
            }

            this.dbClient.queryWithParams(QUERY_GET_CITY, new JsonArray().add(customerId),replyCustomer ->{
                try{
                    if(replyCustomer.failed()){
                        throw new Exception(replyCustomer.cause());
                    }
                    List<JsonObject> result = replyCustomer.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("city_id of " + typeCustomer + " not found");
                    }
                    int customerCityId = result.get(0).getInteger("city_id");
                    this.dbClient.queryWithParams(QUERY_GET_TERMINAL_CITY, new JsonArray().add(terminalId),replyTerminal ->{
                        try {
                            if (replyTerminal.failed()){
                                throw replyTerminal.cause();
                            }
                            List<JsonObject> resultTerminal = replyCustomer.result().getRows();
                            if (resultTerminal.isEmpty()){
                                throw new Exception("city_id of " + typeCustomer + " not found");
                            }
                            int terminalCityId = resultTerminal.get(0).getInteger("city_id");
                            if (customerCityId == terminalCityId){
                                future.complete(true);
                            } else {
                                future.complete(false);
                            }
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });

                }catch (Exception ex){
                    future.completeExceptionally(ex);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private void salesCobranzaGen(Message<JsonObject> message){
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String QUERY = QUERY_COBRANZA_REPORT_GEN;
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            params.add(initDate).add(endDate);
            this.dbClient.queryWithParams(QUERY, params, reply ->{
                try {
                    if (reply.succeeded()) {
                        List<JsonObject> resultParcel = reply.result().getRows();
                        if (resultParcel.isEmpty()) {
                            message.reply(new JsonArray());
                        } else {
                            message.reply(new JsonArray(resultParcel));
                        }
                    } else {
                        reportQueryError(message, reply.cause());
                    }
                } catch (Exception e){
                    reportQueryError(message, e.getCause());
                }
            });
        }
        catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }

    private void salesReportGen(Message<JsonObject> message) {
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String QUERY = QUERY_SALES_REPORT_GEN;
            String shippingType = body.getString("shipping_type");
            Integer terminalCityId = body.getInteger("terminal_city_id");
            Integer terminalId = body.getInteger("terminal_id");
            Integer terminalOriginCityId = body.getInteger("terminal_origin_city_id");
            Integer terminalOriginId = body.getInteger("terminal_origin_id");
            Integer terminalDestinyCityId = body.getInteger("terminal_destiny_city_id");
            Integer terminalDestinyId = body.getInteger("terminal_destiny_id");


            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
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
                QUERY += " AND ob.city_id = ?";
                params.add(terminalOriginCityId);
            }
            if(terminalOriginId != null){
                QUERY += " AND p.terminal_origin_id = ?";
                params.add(terminalOriginId);
            }
            if(terminalDestinyCityId != null){
                QUERY += " AND db.city_id = ?";
                params.add(terminalDestinyCityId);
            }
            if(terminalDestinyId != null){
                QUERY += " AND p.terminal_destiny_id = ?";
                params.add(terminalDestinyId);
            }
            if(shippingType != null){
                QUERY += " AND pp.shipping_type = ?";
                params.add(shippingType);
            }


            QUERY = QUERY.concat(" GROUP BY p.parcel_tracking_code ").concat(SALES_REPORT_ORDER_BY);
            this.dbClient.queryWithParams(QUERY, params, reply ->{
                try {
                    if (reply.succeeded()) {
                        List<JsonObject> resultParcel = reply.result().getRows();
                        if (resultParcel.isEmpty()) {
                            message.reply(new JsonArray());
                        } else {
                            List<CompletableFuture<JsonObject>> parcelTask = new ArrayList<>();

                            resultParcel.forEach(parcel -> {
                                parcel.put("shipping_type" , shippingType);
                                parcelTask.add(getSalesPackagesGen(parcel));
                                parcelTask.add(getPaymentsInfo(parcel));
                            });
                            CompletableFuture.allOf(parcelTask.toArray(new CompletableFuture[parcelTask.size()])).whenComplete((ps, pt) -> {
                                try {
                                    if (pt != null) {
                                        reportQueryError(message, pt.getCause());
                                    } else {
                                        message.reply(new JsonArray(resultParcel));
                                    }
                                } catch (Exception e){
                                    reportQueryError(message, e.getCause());
                                }
                            });
                        }
                    } else {
                        reportQueryError(message, reply.cause());
                    }
                } catch (Exception e){
                    reportQueryError(message, e.getCause());
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }

    private CompletableFuture<JsonObject> getSalesPackagesGen(JsonObject parcel){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(parcel.getInteger("id"));
        String shippingType = parcel.getString("shipping_type");
        String QUERY = QUERY_SALES_REPORT_PACKAGES_GEN;
        if(shippingType != null){
            params.add(shippingType);
            QUERY += " AND pp.shipping_type = ?";
        }

        QUERY += " GROUP BY pp.shipping_type, pp.package_type_id, pp.package_price_id";

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

    private void salesReport_Fecha_Ingresos (Message<JsonObject> message) {  //movdev
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String rangoXfechaPago =  body.getString("RPAGO").toString();
            String QUERY = QUERY_SALES_REPORT;
            System.out.println(rangoXfechaPago);

            if(rangoXfechaPago.equals("true")) //movdev
                QUERY=QUERY_SALES_REPORT_FECHA_PAGO;



            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            params.add(initDate).add(endDate);

            if(body.getInteger("terminal_city_id") != null){
                QUERY += " AND b.city_id = ?";
                params.add(body.getInteger("terminal_city_id"));
            }
            if(body.getInteger("terminal_id") != null){
                QUERY += " AND p.branchoffice_id = ?";
                params.add(body.getInteger("terminal_id"));
            }
            if(body.getInteger("terminal_origin_city_id") != null){
                QUERY += " AND ob.city_id = ?";
                params.add(body.getInteger("terminal_origin_city_id"));
            }
            if(body.getInteger("terminal_origin_id") != null){
                QUERY += " AND p.terminal_origin_id = ?";
                params.add(body.getInteger("terminal_origin_id"));
            }
            if(body.getInteger("terminal_destiny_city_id") != null){
                QUERY += " AND db.city_id = ?";
                params.add(body.getInteger("terminal_destiny_city_id"));
            }
            if(body.getInteger("terminal_destiny_id") != null){
                QUERY += " AND p.terminal_destiny_id = ?";
                params.add(body.getInteger("terminal_destiny_id"));
            }
            //Webdev
            if(body.getInteger("customer_id") != null){
                QUERY += " AND p.customer_id = ?";
                params.add(body.getInteger("customer_id"));
            }
            if(body.getInteger("rangoDesde") != null & body.getInteger("rangoHasta") != null){
                QUERY += " AND p.customer_id BETWEEN ? AND ?";
                params.add(body.getInteger("rangoDesde")).add(body.getInteger("rangoHasta"));
            }
            //WebDev
            if(body.getInteger("status_parcel") != null){
                QUERY += " AND p.parcel_status = ?";
                params.add(body.getInteger("status_parcel"));
            }
            //Webdev
            //if(body.getString("shipping_type") != null){
            //    QUERY += " AND pp.shipping_type = ?";
            //    params.add(body.getString("shipping_type"));
            //}


            if(rangoXfechaPago.equals("true")) //movdev
                QUERY = QUERY.concat(SALES_REPORT_ORDER_BYxFP);
            else
                QUERY = QUERY.concat(SALES_REPORT_ORDER_BY);
            System.out.println(QUERY);
            this.dbClient.queryWithParams(QUERY, params, reply ->{
                try {


                    if (reply.succeeded()) {

                        List<JsonObject> resultParcel = reply.result().getRows();

                        if (resultParcel.isEmpty()) {
                            message.reply(new JsonArray());
                        } else {
                            List<CompletableFuture<JsonObject>> parcelTask = new ArrayList<>();

                            resultParcel.forEach(parcel -> {
                                parcelTask.add(getSalesPackages(parcel));
                                parcelTask.add(getPaymentsInfo_FechaIngresos(parcel)); //movdev
                            });


                            CompletableFuture.allOf(parcelTask.toArray(new CompletableFuture[parcelTask.size()])).whenComplete((ps, pt) -> {
                                try {
                                    if (pt != null) {
                                        reportQueryError(message, pt.getCause());
                                    } else {


                                        message.reply(new JsonArray(resultParcel));
                                    }
                                } catch (Exception e){
                                    reportQueryError(message, e.getCause());
                                }
                            });
                        }
                    } else {
                        reportQueryError(message, reply.cause());
                    }
                } catch (Exception e){
                    reportQueryError(message, e.getCause());
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }

    private CompletableFuture<JsonObject> getPaymentsInfo_FechaIngresos(JsonObject parcel){ ///movdev
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(parcel.getInteger("id"));
        this.dbClient.queryWithParams(QUERY_SALES_REPORT_PAYMENT_INFO_INGRESOS, params, handler->{
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

    private CompletableFuture<Integer> createParcelDelivery(SQLConnection conn, JsonObject deliverBody){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            Integer parcelId = deliverBody.getInteger("id");
            Integer updatedBy = deliverBody.getInteger("updated_by");
            final String credentialType = deliverBody.getString("credential_type");
            final String noCredential = deliverBody.getString("no_credential");

            JsonObject parcelDelivery = new JsonObject()
                    .put("credential_type", credentialType)
                    .put("no_credential", noCredential)
                    .put("created_by", updatedBy);

            if (deliverBody.getString("signature") != null){
                parcelDelivery
                        .put("signature", deliverBody.getString("signature"));
            }

            if (deliverBody.getString("addressee_name") != null && deliverBody.getString("addressee_last_name") != null){
                parcelDelivery
                        .put("name", deliverBody.getString("addressee_name"))
                        .put("last_name", deliverBody.getString("addressee_last_name"));
            }

            if (parcelDelivery.getString("name") != null && parcelDelivery.getString("last_name") != null){
                this.registerParcelDelivery(conn, parcelDelivery).whenComplete((parcelDeliveryId, errorParcelDelivery) -> {
                    try{
                        if (errorParcelDelivery != null){
                            throw errorParcelDelivery;
                        }
                        future.complete(parcelDeliveryId);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } else {
                conn.queryWithParams(QUERY_GET_ADDRESSEE_INFO_BY_PARCEL_ID, new JsonArray().add(parcelId), replyAddresseeInfo -> {
                    try{
                        if (replyAddresseeInfo.failed()) {
                            throw replyAddresseeInfo.cause();
                        }
                        JsonObject resultAddresseeInfo = replyAddresseeInfo.result().getRows().get(0);
                        parcelDelivery
                                .put("name", resultAddresseeInfo.getString("addressee_name"))
                                .put("last_name", resultAddresseeInfo.getString("addressee_last_name"));
                        this.registerParcelDelivery(conn, parcelDelivery).whenComplete((parcelDeliveryId, errorParcelDelivery) -> {
                            try{
                                if (errorParcelDelivery != null){
                                    throw errorParcelDelivery;
                                }
                                future.complete(parcelDeliveryId);
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            }
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Integer> registerParcelDelivery(SQLConnection conn, JsonObject paramInsertParcelsDeliveries){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            String insertParcelsDeliveries = generateGenericCreate("parcels_deliveries", paramInsertParcelsDeliveries);
            conn.update(insertParcelsDeliveries, replyInsertParcelsDeliveries -> {
                try{
                    if (replyInsertParcelsDeliveries.failed()){
                        throw replyInsertParcelsDeliveries.cause();
                    }
                    Integer parcelsDeliveriesId = replyInsertParcelsDeliveries.result().getKeys().getInteger(0);
                    future.complete(parcelsDeliveriesId);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<JsonObject> updateParcelStatus(SQLConnection conn, Integer parcelId, Integer parcelStatus, Double payback, JsonObject customerCreditData, boolean isCredit, double totalAmount, int createdBy, Integer customerId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject bodyUpdate = new JsonObject().put(ID, parcelId)
                .put("parcel_status", parcelStatus)
                .put("delivered_at", sdfDataBase(new Date()));

            if (payback != null){
                bodyUpdate.put("payback", payback);
            }

            if (isCredit){
                bodyUpdate.put("payment_condition", "credit")
                    .put("debt", totalAmount);
            }

            if(customerId != null){
                bodyUpdate.put(Constants.CUSTOMER_ID, customerId);
            }

            String queryUpdateParcel = this.generateGenericUpdateString(this.getTableName(), bodyUpdate);

            conn.update(queryUpdateParcel, replyUpdate -> {
                try {
                    if (replyUpdate.failed()) {
                        throw new Exception(replyUpdate.cause());
                    }

                    if (isCredit){

                        this.updateCustomerCredit(conn, customerCreditData, totalAmount, createdBy, false, false, 0.00)
                            .whenComplete((replyCustomer, errorCustomer) -> {
                                try {
                                    if (errorCustomer != null) {
                                        throw new Exception(errorCustomer);
                                    }

                                    future.complete(replyUpdate.result().toJson());

                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    future.completeExceptionally(t);
                                }
                            });
                    } else {
                        future.complete(replyUpdate.result().toJson());
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }


        return future;
    }


    private void checkTrackingCode(Message<JsonObject> message) {
        Integer trackingCode = message.body().getInteger("parcelTrackingCode");
        JsonArray param = new JsonArray().add(trackingCode);

        this.dbClient.queryWithParams(QUERY_CHECK_TRACKING_CODE, param, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                if(result.isEmpty()){
                    throw new Exception("Element not found");
                }
                message.reply(reply.result().getRows().get(0));
            } catch (Throwable t){
                t.printStackTrace();
                reportQueryError(message, t);
            }
        });
    }

    private void getParcelByTrackingCode (Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            String trackingCode = message.body().getString("parcelTrackingCode");
            Integer updatedBy = message.body().getInteger("updated_by");
            Boolean toPrint = message.body().getBoolean("print");
            getParcelByTrackingCode(conn, trackingCode).whenComplete((parcel, errGPTC) -> {
                try {
                    if(errGPTC != null) {
                        errGPTC.printStackTrace();
                        throw errGPTC;
                    }
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

                                            conn.queryWithParams(GET_PARCELS_PACKAGES_SCANNER_TRACKING_BY_PARCEL_ID, new JsonArray().add(parcelId), replyPPST -> {
                                                try {
                                                    if (replyPPST.failed()){
                                                        throw replyPPST.cause();
                                                    }

                                                    List<JsonObject> parcelsPackagesScannerTracking = replyPPST.result().getRows();
                                                    parcel.put("parcel_packages_scanner_tracking", parcelsPackagesScannerTracking);
                                                    List<String> parcelsPackagesList = parcelsPackagesTracking.stream()
                                                            .map(x -> String.valueOf(x.getInteger("parcel_package_id"))).filter(x -> !Objects.equals(x, "null")).collect(Collectors.toList());
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

                                                                                conn.queryWithParams(QUERY_PARCELS_PREPAID_REMAKED_BY_PARCEL_ID, new JsonArray().add(parcelId), replyGPP -> {
                                                                                    try {
                                                                                        if (replyGPP.failed()) {
                                                                                            throw replyGPP.cause();
                                                                                        }

                                                                                        List<JsonObject> parcelGPP = replyGPP.result().getRows();
                                                                                        if(!parcelGPP.isEmpty()) {
                                                                                            parcel.put("parcel_gpp_remaked", parcelGPP.get(0).getString("guides"));
                                                                                        }
                                                                                        this.commit(conn,message,parcel);
                                                                                    } catch (Throwable t) {
                                                                                        this.rollback(conn, t, message);
                                                                                    }
                                                                                });
                                                                            } catch (Exception e){
                                                                                this.rollback(conn, replyRad.cause(), message);
                                                                            }
                                                                        });
                                                                    } catch (Throwable t){
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
                                                                        this.rollback(conn, replyRad.cause(), message);
                                                                    }
                                                                });
                                                            }
                                                        } catch (Throwable t){
                                                            this.rollback(conn, t, message);
                                                        }
                                                    });
                                                } catch (Throwable t){
                                                    this.rollback(conn, t, message);
                                                }
                                            });
                                        } catch (Throwable t){
                                            this.rollback(conn, t, message);
                                        }
                                    });
                                } catch (Throwable t){
                                    this.rollback(conn, t, message);
                                }
                            });
                        } catch (Throwable t){
                            this.rollback(conn, t, message);
                        }
                    });
                } catch (Throwable t) {
                    this.rollback(conn, t, message);
                }
            });
        });
    }

    private CompletableFuture<JsonObject> getParcelByTrackingCode(SQLConnection conn, String trackingCode) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(trackingCode);
        String QUERY = QUERY_PARCEL_BY_TRACKING_CODE;
        String prefix = trackingCode.substring(0, 2);
        Set<String> parcelPrefixes = new HashSet<>(Arrays.asList("GP", "G1", "G2", "G3", "G4"));
        Set<String> packagePrefixes = new HashSet<>(Arrays.asList("P1", "P2", "P3", "P4"));
        if (parcelPrefixes.contains(prefix)) {
            QUERY = String.format(QUERY, "a.parcel_tracking_code = ?");
        } else if (packagePrefixes.contains(prefix)) {
            QUERY = String.format(QUERY, "pp.package_code = ?");
        } else {
            QUERY = String.format(QUERY, "a.waybill = ?");
        }
        conn.queryWithParams(QUERY, params, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }
                List<JsonObject> resultParcel = reply.result().getRows();
                if (resultParcel.isEmpty()) {
                    if (trackingCode.contains("gpp") || trackingCode.contains("GPP")) {
                        conn.queryWithParams(QUERY_PARCEL_BY_TRACKING_CODE_GPP, new JsonArray().add(trackingCode), replyGpp -> {
                            try {
                                if (replyGpp.failed()) {
                                    throw new Exception(replyGpp.cause());
                                }
                                List<JsonObject> resultParcelGpp = replyGpp.result().getRows();
                                if (resultParcelGpp.isEmpty()) {
                                    throw new Exception("Element not found");
                                }
                                future.complete(resultParcelGpp.get(0));
                            } catch (Throwable t) {
                                future.completeExceptionally(t);
                            }
                        });
                    } else {
                        throw new Exception("Element not found");
                    }
                } else {
                    future.complete(resultParcel.get(0));
                }
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private void getParcelPrint (Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            String trackingCode = message.body().getString("parcelTrackingCode");
            Integer updatedBy = message.body().getInteger("updated_by");
            JsonArray params = new JsonArray().add(trackingCode);
            String QUERY = QUERY_PARCEL_BY_TRACKING_CODE;
            String prefix = trackingCode.substring(0, 2);
            Set<String> parcelPrefixes = new HashSet<>(Arrays.asList("GP", "G1", "G2", "G3", "G4"));
            Set<String> packagePrefixes = new HashSet<>(Arrays.asList("P1", "P2", "P3", "P4"));
            if (parcelPrefixes.contains(prefix)) {
                QUERY = String.format(QUERY, "a.parcel_tracking_code = ?");
            } else if (packagePrefixes.contains(prefix)) {
                QUERY = String.format(QUERY, "pp.package_code = ?");
            } else {
                QUERY = String.format(QUERY, "a.waybill = ?");
            }
            conn.queryWithParams(QUERY, params,reply ->{
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
                    conn.queryWithParams(QUERY_PARCELS_PACKAGES_BY_ID_PARCEL_PRINT,paramParcelPackage,replyParcelPackage->{
                        try {
                            if (replyParcelPackage.failed()){
                                throw new Exception(replyParcelPackage.cause());
                            }
                            List<JsonObject> parcelsPackages = replyParcelPackage.result().getRows();
                            for (JsonObject parcelPackage:parcelsPackages) {
                                if(parcelPackage.getInteger("incidences") > 0){
                                    parcel.put("has_incidences", true);
                                }
                            }
                            conn.queryWithParams(GET_PARCELS_PACKAGES_TRACKING_BY_PARCEL_ID, new JsonArray().add(parcelId), replyPPT -> {
                                try {
                                    if (replyPPT.failed()){
                                        throw new Exception(replyPPT.cause());
                                    }

                                    List<JsonObject> parcelsPackagesTracking = replyPPT.result().getRows();
                                    parcel.put("parcel_packages_tracking",parcelsPackagesTracking);
                                    parcel.put("parcel_packages",parcelsPackages);
                                    if(!parcel.containsKey("has_incidences")){
                                        parcel.put("has_incidences", false);
                                    }
                                    //parcels_packages

                                    this.execUpdatePrintsCounter(conn, new JsonArray().add(parcel), "parcels", updatedBy).whenComplete((resultUpdatePrintsCounter, errorPrintsCounter) -> {
                                        try{
                                            if (errorPrintsCounter != null){
                                                this.rollback(conn, errorPrintsCounter, message);
                                            } else {
                                                this.insertTracking(conn, new JsonArray().add(parcel),"parcels_packages_tracking", null, "parcel_id", null, "printed", updatedBy)
                                                        .whenComplete((resultTrackingParcel, errorTrackingParcel) -> {
                                                            try{
                                                                if(errorTrackingParcel != null){
                                                                    this.rollback(conn, errorTrackingParcel, message);
                                                                } else {
                                                                    conn.queryWithParams(QUERY_PARCELS_RAD_BY_ID_PARCEL_PRINT, paramParcelPackage , replyRad -> {
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
                                                                }
                                                            } catch(Exception e){
                                                                this.rollback(conn, e.getCause(), message);
                                                            }
                                                        });
                                            }
                                        } catch(Exception e){
                                            this.rollback(conn, e.getCause(), message);
                                        }
                                    });
                                }catch(Exception e ){
                                    e.printStackTrace();
                                    this.rollback(conn, replyPPT.cause(), message);

                                }

                            });


                        }catch(Exception e ){
                            e.printStackTrace();
                            this.rollback(conn, replyParcelPackage.cause(), message);
                        }
                    });
                }catch (Exception ex){
                    ex.printStackTrace();
                    this.rollback(conn, reply.cause(), message);
                }
            });
        });
    }
//para que pase
    private void advancedSearch (Message<JsonObject> message) {
        JsonObject body = message.body();
        Boolean includeFinished = body.getBoolean("include_finished");
        Boolean paysSender = body.getBoolean("pays_sender");
        String senderName = body.getString("sender_name");
        String senderLastName = body.getString("sender_last_name");
        String addresseeName = body.getString("addressee_name");
        String addresseeLastName = body.getString("addressee_last_name");
        Integer terminalOriginId = body.getInteger("terminal_origin_id");
        Integer terminalDestinyId = body.getInteger("terminal_destiny_id");
        String initDate = body.getString("init_date");
        String endDate = body.getString("end_date");

        JsonArray param = new JsonArray()
                .add(paysSender)
                .add(senderName)
                .add(senderLastName)
                .add(addresseeName)
                .add(addresseeLastName)
                .add(terminalOriginId)
                .add(terminalDestinyId)
                .add(initDate)
                .add(endDate);

        if (includeFinished) { param.add(-1); } else { param.add(2); }

        this.dbClient.queryWithParams(QUERY_PARCEL_ADVANCED_SEARCH, param, reply ->{
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> resultParcel = reply.result().getRows();
                if(resultParcel.isEmpty()){
                    message.reply(new JsonArray());
                } else {
                    message.reply(new JsonArray().add(resultParcel));
                }
            }catch (Exception ex){
                ex.printStackTrace();
                reportQueryError(message, reply.cause());
            }
        });
    }

    private CompletableFuture<JsonObject> registerPackingsInParcel(SQLConnection conn, JsonObject parcelPacking, Integer parcelId, Integer createdBy) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        parcelPacking.put("parcel_id", parcelId)
                .put("created_by", createdBy);

        Integer packingsId = parcelPacking.getInteger("packing_id");

        if (packingsId == null) {
            future.complete(new JsonObject());
            //future.completeExceptionally(new Throwable("Packing: Invalid ID"));
            return future;
        }

        JsonArray params = new JsonArray()
                .add(packingsId);

        conn.queryWithParams(QUERY_PACKINGS_BY_ID, params, reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> rows = reply.result().getRows();
                if (rows.isEmpty()) {
                    future.completeExceptionally(new Throwable("Packing: Not found"));
                } else {
                    JsonObject packing = rows.get(0);
                    Double cost = packing.getDouble("cost", 0.0);
                    Integer quantity = parcelPacking.getInteger("quantity");
                    Double amount = cost * quantity;
                    parcelPacking.put("unit_price", cost);
                    parcelPacking.put("amount", amount);
                    parcelPacking.put("discount", 0.0);
                    parcelPacking.put("total_amount", amount);

                    String insert = this.generateGenericCreate("parcels_packings", parcelPacking);

                    conn.update(insert, replyInsert -> {

                        try {
                            if (replyInsert.failed()){
                                throw new Exception(replyInsert.cause());
                            }

                            future.complete(parcelPacking);

                        }catch(Exception e ){
                            e.printStackTrace();
                            future.completeExceptionally(e);
                        }

                    });

                }

            }catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    private CompletableFuture<JsonObject> registerPackagesInParcel(SQLConnection conn, JsonObject parcelPackage, Integer parcelId, Boolean paysSender, Integer createdBy, Integer terminalId, Boolean withoutFreight, Boolean internalCustomer, Boolean isInternalParcel, Integer terminalDestinyId,Boolean isGuiappCanje) {
        parcelPackage.put("parcel_id", parcelId);
        parcelPackage.put("created_by", createdBy);
        parcelPackage.put("package_code", UtilsID.generateID("P"));
        JsonArray incidences = parcelPackage.containsKey("packages_incidences") ? (JsonArray) parcelPackage.remove("packages_incidences") : new JsonArray();
        parcelPackage.remove("packing_cost");
        parcelPackage.remove("promo_discount_package_price");
        parcelPackage.remove("package_price_distance_id");
        parcelPackage.remove("package_price_name");
        parcelPackage.remove("promo_discount");
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        String queryPacking = "SELECT * FROM packings WHERE id=?;";
        if(parcelPackage.getInteger("packing_id") != null && parcelPackage.getInteger("packing_id") != 0) {
            this.dbClient.queryWithParams(queryPacking, new JsonArray().add(parcelPackage.getInteger("packing_id")), replyPacking -> {
                try{
                    if(replyPacking.failed()){
                        throw new Exception(replyPacking.cause());
                    }
                    JsonObject packing = replyPacking.result().getRows().get(0);
                    if(isGuiappCanje){
                        parcelPackage.put(AMOUNT, 0.00);
                        parcelPackage.put(DISCOUNT, 0.0);
                        parcelPackage.put(TOTAL_AMOUNT, 0.00);
                        packing.getDouble("cost");
                    }
                    if (withoutFreight){
                        parcelPackage.put(AMOUNT, 0.00);
                        parcelPackage.put(DISCOUNT, 0.0);
                        parcelPackage.put(TOTAL_AMOUNT, 0.00);
                        packing.getDouble("cost");
                    } else {
                        parcelPackage.put("amount", packing.getDouble("cost"));
                        parcelPackage.put("discount", 0.0);
                        Double totalAmount = parcelPackage.getDouble("amount") - parcelPackage.getDouble("discount");
                        parcelPackage.put("total_amount", totalAmount);
                    }

                    this.updatePackage(conn, parcelPackage, incidences, parcelId, createdBy, paysSender, terminalId).whenComplete((result, stThrow) -> {
                        try {
                            if (stThrow != null){
                                throw new Exception(stThrow);
                            }
                            future.complete(parcelPackage);
                        } catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });
                }catch (Exception ex){
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });
        } else {
            if(isGuiappCanje){
                parcelPackage.put(AMOUNT, 0.00);
                parcelPackage.put(DISCOUNT, 0.0);
                parcelPackage.put(TOTAL_AMOUNT, 0.00);
            }
            if (withoutFreight){
                parcelPackage.put(AMOUNT, 0.00);
                parcelPackage.put(DISCOUNT, 0.0);
                parcelPackage.put(TOTAL_AMOUNT, 0.00);
                parcelPackage.remove("cost");
            } else {
                Double amount = Double.parseDouble(String.valueOf(parcelPackage.remove("cost")));
                Double discount = isInternalParcel ? amount : parcelPackage.getDouble(DISCOUNT);
                Double totalAmount = isInternalParcel ? 0.00 : parcelPackage.getDouble(TOTAL_AMOUNT, amount);

                parcelPackage.put(AMOUNT, amount);
                parcelPackage.put(DISCOUNT, discount);
                parcelPackage.put(TOTAL_AMOUNT, totalAmount);
            }

            this.updatePackage(conn, parcelPackage.copy(), incidences, parcelId, createdBy, paysSender, terminalId).whenComplete((result, stThrow) -> {
                try {
                    if (stThrow != null) {
                        throw new Exception(stThrow);
                    }
                    future.complete(parcelPackage);
                } catch (Exception e){
                    future.completeExceptionally(e);
                }
            });
        }
        return future;
    }

    private CompletableFuture<JsonObject> updatePackage(SQLConnection conn, JsonObject parcelPackage, JsonArray incidences, Integer parcelId, Integer createdBy, Boolean paysSender, Integer terminalId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String notes = parcelPackage.getString("notes");
        parcelPackage.remove("notes");
        parcelPackage.remove(_DISTANCE_KM);
        parcelPackage.remove(PACKAGE_PRICE_NAME);
        String update = this.generateGenericCreate("parcels_packages", parcelPackage);
        conn.update(update, (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                final int id = reply.result().getKeys().getInteger(0);
                parcelPackage.put("id", id);

                this.addParcelPackageTracking(conn, parcelId, parcelPackage, notes, createdBy, paysSender, terminalId).whenComplete((s, t) -> {
                    try {
                        if (t != null){
                            throw new Exception(t);
                        }

                        if (incidences.isEmpty()) {
                            future.complete(parcelPackage);
                        } else {
                            final int len = incidences.size();
                            List<String> inserts = new ArrayList<>();

                            for(int i = 0; i < len; i++) {
                                JsonObject incidence = incidences.getJsonObject(i);
                                incidence.put("parcel_id", parcelId);
                                incidence.put("parcel_package_id", id);
                                incidence.put("created_by", createdBy);
                                inserts.add(this.generateGenericCreate("parcels_incidences", incidence));
                            }

                            conn.batch(inserts, (AsyncResult<List<Integer>> ar) -> {
                                if(ar.succeeded()) {
                                    future.complete(parcelPackage);
                                } else {
                                    future.completeExceptionally(ar.cause());
                                }
                            });
                        }
                    } catch (Exception e){
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

    private CompletableFuture<JsonObject> addParcelPackageTracking(SQLConnection conn, Integer parcelId, JsonObject parcelPackage, String notes, Integer createdBy, Boolean paysSender, Integer terminalId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            JsonObject parcelPackageTracking = new JsonObject()
                    .put("parcel_id", parcelId)
                    .put("parcel_package_id", parcelPackage.getInteger("id"))
                    .put("action", "register")
                    .put("terminal_id", terminalId)
                    .put("notes", notes)
                    .put("created_by", createdBy);

            String insertParcelPackageTracking = this.generateGenericCreate("parcels_packages_tracking", parcelPackageTracking);

            conn.update(insertParcelPackageTracking, (AsyncResult<UpdateResult> resp) -> {
                try{
                    if(resp.failed()){
                        throw new Exception(resp.cause());
                    }
                    if(paysSender) {
                        parcelPackageTracking.put("action", "paid");
                        conn.update(this.generateGenericCreate("parcels_packages_tracking", parcelPackageTracking), (AsyncResult<UpdateResult> res) -> {
                            try {
                                if (res.failed()){
                                    throw new Exception(res.cause());
                                }
                                future.complete(parcelPackageTracking);
                            } catch (Exception e){
                                future.completeExceptionally(e);
                            }
                        });
                    } else {
                        future.complete(parcelPackageTracking);
                    }

                }catch (Exception ex){
                    ex.printStackTrace();
                    future.completeExceptionally(resp.cause());
                }
            });
        } catch (Exception e){
            future.completeExceptionally(e);
        }

        return future;
    }
    private CompletableFuture<JsonObject> insertTicket(SQLConnection conn, Integer cashOutId, Integer parcelId, Double totalPayments, JsonObject cashChange, Integer createdBy, Double ivaPercent, String action) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject ticket = new JsonObject();
            Double iva = this.getIva(totalPayments, ivaPercent);

            // Create ticket_code
            // ticket.put("ticket_code", ticketCode);
            ticket.put("parcel_id", parcelId);
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

    private CompletableFuture<JsonObject> insertPaymentAndCashOutMove(SQLConnection conn, JsonObject payment, Integer currencyId, Integer packageId, Integer cashOutId, Integer createdBy, Boolean is_credit) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if (!is_credit) {
                JsonObject cashOutMove = new JsonObject();
                payment.put("currency_id", currencyId);
                payment.put("parcel_id", packageId);
                payment.put("created_by", createdBy);
                cashOutMove.put("quantity", payment.getDouble("amount"));
                cashOutMove.put("move_type", "0");
                cashOutMove.put("cash_out_id", cashOutId);
                cashOutMove.put("created_by", createdBy);
                //String insert = this.generateGenericCreate("payment", payment);
                PaymentDBV objPayment = new PaymentDBV();
                objPayment.insertPayment(conn, payment).whenComplete((resultPayment, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        payment.put("id", resultPayment.getInteger("id"));
                        cashOutMove.put("payment_id", resultPayment.getInteger("id"));
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
            } else {
                future.complete(payment);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
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

    private CompletableFuture<JsonObject> checkCancelReason(SQLConnection conn, Integer cancelReasonId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.queryWithParams("SELECT id, responsable, cancel_type FROM parcels_cancel_reasons WHERE id = ?", new JsonArray().add(cancelReasonId), reply -> {
            try{
                if (reply.succeeded()){
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()){
                        future.completeExceptionally(new Throwable("The parcels_cancel_reason_id not exists in the catalog"));
                    } else {
                        JsonObject cancelReason = result.get(0);
                        future.complete(cancelReason);
                    }
                } else {
                    reply.cause().printStackTrace();
                    future.completeExceptionally(reply.cause());
                }
            } catch(Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private  void cancelParcelById(Message<JsonObject> message){
        JsonObject body = message.body();
        int parcelId = body.getInteger(PARCEL_ID);
        int cancelBy = body.getInteger(UPDATED_BY);
        Integer cashOutId = body.getInteger(CASHOUT_ID);
        int cancelReasonId = body.getInteger(PARCELS_CANCEL_REASON_ID);
        int currencyId = body.getInteger("currency_id");
        double ivaPercent = (Double) body.remove("iva_percent");
        Boolean applyInsurance = body.getBoolean("apply_insurance");
        String notes = null;
        String cancelCode = null;
        JsonArray payments = new JsonArray();
        JsonObject cashChange = new JsonObject();

        if(body.getString(CANCEL_CODE) != null){
            cancelCode = body.getString(CANCEL_CODE);
        }
        if(body.getString(NOTES) != null){
            notes = body.getString(NOTES);
        }
        if(body.getJsonArray("payments") != null && !body.getJsonArray("payments").isEmpty()){
            payments = body.getJsonArray("payments");
        }
        if(body.getJsonObject("cash_change") != null && !body.getJsonObject("cash_change").isEmpty()){
            cashChange = body.getJsonObject("cash_change");
        }

        JsonArray finalPayments = payments;
        JsonObject finalCashChange = cashChange;
        String finalNotes = notes;
        String finalCancelCode = cancelCode;
        this.startTransaction(message, conn -> {
            JsonArray params2 = new JsonArray()
                    .add(parcelId);
            conn.queryWithParams(QUERY_GET_PARCEL_PACKAGE_BY_PARCEL_ID, params2, resultHandler->{
                try {
                    if (resultHandler.failed()){
                        throw new Exception(resultHandler.cause());
                    }

                    List<JsonObject> resultParcel = resultHandler.result().getRows();
                    if (resultParcel.isEmpty()){
                        throw new Exception("Parcel wasn't found");
                    }

                    JsonObject parcel = resultParcel.get(0);
                    boolean isCredit = parcel.getString("payment_condition").equals("credit");
                    Integer canceledBy = parcel.getInteger("canceled_by");
                    if(Objects.nonNull(canceledBy)) {
                        throw new Exception("Parcel was canceled");
                    }
                    Integer invoiceId = parcel.getInteger("invoice_id");
                    if(Objects.nonNull(invoiceId)) {
                        throw new Exception("Parcel was invoiced");
                    }

                    this.checkCancelReason(conn, cancelReasonId).whenComplete((resultCancelReason, errorCancelReason) -> {
                        try{
                            if (errorCancelReason != null){
                                throw errorCancelReason;
                            }
                            String cancelReasonResponsable = resultCancelReason.getString("responsable");
                            CANCEL_RESPONSABLE cancelResponsable = CANCEL_RESPONSABLE.valueOf(cancelReasonResponsable.toUpperCase());
                            String cancelReasonType = resultCancelReason.getString("cancel_type");
                            CANCEL_TYPE cancelType = CANCEL_TYPE.valueOf(cancelReasonType.toUpperCase());

                            switch (cancelType){
                                case FAST_CANCEL:
                                    this.parcelFastCancel(conn, message, cashOutId, cancelBy, finalNotes, parcel, finalCancelCode, currencyId, cancelReasonId, isCredit, ivaPercent);
                                    break;
                                case END_CANCEL:
                                    this.parcelEndCancel(conn, message, finalNotes, cancelBy, finalPayments, cashOutId, finalCashChange, finalCancelCode, applyInsurance, parcel, cancelReasonId, isCredit);
                                    break;
                                case REWORK:
                                    try {
                                        UtilsValidation.isGrater(body, SCHEDULE_ROUTE_DESTINATION_ID, 0);
                                        UtilsValidation.isGrater(body, REWORK_SCHEDULE_ROUTE_DESTINATION_ID, 0);
                                        UtilsValidation.isBooleanAndNotNull(body, PAYS_SENDER);
                                        UtilsValidation.isGrater(body, ADDRESSEE_ID, 0);
                                        UtilsValidation.isEmpty(body, ADDRESSEE_NAME);
                                        UtilsValidation.isEmpty(body, ADDRESSEE_LAST_NAME);
                                        UtilsValidation.isPhoneNumber(body, ADDRESSEE_PHONE);
                                        UtilsValidation.isMail(body, ADDRESSEE_EMAIL);
                                        UtilsValidation.isGrater(body, ADDRESSEE_ADDRESS_ID, 0);
                                        parcel.put("payment_condition", "cash");
                                        parcel.put("debt", 0);
                                        this.parcelReworkCancel(conn, message, cancelResponsable, parcel, body, finalNotes, cancelBy, finalCancelCode, finalPayments, finalCashChange, cancelReasonId, false);
                                    } catch (UtilsValidation.PropertyValueException e){
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
                                    }
                                    break;
                                case RETURN:
                                    try {
                                        UtilsValidation.isGrater(body, SCHEDULE_ROUTE_DESTINATION_ID, 0);
                                        UtilsValidation.isBooleanAndNotNull(body, PAYS_SENDER);
                                        parcel.put("payment_condition", "cash");
                                        parcel.put("debt", 0);
                                        this.parcelReturnCancel(conn, message, cancelReasonResponsable, parcel, body, finalNotes, cancelBy, finalCancelCode, finalPayments, finalCashChange, cancelReasonId, false);
                                    } catch (UtilsValidation.PropertyValueException e){
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
                                    }
                                    break;
                            }
                        } catch (Throwable t){
                            t.printStackTrace();
                            this.rollback(conn, t, message);
                        }
                    });
                } catch(Exception ex){
                    ex.printStackTrace();
                    this.rollback(conn, ex, message);
                }
            });
        });
    }

    private void parcelFastCancel(SQLConnection conn, Message<JsonObject> message, Integer cashOutId,
                                  Integer cancelBy, String finalNotes, JsonObject parcel, String cancelCode, Integer currencyId, Integer cancelReasonId, boolean is_credit, double ivaPercent){
        try {
            Integer parcelId = parcel.getInteger("id");
            PARCEL_STATUS parcelStatus = PARCEL_STATUS.values()[parcel.getInteger(_PARCEL_STATUS)];
            Boolean paysSender = parcel.getBoolean("pays_sender");

            if (!parcelStatus.equals(PARCEL_STATUS.DOCUMENTED)
                    && !parcelStatus.equals(PARCEL_STATUS.IN_TRANSIT)
                    && !parcelStatus.equals(PARCEL_STATUS.ARRIVED)
                    && !parcelStatus.equals(PARCEL_STATUS.ARRIVED_INCOMPLETE)
                    && !parcelStatus.equals(PARCEL_STATUS.LOCATED)
                    && !parcelStatus.equals(PARCEL_STATUS.LOCATED_INCOMPLETE)){
                throw new Exception("Parcel was delivered or canceled");
            }

            String createdAt = format_yyyy_MM_dd(getDateConvertedTimeZone(timezone, parse_yyyy_MM_dd_T_HH_mm_ss(parcel.getString(CREATED_AT))));
            String today = format_yyyy_MM_dd(getDateConvertedTimeZone(timezone, new Date()));
            if(!createdAt.equals(today)) {
                throw new Throwable("Parcel can't cancel");
            }

            if(paysSender && !is_credit){
                conn.queryWithParams("SELECT total_amount FROM parcels WHERE id = ?;", new JsonArray().add(parcelId), replyTAParcel -> {
                    try{
                        if(replyTAParcel.succeeded()){
                            Double amountToReturn = replyTAParcel.result().getRows().get(0).getDouble("total_amount");
                            this.returnMoney(conn, cashOutId, currencyId, "Devolución por cancelación de paquetería", "parcel_id", parcelId, amountToReturn, cancelBy, is_credit, parcel.getInteger("sender_id"), ivaPercent).whenComplete((resultReturnMoney, errorReturnMoney) -> {
                                try {
                                    if (errorReturnMoney != null){
                                        throw errorReturnMoney;
                                    }
                                    this.setParcelStatus(conn, parcelId,
                                                    PARCEL_STATUS.CANCELED.ordinal(), PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                                    ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase(), resultReturnMoney, cancelBy, cancelCode, cancelReasonId, is_credit)
                                            .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                                try{
                                                    if (errorParcelStatus != null) {
                                                        throw errorParcelStatus;
                                                    }
                                                    this.setRadEadStatus(conn, parcelId).whenComplete((resultRadEadCancel , errorResultRadEadCancel) -> {
                                                        try{
                                                            if(errorResultRadEadCancel != null){
                                                                throw errorResultRadEadCancel;
                                                            }
                                                            this.commit(conn, message, resultParcelStatus);
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
                        } else {
                            replyTAParcel.cause().printStackTrace();
                            this.rollback(conn, replyTAParcel.cause(), message);
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        this.rollback(conn, e, message);
                    }
                });
            } else {
                this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, is_credit)
                    .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                        try{
                            if (errorParcelStatus != null) {
                                errorParcelStatus.printStackTrace();
                                this.rollback(conn, errorParcelStatus, message);
                            } else {
                                this.insertTicket(conn, null, parcelId, 0.00, null, cancelBy, ivaPercent,  "voucher").whenComplete((JsonObject ticketV, Throwable ticketErrorV) -> {
                                    try{
                                        if (ticketErrorV != null) {
                                            this.rollback(conn, ticketErrorV, message);
                                        } else {
                                            JsonObject ticketDetail = new JsonObject();
                                            ticketDetail.put("ticket_id", ticketV.getInteger("id"));
                                            ticketDetail.put("quantity", 1);
                                            ticketDetail.put("detail", "Comprobante de cancelación de paquetería");
                                            ticketDetail.put("unit_price", 0.00);
                                            ticketDetail.put("amount", 0.00);
                                            ticketDetail.put("created_by", cancelBy);

                                            String insertTicketDetail = this.generateGenericCreate("tickets_details", ticketDetail);

                                            conn.update(insertTicketDetail, replyInsertTicketDetail -> {
                                                try {
                                                    if (replyInsertTicketDetail.succeeded()){
                                                        if(is_credit){
                                                            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT);
                                                            JsonObject paramsCredit = new JsonObject().put(Constants.CUSTOMER_ID, parcel.getInteger(Constants.CUSTOMER_ID));
                                                            vertx.eventBus().send(CustomerDBV.class.getSimpleName(), paramsCredit, options, (AsyncResult<Message<JsonObject>> replyCredit) -> {
                                                                try{
                                                                    if(replyCredit.failed()) {
                                                                        throw new Exception(replyCredit.cause());
                                                                    }
                                                                    Message<JsonObject> customerCreditDataMsg = replyCredit.result();
                                                                    JsonObject customerCreditData = customerCreditDataMsg.body();

                                                                    this.updateCustomerCredit(conn, customerCreditData, parcel.getDouble("debt"), cancelBy, true, false, 0.00)
                                                                            .whenComplete((replyCustomer, errorCustomer) -> {
                                                                                try{
                                                                                    if (errorCustomer != null) {
                                                                                        throw new Exception(errorCustomer);
                                                                                    }

                                                                                    resultParcelStatus.put("voucher_ticket_id", ticketV.getInteger("id"));
                                                                                    this.commit(conn, message, resultParcelStatus);

                                                                                } catch (Throwable t) {
                                                                                    t.printStackTrace();
                                                                                    this.rollback(conn, t, message);
                                                                                }
                                                                            });

                                                                } catch (Exception e) {
                                                                    e.printStackTrace();
                                                                    this.rollback(conn, e, message);
                                                                }
                                                            });
                                                        }else {
                                                            resultParcelStatus.put("voucher_ticket_id", ticketV.getInteger("id"));
                                                            this.commit(conn, message, resultParcelStatus);
                                                        }
                                                    } else {
                                                        replyInsertTicketDetail.cause().printStackTrace();
                                                        this.rollback(conn, replyInsertTicketDetail.cause(), message);
                                                    }
                                                } catch (Exception e){
                                                    e.printStackTrace();
                                                    this.rollback(conn, e, message);
                                                }
                                            });
                                        }
                                    } catch (Exception e){
                                        this.rollback(conn, e, message);
                                    }
                                });
                            }
                        } catch (Exception e){
                            e.printStackTrace();
                            this.rollback(conn, e, message);
                        }
                    });
            }
        } catch (Throwable t) {
            this.rollback(conn, t, message);
        }

    }

    private void parcelEndCancel(SQLConnection conn, Message<JsonObject> message, String finalNotes, Integer cancelBy, JsonArray finalPayments,
                                 Integer cashOutId, JsonObject finalCashChange, String cancelCode, Boolean applyInsurance, JsonObject parcel, Integer cancelReasonId, boolean isCredit){
        try {
            PARCEL_STATUS currentParcelStatus = PARCEL_STATUS.values()[parcel.getInteger(_PARCEL_STATUS)];
            if(currentParcelStatus.equals(PARCEL_STATUS.IN_TRANSIT)) {
                throw new Throwable("Parcel is in transit");
            }

            Integer parcelId = parcel.getInteger("id");
            Boolean paysSender = parcel.getBoolean("pays_sender");
            Boolean hasInsurance = parcel.getBoolean("has_insurance");
            Double totalAmount = parcel.getDouble("total_amount");
            Integer customerId = parcel.getInteger(BoardingPassDBV.CUSTOMER_ID);
            if(paysSender){
                int parcelStatus = PARCEL_STATUS.DELIVERED_OK.ordinal();
                int packageStatus = PACKAGE_STATUS.DELIVERED.ordinal();
                String packageTracking = ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase();
                if(isCredit) {
                    parcelStatus = PARCEL_STATUS.CANCELED.ordinal();
                    packageStatus = PACKAGE_STATUS.CANCELED.ordinal();
                    packageTracking = ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase();
                }
                this.setParcelStatus(conn, parcelId, parcelStatus,
                                packageStatus, finalNotes, packageTracking, null, cancelBy, cancelCode, cancelReasonId, isCredit)
                        .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                            try{
                                if (errorParcelStatus != null) {
                                    throw new Exception(errorParcelStatus);
                                }

                                this.insertTicketEndCancel(conn, message, resultParcelStatus, parcelId, cancelBy, isCredit, customerId, totalAmount);

                            } catch (Exception e){
                                e.printStackTrace();
                                this.rollback(conn, e, message);
                            }
                        });
            } else {
                if(finalPayments.isEmpty()){
                    this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                    PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                    ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                            .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                try{
                                    if (errorParcelStatus != null) {
                                        throw new Exception(errorParcelStatus);
                                    }

                                    this.insertTicketEndCancel(conn, message, resultParcelStatus, parcelId, cancelBy, isCredit, customerId, totalAmount);

                                } catch (Exception e){
                                    e.printStackTrace();
                                    this.rollback(conn, e, message);
                                }
                            });
                } else {
                    if (hasInsurance){
                        if (applyInsurance){
                            //REGISTRA PAGO
                            conn.queryWithParams("SELECT id FROM parcels_packages WHERE parcel_id = ?", new JsonArray().add(parcelId), replyGetPackages -> {
                                try{
                                    if (replyGetPackages.succeeded()) {
                                        List<JsonObject> packages = replyGetPackages.result().getRows();
                                        this.paymentInsert(conn, finalPayments, parcel, new JsonArray(packages), totalAmount, cashOutId, finalCashChange, cancelBy).whenComplete((resultPaymentInsert, errorPaymentInsert) -> {
                                            try {
                                                if(errorPaymentInsert != null){
                                                    errorPaymentInsert.printStackTrace();
                                                    this.rollback(conn, errorPaymentInsert, message);
                                                } else {
                                                    this.setParcelStatus(conn, parcelId, PARCEL_STATUS.DELIVERED_OK.ordinal(),
                                                                    PACKAGE_STATUS.DELIVERED.ordinal(), finalNotes,
                                                                    ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase(), resultPaymentInsert, cancelBy, cancelCode, cancelReasonId, isCredit)
                                                            .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                                                try{
                                                                    if (errorParcelStatus != null) {
                                                                        errorParcelStatus.printStackTrace();
                                                                        this.rollback(conn, errorParcelStatus, message);
                                                                    } else {
                                                                        this.commit(conn, message, resultParcelStatus);
                                                                    }
                                                                } catch (Exception e){
                                                                    e.printStackTrace();
                                                                    this.rollback(conn, e, message);
                                                                }
                                                            });
                                                }
                                            } catch (Exception e){
                                                e.printStackTrace();
                                                this.rollback(conn, e, message);
                                            }
                                        });
                                    } else {
                                        replyGetPackages.cause().printStackTrace();
                                        this.rollback(conn, replyGetPackages.cause(), message);
                                    }
                                } catch (Exception e){
                                    e.printStackTrace();
                                    this.rollback(conn, e, message);
                                }
                            });
                        } else {
                            this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                            PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                            ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                                    .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                        try{
                                            if (errorParcelStatus != null) {
                                                throw new Exception(errorParcelStatus);
                                            }

                                            this.insertTicketEndCancel(conn, message, resultParcelStatus, parcelId, cancelBy, isCredit, customerId, totalAmount);

                                        } catch (Exception e){
                                            e.printStackTrace();
                                            this.rollback(conn, e, message);
                                        }
                                    });
                        }
                    } else {
                        this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                        PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                        ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                                .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                    try{
                                        if (errorParcelStatus != null) {
                                            throw new Exception(errorParcelStatus);
                                        }

                                        this.insertTicketEndCancel(conn, message, resultParcelStatus, parcelId, cancelBy, isCredit, customerId, totalAmount);

                                    } catch (Exception e){
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
                                    }
                                });
                    }
                }
            }
        } catch (Throwable t) {
           this.rollback(conn, t, message);
        }
    }

    private void insertTicketEndCancel(SQLConnection conn, Message<JsonObject> message, JsonObject resultParcelStatus, Integer parcelId, Integer cancelBy, Boolean is_credit, Integer customerId, Double total_amount){
        this.insertTicket(conn, null, parcelId, 0.00, null, cancelBy, 0.00,  "voucher").whenComplete((JsonObject ticketV, Throwable ticketErrorV) -> {
            try{
                if (ticketErrorV != null) {
                    this.rollback(conn, ticketErrorV, message);
                } else {
                    Integer ticketId = ticketV.getInteger(ID);
                    JsonObject ticketDetail = new JsonObject();
                    ticketDetail.put("ticket_id", ticketId);
                    ticketDetail.put("quantity", 1);
                    ticketDetail.put("detail", "Comprobante de cancelación de paquetería");
                    ticketDetail.put("unit_price", 0.00);
                    ticketDetail.put(AMOUNT, 0.00);
                    ticketDetail.put(CREATED_BY, cancelBy);

                    String insertTicketDetail = this.generateGenericCreate("tickets_details", ticketDetail);

                    conn.update(insertTicketDetail, replyInsertTicketDetail -> {
                        try {
                            if (replyInsertTicketDetail.failed()){
                                throw new Exception(replyInsertTicketDetail.cause());
                            }

                            resultParcelStatus.put("voucher_ticket_id", ticketId);
                            if (is_credit) {
                                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT);
                                JsonObject paramsCredit = new JsonObject().put("customer_id", customerId);
                                vertx.eventBus().send(CustomerDBV.class.getSimpleName(), paramsCredit, options, (AsyncResult<Message<JsonObject>> replyCredit) -> {
                                    try {
                                        if (replyCredit.failed()) {
                                            throw new Exception(replyCredit.cause());
                                        }
                                        Message<JsonObject> customerCreditDataMsg = replyCredit.result();
                                        JsonObject customerCreditData = customerCreditDataMsg.body();
                                        this.updateCustomerCredit(conn, customerCreditData, total_amount, cancelBy, true, false, 0.00)
                                                .whenComplete((replyCustomer, errorCustomer) -> {
                                                    try{
                                                        if (errorCustomer != null) {
                                                            throw new Exception(errorCustomer);
                                                        }
                                                        this.commit(conn, message, resultParcelStatus);

                                                    } catch (Throwable t) {
                                                        t.printStackTrace();
                                                        this.rollback(conn, t, message);
                                                    }
                                                });

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
                                    }
                                });

                            } else {
                                this.commit(conn, message, resultParcelStatus);
                            }


                        } catch (Exception e){
                            e.printStackTrace();
                            this.rollback(conn, e, message);
                        }
                    });
                }
            } catch (Exception e){
                e.printStackTrace();
                this.rollback(conn, e, message);
            }
        });
    }

    private void parcelReworkCancel(SQLConnection conn, Message<JsonObject> message, CANCEL_RESPONSABLE cancelResponsable, JsonObject parcel,
                                    JsonObject cancelBody, String finalNotes, Integer cancelBy, String cancelCode, JsonArray payments, JsonObject cashChange, Integer cancelReasonId, boolean isCredit){
        try {
            PARCEL_STATUS currentParcelStatus = PARCEL_STATUS.values()[parcel.getInteger(_PARCEL_STATUS)];
            if (!currentParcelStatus.equals(PARCEL_STATUS.DOCUMENTED)
                    && !currentParcelStatus.equals(PARCEL_STATUS.IN_TRANSIT)
                    && !currentParcelStatus.equals(PARCEL_STATUS.ARRIVED)
                    && !currentParcelStatus.equals(PARCEL_STATUS.ARRIVED_INCOMPLETE)
                    && !currentParcelStatus.equals(PARCEL_STATUS.LOCATED)
                    && !currentParcelStatus.equals(PARCEL_STATUS.LOCATED_INCOMPLETE)){
                throw new Exception("Parcel was delivered or canceled");
            }

            Boolean paysSender = parcel.getBoolean(PAYS_SENDER);
            Integer parcelId = parcel.getInteger(ID);
            Integer scheduleRouteDestinationId = cancelBody.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);
            Integer reworkScheduleRouteDestinationId = cancelBody.getInteger(REWORK_SCHEDULE_ROUTE_DESTINATION_ID);
            parcel.put(PAYS_SENDER, cancelBody.getBoolean(PAYS_SENDER))
                    .put(REISSUE, true)
                    .put(REWORK, true)
                    .put(REWORK_SCHEDULE_ROUTE_DESTINATION_ID, reworkScheduleRouteDestinationId)
                    .put(SCHEDULE_ROUTE_DESTINATION_ID, scheduleRouteDestinationId)
                    .put("credit_customer_id", paysSender ? parcel.getInteger(BoardingPassDBV.CUSTOMER_ID): parcel.getInteger(ADDRESSEE_ID));

            if (cancelResponsable.equals(CANCEL_RESPONSABLE.CUSTOMER)){
                try {
                    if (payments == null || payments.isEmpty()){
                        throw new UtilsValidation.PropertyValueException("payments", MISSING_REQUIRED_VALUE);
                    }
                    if (cashChange == null){
                        throw new UtilsValidation.PropertyValueException("cash_change", MISSING_REQUIRED_VALUE);
                    }
                    parcel.put(WITHOUT_FREIGHT, false);
                    if (paysSender){
                        parcel.put(CUMULATIVE_COST, isCredit);
                        this.setParcelStatus(conn, parcelId, PARCEL_STATUS.DELIVERED_OK.ordinal(),
                                        PACKAGE_STATUS.DELIVERED.ordinal(), finalNotes,
                                        ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                                .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                    try{
                                        if (errorParcelStatus != null) {
                                            throw new Exception(errorParcelStatus);
                                        }

                                        parcel.mergeIn(resultParcelStatus);

                                        this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                            try {
                                                if (errorParcelRegisterObject != null){
                                                    throw errorParcelRegisterObject;
                                                }

                                                this.commit(conn, message, parcelRegisterObject);

                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    } catch (Exception e){
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
                                    }
                                });
                    } else {
                        parcel.put(CUMULATIVE_COST, true);
                        this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                        PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                        ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase(), null, cancelBy, cancelCode,cancelReasonId, isCredit)
                                .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                    try{
                                        if (errorParcelStatus != null) {
                                            throw new Exception(errorParcelStatus);
                                        }

                                        parcel.mergeIn(resultParcelStatus);

                                        this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                            try {
                                                if (errorParcelRegisterObject != null){
                                                    throw errorParcelRegisterObject;
                                                }

                                                this.commit(conn, message, parcelRegisterObject);

                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    } catch (Exception e){
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
                                    }
                                });
                    }
                } catch (UtilsValidation.PropertyValueException ex){
                    ex.printStackTrace();
                    this.rollback(conn, ex, message);
                }
            } else if (cancelResponsable.equals(CANCEL_RESPONSABLE.COMPANY)){
                if (paysSender){
                    parcel.put(CUMULATIVE_COST, isCredit);
                    parcel.put(WITHOUT_FREIGHT, true);
                    this.setParcelStatus(conn, parcelId, PARCEL_STATUS.DELIVERED_OK.ordinal(),
                                    PACKAGE_STATUS.DELIVERED.ordinal(), finalNotes,
                                    ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                            .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                try{
                                    if (errorParcelStatus != null) {
                                        throw new Exception(errorParcelStatus);
                                    }

                                    parcel.mergeIn(resultParcelStatus);

                                    this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                        try {
                                            if (errorParcelRegisterObject != null){
                                                throw errorParcelRegisterObject;
                                            }

                                            this.commit(conn, message, parcelRegisterObject);

                                        } catch (Throwable t){
                                            t.printStackTrace();
                                            this.rollback(conn, t, message);
                                        }
                                    });

                                } catch (Exception e){
                                    e.printStackTrace();
                                    this.rollback(conn, e, message);
                                }
                            });
                } else {
                    parcel.put(CUMULATIVE_COST, false);
                    parcel.put(WITHOUT_FREIGHT, false);
                    this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                    PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                    ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                            .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                try{
                                    if (errorParcelStatus != null) {
                                        throw new Exception(errorParcelStatus);
                                    }

                                    parcel.mergeIn(resultParcelStatus);

                                    this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                        try {
                                            if (errorParcelRegisterObject != null){
                                                throw errorParcelRegisterObject;
                                            }

                                            this.commit(conn, message, parcelRegisterObject);

                                        } catch (Throwable t){
                                            t.printStackTrace();
                                            this.rollback(conn, t, message);
                                        }
                                    });

                                } catch (Exception e){
                                    e.printStackTrace();
                                    this.rollback(conn, e, message);
                                }
                            });
                }
            } else {
                throw new Throwable("Parcel can't cancel");
            }
        } catch (Throwable t) {
            this.rollback(conn, t, message);
        }
    }

    private void parcelReturnCancel(SQLConnection conn, Message<JsonObject> message, String cancelReasonResponsable, JsonObject parcel,
                                    JsonObject cancelBody, String finalNotes, Integer cancelBy, String cancelCode, JsonArray payments, JsonObject cashChange, Integer cancelReasonId, boolean isCredit){
        try {
            PARCEL_STATUS currentParcelStatus = PARCEL_STATUS.values()[parcel.getInteger(_PARCEL_STATUS)];
            if (!currentParcelStatus.equals(PARCEL_STATUS.DOCUMENTED)
                    && !currentParcelStatus.equals(PARCEL_STATUS.IN_TRANSIT)
                    && !currentParcelStatus.equals(PARCEL_STATUS.ARRIVED)
                    && !currentParcelStatus.equals(PARCEL_STATUS.ARRIVED_INCOMPLETE)
                    && !currentParcelStatus.equals(PARCEL_STATUS.LOCATED)
                    && !currentParcelStatus.equals(PARCEL_STATUS.LOCATED_INCOMPLETE)){
                throw new Exception("Parcel was delivered or canceled");
            }

            parcel.put(REISSUE, true);
            Boolean paysSender = parcel.getBoolean(PAYS_SENDER);
            parcel.put(PAYS_SENDER, cancelBody.getBoolean(PAYS_SENDER));
            Integer parcelId = parcel.getInteger(ID);
            Integer scheduleRouteDestinationId = cancelBody.getInteger(SCHEDULE_ROUTE_DESTINATION_ID);
            parcel.put(SCHEDULE_ROUTE_DESTINATION_ID, scheduleRouteDestinationId)
                    .put("credit_customer_id", paysSender ? parcel.getInteger(BoardingPassDBV.CUSTOMER_ID): parcel.getInteger(ADDRESSEE_ID));

            this.swapResponsable(parcel);

            if (cancelReasonResponsable.equalsIgnoreCase(CANCEL_RESPONSABLE.CUSTOMER.name())){
                try {
                    if (payments == null || payments.isEmpty()){
                        throw new UtilsValidation.PropertyValueException("payments", MISSING_REQUIRED_VALUE);
                    }
                    if (cashChange == null){
                        throw new UtilsValidation.PropertyValueException("cash_change", MISSING_REQUIRED_VALUE);
                    }
                    parcel.put(WITHOUT_FREIGHT, false);
                    if (paysSender){
                        parcel.put(CUMULATIVE_COST, isCredit);
                        this.setParcelStatus(conn, parcelId, PARCEL_STATUS.DELIVERED_OK.ordinal(),
                                        PACKAGE_STATUS.DELIVERED.ordinal(), finalNotes,
                                        ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                                .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                    try{
                                        if (errorParcelStatus != null) {
                                            throw new Exception(errorParcelStatus);
                                        }

                                        parcel.mergeIn(resultParcelStatus);

                                        this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                            try {
                                                if (errorParcelRegisterObject != null){
                                                    throw errorParcelRegisterObject;
                                                }

                                                this.commit(conn, message, parcelRegisterObject);

                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    } catch (Exception e){
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
                                    }
                                });
                    } else {
                        parcel.put(CUMULATIVE_COST, true);
                        this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                        PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                        ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                                .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                    try{
                                        if (errorParcelStatus != null) {
                                            throw new Exception(errorParcelStatus);
                                        }

                                        parcel.mergeIn(resultParcelStatus);

                                        this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                            try {
                                                if (errorParcelRegisterObject != null){
                                                    throw errorParcelRegisterObject;
                                                }

                                                this.commit(conn, message, parcelRegisterObject);

                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    } catch (Exception e){
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
                                    }
                                });
                    }
                } catch (UtilsValidation.PropertyValueException ex){
                    ex.printStackTrace();
                    this.rollback(conn, ex, message);
                }
            } else if (cancelReasonResponsable.equalsIgnoreCase(CANCEL_RESPONSABLE.COMPANY.name())){
                if (paysSender){
                    parcel.put(CUMULATIVE_COST, isCredit);
                    parcel.put(WITHOUT_FREIGHT, true);
                    this.setParcelStatus(conn, parcelId, PARCEL_STATUS.DELIVERED_OK.ordinal(),
                                    PACKAGE_STATUS.DELIVERED.ordinal(), finalNotes,
                                    ParcelsPackagesTrackingDBV.ACTION.DELIVERED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                            .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                try{
                                    if (errorParcelStatus != null) {
                                        throw new Exception(errorParcelStatus);
                                    }

                                    parcel.mergeIn(resultParcelStatus);

                                    this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                        try {
                                            if (errorParcelRegisterObject != null){
                                                throw errorParcelRegisterObject;
                                            }

                                            this.commit(conn, message, parcelRegisterObject);

                                        } catch (Throwable t){
                                            t.printStackTrace();
                                            this.rollback(conn, t, message);
                                        }
                                    });

                                } catch (Exception e){
                                    e.printStackTrace();
                                    this.rollback(conn, e, message);
                                }
                            });
                } else {
                    try {
                        if (payments == null || payments.isEmpty()){
                            throw new UtilsValidation.PropertyValueException("payments", MISSING_REQUIRED_VALUE);
                        }
                        if (cashChange == null){
                            throw new UtilsValidation.PropertyValueException("cash_change", MISSING_REQUIRED_VALUE);
                        }
                        parcel.put(CUMULATIVE_COST, false);
                        parcel.put(WITHOUT_FREIGHT, false);
                        this.setParcelStatus(conn, parcelId, PARCEL_STATUS.CANCELED.ordinal(),
                                        PACKAGE_STATUS.CANCELED.ordinal(), finalNotes,
                                        ParcelsPackagesTrackingDBV.ACTION.CANCELED.name().toLowerCase(), null, cancelBy, cancelCode, cancelReasonId, isCredit)
                                .whenComplete((resultParcelStatus, errorParcelStatus) -> {
                                    try{
                                        if (errorParcelStatus != null) {
                                            throw new Exception(errorParcelStatus);
                                        }

                                        parcel.mergeIn(resultParcelStatus);

                                        this.parcelRegisterFromCancel(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
                                            try {
                                                if (errorParcelRegisterObject != null){
                                                    throw errorParcelRegisterObject;
                                                }

                                                this.commit(conn, message, parcelRegisterObject);

                                            } catch (Throwable t){
                                                t.printStackTrace();
                                                this.rollback(conn, t, message);
                                            }
                                        });

                                    } catch (Exception e){
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
                                    }
                                });
                    } catch (UtilsValidation.PropertyValueException ex){
                        ex.printStackTrace();
                        this.rollback(conn, ex, message);
                    }
                }
            } else {
                this.rollback(conn, new Throwable("Parcel can't cancel"), message);
            }
        } catch (Throwable t) {
            this.rollback(conn, t, message);
        }
    }

    private void swapResponsable(JsonObject parcel){
        Integer originId = parcel.getInteger(_TERMINAL_ORIGIN_ID);
        Integer destinyId = parcel.getInteger(_TERMINAL_DESTINY_ID);
        Integer senderId = parcel.getInteger("sender_id");
        String senderName = parcel.getString("sender_name");
        String senderLastName = parcel.getString("sender_last_name");
        String senderPhone = parcel.getString("sender_phone");
        String senderEmail = parcel.getString("sender_email");
        Integer senderZipCode = parcel.getInteger("sender_zip_code");
        Integer senderAddressId = parcel.getInteger("sender_address_id");

        Integer addresseeId = parcel.getInteger(ADDRESSEE_ID);
        String addresseeName = parcel.getString(ADDRESSEE_NAME);
        String addresseeLastName = parcel.getString(ADDRESSEE_LAST_NAME);
        String addresseePhone = parcel.getString(ADDRESSEE_PHONE);
        String addresseeEmail = parcel.getString(ADDRESSEE_EMAIL);
        Integer addresseeZipCode = parcel.getInteger(ADDRESSEE_ZIP_CODE);
        Integer addresseeAddressId = parcel.getInteger(ADDRESSEE_ADDRESS_ID);

        parcel.put("terminal_origin_id", destinyId)
                .put("terminal_destiny_id", originId)
                .put("sender_id", addresseeId)
                .put("sender_name", addresseeName)
                .put("sender_last_name", addresseeLastName)
                .put("sender_phone", addresseePhone)
                .put("sender_email", addresseeEmail)
                .put("sender_zip_code", addresseeZipCode)
                .put("sender_address_id", addresseeAddressId)
                .put(ADDRESSEE_ID, senderId)
                .put(ADDRESSEE_NAME, senderName)
                .put(ADDRESSEE_LAST_NAME, senderLastName)
                .put(ADDRESSEE_PHONE, senderPhone)
                .put(ADDRESSEE_EMAIL, senderEmail)
                .put(ADDRESSEE_ZIP_CODE, senderZipCode)
                .put(ADDRESSEE_ADDRESS_ID, senderAddressId);
    }

    private CompletableFuture<JsonObject> createParcelRegisterObject(SQLConnection conn, JsonObject body, JsonObject parcel){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray compareArray = new JsonArray()
                .add(ADDRESSEE_ID)
                .add(ADDRESSEE_NAME)
                .add(ADDRESSEE_LAST_NAME)
                .add(ADDRESSEE_PHONE)
                .add(ADDRESSEE_EMAIL)
                .add(ADDRESSEE_ZIP_CODE)
                .add(ADDRESSEE_ADDRESS_ID);

        for (int i=0; i<compareArray.size(); i++){
            if (body.containsKey(compareArray.getString(i)) && body.getValue(compareArray.getString(i)) != null){
                parcel.put(compareArray.getString(i), body.getValue(compareArray.getString(i)));
            }
        }

        Integer parcelId = body.getInteger(PARCEL_ID);
        boolean isCredit = parcel.getString(PAYMENT_CONDITION).equals("credit");
        parcel.put("is_credit", isCredit);
        parcel.put(STATUS, 1);
        parcel.put(PARENT_ID, parcelId);
        parcel.put(INTERNAL_CUSTOMER, body.getBoolean(INTERNAL_CUSTOMER, false));
        parcel.put(CASHOUT_ID, body.getInteger(CASHOUT_ID));
        parcel.put(CASH_REGISTER_ID, body.getInteger(CASH_REGISTER_ID));
        parcel.put(CREATED_BY, body.getInteger(UPDATED_BY));
        parcel.put(CREATED_AT, sdfDataBase(new Date()));
        parcel.put("reissue_debt", parcel.getDouble("debt"));

        if (isCredit){
            Double totalAmount = parcel.getDouble(TOTAL_AMOUNT);
            Double debt = parcel.getDouble("debt", 0.00);
            parcel.put("reissue_paid", totalAmount - debt);
        }

        String newNotes = "Carta porte: ".concat(parcel.getString(WAYBILL));
        if (body.getString(CANCEL_CODE) != null){
            newNotes = newNotes.concat(" Código de cancelación: ").concat(body.getString(CANCEL_CODE));
        }

        parcel.put(NOTES, newNotes);

        this.getPackagesByParcelId(conn, parcelId).whenComplete((resultPackages, errorPackages) -> {
           try {
               if (errorPackages != null){
                   throw errorPackages;
               }

               parcel.put(PARCEL_PACKAGES, resultPackages);

               this.getPackingsByParcelId(conn, parcelId).whenComplete((resultPackings, errorPackings) -> {
                   try {
                       if (errorPackings != null){
                           throw errorPackings;
                       }

                       JsonArray payments = body.getJsonArray("payments");
                       JsonObject cashChange = body.getJsonObject("cash_change");

                       parcel.put(PARCEL_PACKINGS, resultPackings)
                           .put("payments", payments)
                           .put("cash_change", cashChange);

                       this.cleanRegisterObject(parcel);

                       future.complete(parcel);

                   } catch (Throwable t){
                       future.completeExceptionally(t);
                   }
               });
           } catch (Throwable t){
               future.completeExceptionally(t);
           }
        });

        return future;
    }

    private void cleanRegisterObject(JsonObject parcel){
        parcel.remove(ID);
        parcel.remove(TICKET_ID);
        parcel.remove("promo_id");
        parcel.remove(PARCELS_CANCEL_REASON_ID);
        parcel.remove(PAYMENT_CONDITION);
        parcel.remove(_PARCEL_STATUS);
        parcel.remove(STATUS);
        parcel.remove("updated");
        parcel.remove(UPDATED_BY);
        parcel.remove(UPDATED_AT);
    }

    private CompletableFuture<JsonObject> prepareRegister(JsonObject body){

        Boolean isCreditParcel = Boolean.FALSE;
        if (body.containsKey("is_credit")){
            isCreditParcel = body.getBoolean("is_credit");
        }
        boolean paysSender = body.getBoolean(PAYS_SENDER);
        JsonObject customerId = new JsonObject().put(BoardingPassDBV.CUSTOMER_ID, (Integer) body.remove("credit_customer_id"));

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Future<Message<JsonObject>> f1 = Future.future();
        Future<Message<JsonObject>> f2 = Future.future();
        Future<Message<JsonObject>> f3 = Future.future();
        Future<Message<JsonObject>> f4 = Future.future();
        Future<Message<JsonObject>> f5 = Future.future();
        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "currency_id"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "parcel_iva"), new DeliveryOptions().addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());
        vertx.eventBus().send(ScheduleRouteDBV.class.getSimpleName(), new JsonObject().put(SCHEDULE_ROUTE_DESTINATION_ID, body.getInteger(SCHEDULE_ROUTE_DESTINATION_ID)), new DeliveryOptions().addHeader(ACTION, ScheduleRouteDBV.ACTION_GET_TERMINALS_BY_DESTINATION), f4.completer());
        vertx.eventBus().send(CustomerDBV.class.getSimpleName(), customerId, new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT), f5.completer());
        Boolean finalIsCreditParcel = isCreditParcel;
        CompositeFuture.all(f1, f2, f3, f4, f5).setHandler(detailReply -> {
            try {
                if (detailReply.failed()) {
                    throw detailReply.cause();
                }
                Message<JsonObject> ivaPercentMsg = detailReply.result().resultAt(0);
                Message<JsonObject> currencyIdMsg = detailReply.result().resultAt(1);
                Message<JsonObject> parcelIvaMsg = detailReply.result().resultAt(2);
                Message<JsonObject> terminalsDestinationMsg = detailReply.result().resultAt(3);
                Message<JsonObject> customerCreditDataMsg = detailReply.result().resultAt(4);

                JsonObject ivaPercent = ivaPercentMsg.body();
                JsonObject currencyId = currencyIdMsg.body();
                JsonObject parcelIva = parcelIvaMsg.body();
                JsonObject terminalsDestination = terminalsDestinationMsg.body();
                JsonObject customerCreditData = customerCreditDataMsg.body();

                body.put("iva_percent", Double.valueOf(ivaPercent.getString("value")));
                body.put("currency_id", Integer.valueOf(currencyId.getString("value")));
                body.put("parcel_iva", Double.valueOf(parcelIva.getString("value")));
                body.put("parcel_tracking_code", UtilsID.generateID("G"));
                body.put(_TERMINAL_ORIGIN_ID, terminalsDestination.getInteger(_TERMINAL_ORIGIN_ID));
                body.put(_TERMINAL_DESTINY_ID, terminalsDestination.getInteger(_TERMINAL_DESTINY_ID));
                body.put("customer_credit_data", customerCreditData);
                body.put("payment_condition", finalIsCreditParcel ? "credit" : "cash");

                if (finalIsCreditParcel && paysSender) {
                    Double reissueDebt = body.getDouble("reissue_debt", 0.00);
                    Double parcelAvailableCredit = customerCreditData.getDouble("available_credit", 0.00);
                    Boolean parcelHasCredit = customerCreditData.getBoolean("has_credit", false);
                    if (parcelAvailableCredit == null) parcelAvailableCredit = Double.valueOf(0);
                    Double parcelPaymentsAmount = body.getJsonObject("cash_change").getDouble("total");
                    body.put("debt", parcelPaymentsAmount);

                    if (!parcelHasCredit) {
                        throw new Exception("Customer: no credit available");
                    }
                    if (parcelAvailableCredit < (parcelPaymentsAmount + reissueDebt)) {
                        throw new Exception("Customer: Insufficient funds to apply credit");
                    }
                    if(!customerCreditData.getString("services_apply_credit").contains("parcel"))
                        throw new Exception("Customer: service not applicable");
                }

                future.complete(body);

            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> parcelRegisterFromCancel(SQLConnection conn, JsonObject cancelBody, JsonObject parcel){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        this.createParcelRegisterObject(conn, cancelBody, parcel).whenComplete((parcelRegisterObject, errorParcelRegisterObject) -> {
            try {
                if (errorParcelRegisterObject != null){
                    throw errorParcelRegisterObject;
                }

                this.prepareRegister(parcel).whenComplete((resultPrepare, errorPrepare) -> {
                    try {
                        if (errorPrepare != null){
                            throw errorPrepare;
                        }

                        this.register(conn, resultPrepare).whenComplete((resultRegister, errorRegister) -> {
                            try {
                                if (errorRegister != null){
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
            } catch (Throwable t){
               future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getPackagesByParcelId(SQLConnection conn, Integer parcelId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_PACKAGES_BY_PARCEL_ID, new JsonArray().add(parcelId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                future.complete(new JsonArray(result));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private CompletableFuture<JsonArray> getPackingsByParcelId(SQLConnection conn, Integer parcelId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_PACKINGS_BY_PARCEL_ID, new JsonArray().add(parcelId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> result = reply.result().getRows();
                future.complete(new JsonArray(result));
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }


    private CompletableFuture<JsonObject> setParcelStatus(SQLConnection conn, Integer parcelId, Integer parcelStatus, Integer packageStatus, String notes, String trackingAction, Integer ticketId, Integer updatedBy, String cancelCode, Integer cancelReasonId, boolean isCredit){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject parcel = new JsonObject()
                .put(ID, parcelId)
                .put(_PARCEL_STATUS, parcelStatus)
                .put(CANCELED_AT, sdfDataBase(new Date()))
                .put(CANCELED_BY, updatedBy);

        if(parcelStatus.equals(PARCEL_STATUS.DELIVERED_OK.ordinal())){
            parcel.put(DELIVERED_AT, sdfDataBase(new Date()));
        }

        if (cancelCode != null){
            parcel.put(CANCEL_CODE, cancelCode);
        }
        if(cancelReasonId != null ){
            parcel.put(PARCELS_CANCEL_REASON_ID, cancelReasonId);
        }
        if (notes != null){
            parcel.put(NOTES, notes);
        }
        if(cancelReasonId != null){
            parcel.put("parcels_cancel_reason_id", cancelReasonId);
        }
        if(isCredit){
            parcel.put("debt", 0.00);
        }

        String updateParcel = this.generateGenericUpdateString(this.getTableName(), parcel);
        conn.queryWithParams("select id from parcels_prepaid_detail where parcel_status!=4 and parcel_id=?", new JsonArray().add(parcelId), replyGetPrepaid -> {
            try {
                if (replyGetPrepaid.succeeded()){
                    List<JsonObject> guiapp_prepaid_id = replyGetPrepaid.result().getRows();
                    if(guiapp_prepaid_id.size()>0){
                        String query_status_cancel_prepaid="update  parcels_prepaid_detail set parcel_status=4 WHERE parcel_status!=4 AND id="+guiapp_prepaid_id.get(0).getInteger("id");
                        conn.update(query_status_cancel_prepaid, replyPrepaid -> {
                            try {
                                if (replyPrepaid.succeeded()){
                                    conn.queryWithParams("SELECT id FROM parcels_packages WHERE parcel_id = ?", new JsonArray().add(parcelId), replyGetPackages -> {
                                        try {
                                            if (replyGetPackages.succeeded()){
                                                List<JsonObject> packages = replyGetPackages.result().getRows();
                                                //if (!packages.isEmpty()){
                                                List<String> updatePackages = new ArrayList<>();
                                                for(int i = 0; i < packages.size(); i++) {
                                                    JsonObject pack = packages.get(i);
                                                    pack.put(PARCEL_ID, parcelId);
                                                    pack.put("package_status", packageStatus);
                                                    if (notes != null){
                                                        pack.put(NOTES, notes);
                                                    }
                                                    updatePackages.add(generateGenericUpdateString("parcels_packages", pack));
                                                }

                                                conn.update(updateParcel, replyParcel -> {
                                                    try {
                                                        if (replyParcel.succeeded()){
                                                            this.insertTracking(conn, new JsonArray().add(parcel),"parcels_packages_tracking", null, "parcel_id", null, trackingAction, updatedBy)
                                                                    .whenComplete((resultTrackingParcel, errorTrackingParcel) -> {
                                                                        try {
                                                                            if (errorTrackingParcel != null){
                                                                                future.completeExceptionally(errorTrackingParcel);
                                                                            } else {
                                                                                conn.batch(updatePackages, updatePackagesReply -> {
                                                                                    try {
                                                                                        if (updatePackagesReply.succeeded()) {
                                                                                            this.insertTracking(conn, new JsonArray(packages), "parcels_packages_tracking", "parcel_id", "parcel_package_id", null, trackingAction, updatedBy)
                                                                                                    .whenComplete((resultTracking, errorTrackingPackages) -> {
                                                                                                        try {
                                                                                                            if(errorTrackingPackages != null){
                                                                                                                future.completeExceptionally(errorTrackingPackages);
                                                                                                            } else {
                                                                                                                JsonObject result = new JsonObject()
                                                                                                                        .put("updated", true);
                                                                                                                if (ticketId != null){
                                                                                                                    result.put("cancel_ticket_id", ticketId);
                                                                                                                }
                                                                                                                future.complete(result);
                                                                                                            }
                                                                                                        } catch (Exception e){
                                                                                                            future.completeExceptionally(e);
                                                                                                        }
                                                                                                    });
                                                                                        } else {
                                                                                            future.completeExceptionally(updatePackagesReply.cause());
                                                                                        }
                                                                                    } catch (Exception e){
                                                                                        future.completeExceptionally(e);
                                                                                    }
                                                                                });
                                                                            }
                                                                        } catch (Exception e){
                                                                            future.completeExceptionally(e);
                                                                        }
                                                                    });
                                                        } else {
                                                            future.completeExceptionally(replyParcel.cause());
                                                        }
                                                    } catch (Exception e) {
                                                        future.completeExceptionally(e);
                                                    }
                                                });
                                                //} // TODO : agregar else
                                            } else {
                                                future.completeExceptionally(replyGetPackages.cause());
                                            }
                                        } catch (Exception e){
                                            future.completeExceptionally(e);
                                        }
                                    });
                                } else {
                                    future.completeExceptionally(replyPrepaid.cause());
                                }
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            }
                        });
                    }else{
                        conn.queryWithParams("SELECT id FROM parcels_packages WHERE parcel_id = ?", new JsonArray().add(parcelId), replyGetPackages -> {
                            try {
                                if (replyGetPackages.succeeded()){
                                    List<JsonObject> packages = replyGetPackages.result().getRows();
                                    //if (!packages.isEmpty()){
                                    List<String> updatePackages = new ArrayList<>();
                                    for(int i = 0; i < packages.size(); i++) {
                                        JsonObject pack = packages.get(i);
                                        pack.put(PARCEL_ID, parcelId);
                                        pack.put("package_status", packageStatus);
                                        if (notes != null){
                                            pack.put(NOTES, notes);
                                        }
                                        updatePackages.add(generateGenericUpdateString("parcels_packages", pack));
                                    }

                                    conn.update(updateParcel, replyParcel -> {
                                        try {
                                            if (replyParcel.succeeded()){
                                                this.insertTracking(conn, new JsonArray().add(parcel),"parcels_packages_tracking", null, "parcel_id", null, trackingAction, updatedBy)
                                                        .whenComplete((resultTrackingParcel, errorTrackingParcel) -> {
                                                            try {
                                                                if (errorTrackingParcel != null){
                                                                    future.completeExceptionally(errorTrackingParcel);
                                                                } else {
                                                                    conn.batch(updatePackages, updatePackagesReply -> {
                                                                        try {
                                                                            if (updatePackagesReply.succeeded()) {
                                                                                this.insertTracking(conn, new JsonArray(packages), "parcels_packages_tracking", "parcel_id", "parcel_package_id", null, trackingAction, updatedBy)
                                                                                        .whenComplete((resultTracking, errorTrackingPackages) -> {
                                                                                            try {
                                                                                                if(errorTrackingPackages != null){
                                                                                                    future.completeExceptionally(errorTrackingPackages);
                                                                                                } else {
                                                                                                    JsonObject result = new JsonObject()
                                                                                                            .put("updated", true);
                                                                                                    if (ticketId != null){
                                                                                                        result.put("cancel_ticket_id", ticketId);
                                                                                                    }
                                                                                                    future.complete(result);
                                                                                                }
                                                                                            } catch (Exception e){
                                                                                                future.completeExceptionally(e);
                                                                                            }
                                                                                        });
                                                                            } else {
                                                                                future.completeExceptionally(updatePackagesReply.cause());
                                                                            }
                                                                        } catch (Exception e){
                                                                            future.completeExceptionally(e);
                                                                        }
                                                                    });
                                                                }
                                                            } catch (Exception e){
                                                                future.completeExceptionally(e);
                                                            }
                                                        });
                                            } else {
                                                future.completeExceptionally(replyParcel.cause());
                                            }
                                        } catch (Exception e) {
                                            future.completeExceptionally(e);
                                        }
                                    });
                                    //} // TODO : agregar else
                                } else {
                                    future.completeExceptionally(replyGetPackages.cause());
                                }
                            } catch (Exception e){
                                future.completeExceptionally(e);
                            }
                        });
                    }
                } else {
                    future.completeExceptionally(replyGetPrepaid.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });

        return future;
    }

//    private CompletableFuture<Integer> registerPaybackCancelParcel(SQLConnection conn, Integer parcelId, Integer cancelBy, JsonObject res){
//        CompletableFuture<Integer> future = new CompletableFuture<>();
//        PaybackDBV objPayback = new PaybackDBV();
//        JsonObject paramMovPayback = new JsonObject()
//                .put("customer_id", res.getInteger("sender_id"))
//                .put("points", 0)
//                .put("money", res.getDouble("payback"))
//                .put("type_movement", "O")
//                .put("motive", "Cancelación de paquetería")
//                .put("id_parent", parcelId)
//                .put("employee_id", cancelBy);
//        objPayback.generateMovementPayback(conn, paramMovPayback).whenComplete((movementPayback, errorMP) ->{
//            try {
//                if(errorMP != null){
//                    future.completeExceptionally(errorMP);
//                } else {
//                    this.getMoneyReturnInPayback(res.getDouble("total_amount")).whenComplete((resultMoneyReturn, errorP) -> {
//                        try {
//                            if (errorP != null) {
//                                future.completeExceptionally(errorMP);
//                            } else {
//                                JsonObject paramMovReturnPayback = new JsonObject()
//                                        .put("customer_id", res.getInteger("sender_id"))
//                                        .put("points", 0)
//                                        .put("money", resultMoneyReturn)
//                                        .put("type_movement", "I")
//                                        .put("motive", "Reembolso de monedero(Cancelación de paquetería)")
//                                        .put("id_parent", parcelId)
//                                        .put("employee_id", cancelBy);
//                                objPayback.generateMovementPayback(conn, paramMovReturnPayback).whenComplete((movementReturnPayback, errorMRP) -> {
//                                    try {
//                                        if (errorMRP != null) {
//                                            future.completeExceptionally(errorMRP);
//                                        } else {
//                                            this.insertTicket(conn, cancelBy,"cancel", parcelId, movementReturnPayback).whenComplete((JsonObject ticket, Throwable ticketError) -> {
//                                                try {
//                                                    if (ticketError != null) {
//                                                        future.completeExceptionally(ticketError);
//                                                    } else {
//                                                        future.complete(ticket.getInteger("id"));
//                                                    }
//                                                } catch (Exception e){
//                                                    future.completeExceptionally(e);
//                                                }
//                                            });
//                                        }
//                                    } catch (Exception e){
//                                        future.completeExceptionally(e);
//                                    }
//                                });
//                            }
//                        } catch (Exception e){
//                            future.completeExceptionally(e);
//                        }
//                    });
//                }
//            } catch (Exception e){
//                future.completeExceptionally(e);
//            }
//        });
//        return future;
//    }

    protected CompletableFuture<Integer> returnMoney(SQLConnection conn, Integer cashOutId, Integer currencyId, String expenceConcept, String referenceField, Integer referenceId, Double amountToReturn, Integer createdBy, boolean is_credit, Integer customerId, double ivaPercent) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        Future f1 = Future.future();
        Future f2 = Future.future();

        conn.queryWithParams(QUERY_EXPENSE_CONCEPT_RETURN, new JsonArray().add(expenceConcept), f1.completer());

        conn.query("SELECT id FROM payment_method WHERE is_cash = 1 AND status = 1 LIMIT 1;", f2.completer());

        CompositeFuture.all(f1, f2).setHandler(r -> {
            try {
                if (r.failed()) {
                    throw new Exception(r.cause());
                }
                List<JsonObject> expenses = r.result().<ResultSet>resultAt(0).getRows();
                Integer paymentMethodId = r.result().<ResultSet>resultAt(1).getRows().get(0).getInteger("id");
                Integer expenseConceptId = null;
                if (!expenses.isEmpty()) {
                    expenseConceptId = expenses.get(0).getInteger("id");
                }
                JsonObject expense = new JsonObject()
                        .put(referenceField, referenceId)
                        .put("payment_method_id", paymentMethodId)
                        .put("amount", amountToReturn)
                        .put("reference", expenceConcept)
                        .put("currency_id", currencyId)
                        .put("created_by", createdBy)
                        .put("expense_concept_id", expenseConceptId)
                        .put("description", expenceConcept);

                JsonObject cashChange = new JsonObject().put("paid", amountToReturn).put("total", amountToReturn).put("paid_change", 0.0);

                this.insertTicket(conn, cashOutId, referenceId, amountToReturn, cashChange, createdBy, ivaPercent, "cancel")
                        .whenComplete((JsonObject ticket, Throwable ticketError) -> {
                            try {
                                if (ticketError != null) {
                                    throw new Exception(ticketError);
                                }

                                JsonObject ticketDetail = new JsonObject();
                                ticketDetail.put("ticket_id", ticket.getInteger("id"));
                                ticketDetail.put("quantity", 1);
                                ticketDetail.put("detail", "Comprobante de cancelación de paquetería");
                                ticketDetail.put("unit_price", amountToReturn);
                                ticketDetail.put("amount", amountToReturn);
                                ticketDetail.put("created_by", createdBy);

                                String insertTicketDetail = this.generateGenericCreate("tickets_details", ticketDetail);
                                conn.update(insertTicketDetail, replyInsertTicketDetail -> {
                                    try {
                                        if (replyInsertTicketDetail.failed()) {
                                            throw new Exception(replyInsertTicketDetail.cause());
                                        }

                                        if (is_credit) {
                                            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CustomerDBV.ACTION_GET_CUSTOMER_CREDIT);
                                            JsonObject paramsCredit = new JsonObject().put("customer_id", customerId);
                                            vertx.eventBus().send(CustomerDBV.class.getSimpleName(), paramsCredit, options, (AsyncResult<Message<JsonObject>> replyCredit) -> {
                                                try {
                                                    if (replyCredit.failed()) {
                                                        throw new Exception(replyCredit.cause());
                                                    }
                                                    Message<JsonObject> customerCreditDataMsg = replyCredit.result();
                                                    JsonObject customerCreditData = customerCreditDataMsg.body();
                                                    this.updateCustomerCredit(conn, customerCreditData, amountToReturn, createdBy, true, false, 0.00)
                                                            .whenComplete((replyCustomer, errorCustomer) -> {
                                                                try{
                                                                    if (errorCustomer != null) {
                                                                        throw new Exception(errorCustomer);
                                                                    }

                                                                    future.complete(ticket.getInteger("id"));

                                                                } catch (Throwable t) {
                                                                    t.printStackTrace();
                                                                    future.completeExceptionally(t);
                                                                }
                                                            });

                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    future.completeExceptionally(e);
                                                }
                                            });

                                        } else {
                                            expense.put("ticket_id", ticket.getInteger("id"));
                                            this.registerExpense(conn, expense, cashOutId, createdBy).whenComplete((JsonObject expenseReturn, Throwable expenseError) -> {
                                                try {
                                                    if (expenseError != null) {
                                                        throw new Exception(expenseError);
                                                    }


                                                    //TODO: registrar cashout move

                                                    future.complete(ticket.getInteger("id"));

                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    future.completeExceptionally(e);
                                                }
                                            });
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        future.completeExceptionally(e);
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                future.completeExceptionally(e);
                            }
                        });

            } catch (Exception e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> registerExpense(SQLConnection conn, JsonObject expense, Integer cashOutId, Integer createdBy){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String insertExpenses = generateGenericCreate("expense", expense);

        JsonObject cashOutMove = new JsonObject()
                .put("cash_out_id", cashOutId)
                .put("quantity", expense.getDouble("amount"))
                .put("move_type", "1")
                .put("created_by", createdBy);

        conn.update(insertExpenses, (AsyncResult<UpdateResult> reply) -> {
            try {
                if (reply.succeeded()) {
                    final int id = reply.result().getKeys().getInteger(0);
                    expense.put("id", id);
                    cashOutMove.put("expense_id", id);
                    String insertCashOutMove = this.generateGenericCreate("cash_out_move", cashOutMove);
                    conn.update(insertCashOutMove, (AsyncResult<UpdateResult> replyInsert) -> {
                        try {
                            if (replyInsert.succeeded()) {
                                future.complete(expense);
                            } else {
                                replyInsert.cause().printStackTrace();
                                future.completeExceptionally(replyInsert.cause());
                            }
                        } catch (Exception e){
                            e.printStackTrace();
                            future.completeExceptionally(e);
                        }
                    });

                } else {
                    reply.cause().printStackTrace();
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<Integer> paymentInsert(SQLConnection conn, JsonArray payments, JsonObject parcel, JsonArray packagesArray,
                                                     Double innerTotalAmount, Integer cashOutId, JsonObject cashChange, Integer updatedBy) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        Future f1 = Future.future();
        Future f2 = Future.future();

        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "currency_id"),
                new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f1.completer());

        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(), new JsonObject().put("fieldName", "iva"),
                new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), f2.completer());

        CompositeFuture.all(f1, f2).setHandler(r -> {
            try {
                if (r.succeeded()) {
                    JsonObject currencyMsg = r.result().<Message<JsonObject>>resultAt(0).body();
                    Integer currencyId = Integer.valueOf(currencyMsg.getString("value"));
                    JsonObject ivaMsg = r.result().<Message<JsonObject>>resultAt(0).body();
                    Double ivaPercent = Double.valueOf(ivaMsg.getString("value"));
                    //change this , its a test to the rad parcel
                    JsonObject rad = new JsonObject();

                    Double totalPayments = 0.0;
                    final int pLen = payments.size();

                    for (int i = 0; i < pLen; i++) {
                        JsonObject payment = payments.getJsonObject(i);
                        Double paymentAmount = payment.getDouble("amount");
                        if (paymentAmount == null || paymentAmount < 0.0) {
                            future.completeExceptionally(new Throwable("Invalid payment amount: " + paymentAmount));
                        }
                        totalPayments += UtilsMoney.round(paymentAmount, 2);
                    }

                    if (totalPayments > innerTotalAmount) {
                        future.completeExceptionally(new Throwable("The payment " + totalPayments + " is greater than the total " + innerTotalAmount));
                    } else if (totalPayments < innerTotalAmount) {
                        future.completeExceptionally(new Throwable("The payment " + totalPayments + " is lower than the total " + innerTotalAmount));
                    } else {
                        final Double finalIva = UtilsMoney.round(this.getIva(innerTotalAmount, ivaPercent), 2);
                        final Double finalTotalAmount = innerTotalAmount;

                        // Insert ticket
                        this.insertTicket(conn, cashOutId, parcel.getInteger("id"), finalTotalAmount, cashChange, updatedBy, ivaPercent, "purchase").whenComplete((JsonObject ticket, Throwable ticketError) -> {
                            if (ticketError != null) {
                                future.completeExceptionally(ticketError);
                            } else {
                                // Insert ticket detail
                                this.insertTicketDetail(conn, ticket.getInteger("id"), updatedBy, packagesArray, new JsonArray(), parcel, false , rad).whenComplete((Boolean detailsSuccess, Throwable dError) -> {
                                    if (dError != null) {
                                        future.completeExceptionally(dError);
                                    } else {
                                        // insert payments
                                        List<CompletableFuture<JsonObject>> pTasks = new ArrayList<>();
                                        for (int i = 0; i < pLen; i++) {
                                            JsonObject payment = payments.getJsonObject(i);
                                            payment.put("ticket_id", ticket.getInteger("id"));
                                            pTasks.add(insertPaymentAndCashOutMove(conn, payment, currencyId, parcel.getInteger("id"), cashOutId, updatedBy, false));
                                        }
                                        CompletableFuture<Void> allPayments = CompletableFuture.allOf(pTasks.toArray(new CompletableFuture[pLen]));

                                        allPayments.whenComplete((s, t) -> {
                                            try {
                                                if (t != null) {
                                                    future.completeExceptionally(t.getCause());
                                                } else {
                                                    future.complete(ticket.getInteger("id"));
                                                }
                                            } catch (Exception e){
                                                future.completeExceptionally(e);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                } else {
                    future.completeExceptionally(r.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<Double> getMoneyReturnInPayback(Double amount){
        CompletableFuture<Double> future = new CompletableFuture<>();
        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                new JsonObject().put("fieldName", "cancel_penalty_parcel"),
                new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD),
                replyC -> {
                    try {
                        if (replyC.succeeded()){
                            JsonObject result = (JsonObject) replyC.result().body();
                            Double percentPenality = Double.valueOf(result.getString("value"));
                            Double moneyPenality = (amount * percentPenality) / 100;
                            Double moneyReturnPayback = amount - moneyPenality;
                            future.complete(moneyReturnPayback);
                        } else {
                            future.completeExceptionally(replyC.cause());
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                        future.completeExceptionally(e);
                    }
                });
        return future;
    }

    private CompletableFuture<JsonObject> insertTicket(SQLConnection conn, Integer createdBy, String action, Integer parcelId, JsonObject movementPayback) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject ticket = new JsonObject();
        List<String> inserts = new ArrayList<>();

        // Create ticket_code
        ticket.put("created_by", createdBy);
        ticket.put("parcel_id", parcelId);
        ticket.put("action", action);
        ticket.put("ticket_code", UtilsID.generateID("T"));
        if (movementPayback != null){
            Double beforeMoney = movementPayback.getDouble("before_money_payback");
            ticket.put("payback_before", beforeMoney);
            ticket.put("payback_money", beforeMoney + movementPayback.getDouble("money_payback"));
        }

        String insert = this.generateGenericCreate("tickets", ticket);

        conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }
                final int id = reply.result().getKeys().getInteger(0);
                ticket.put("id", id);
                JsonObject ticketDetail = new JsonObject();
                ticketDetail.put("ticket_id", id);
                ticketDetail.put("quantity", 1);
                ticketDetail.put("detail", "Cancelación de paquetería");
                ticketDetail.put("created_by", createdBy);
                inserts.add(this.generateGenericCreate("tickets_details", ticketDetail));
                conn.batch(inserts, (AsyncResult<List<Integer>> replyInsert) -> {

                    try {
                        if (replyInsert.failed()){
                            throw new Exception(replyInsert.cause());
                        }
                        future.complete(ticket);


                    }catch(Exception e ){
                        e.printStackTrace();
                        future.completeExceptionally(replyInsert.cause());
                    }

                });

            }catch (Exception ex){
                ex.printStackTrace();
                future.completeExceptionally(reply.cause());
            }
        });

        return future;
    }

    private CompletableFuture<Boolean> insertParcelPackageTracking(SQLConnection conn,List<JsonObject> parcelPackages,JsonObject message) {

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        List<String> parcelPackageTrackings = new ArrayList<>();
        JsonObject parcelPackageTracking = new JsonObject();

        for (JsonObject parcelPackage: parcelPackages){

            parcelPackageTracking.put("parcel_id",parcelPackage.getInteger("parcel_id"));
            parcelPackageTracking.put("parcel_package_id",parcelPackage.getInteger("id"));
            parcelPackageTracking.put("action","canceled");
            parcelPackageTracking.put("notes",parcelPackage.getString("notes"));
            parcelPackageTracking.put("status",4);
            parcelPackageTracking.put("created_by",message.getInteger("CANCEL_BY"));
            parcelPackageTrackings.add(this.generateGenericCreate("parcels_packages_tracking", parcelPackageTracking));
        }

        conn.batch(parcelPackageTrackings, (AsyncResult<List<Integer>> replyInsert) -> {
            if (replyInsert.succeeded()) {
                future.complete(replyInsert.succeeded());
            } else {
                future.completeExceptionally(replyInsert.cause());
            }
        });

        return future;
    }


    private CompletableFuture<JsonObject> deliverPackage(SQLConnection conn, JsonObject parcelPackage, int updateBy, Integer parcelId, int parcelsAddresseeId, Integer empBranchofficeId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        Integer parcelPackageId= parcelPackage.getInteger("id");
        Integer packageStatusCode = parcelPackage.getInteger("package_status");

        this.checkParcelPackageBelongsParcel(conn, parcelId, parcelPackageId).whenComplete((resultCheckParcelPackageBelongsParcel, errorCheckParcelPackageBelongsParcel) ->{
            try{
                if (errorCheckParcelPackageBelongsParcel != null){
                    future.completeExceptionally(errorCheckParcelPackageBelongsParcel);
                } else {
                    this.checkEmployeeBranchParcelPackage(conn, parcelId, empBranchofficeId).whenComplete((resultCheckEmployeeBranchParcelPackage, errorCheckEmployeeBranchParcelPackage) -> {
                        try{
                            if (errorCheckEmployeeBranchParcelPackage != null){
                                future.completeExceptionally(errorCheckEmployeeBranchParcelPackage);
                            } else {
                                this.checkParcelDestiny(conn, parcelId, resultCheckEmployeeBranchParcelPackage).whenComplete((resultCheckParcelDestiny, errorCheckParcelDestiny) -> {
                                    try{
                                        if(errorCheckParcelDestiny != null){
                                            future.completeExceptionally(errorCheckParcelDestiny);
                                        } else {
                                            String updatePackage = "UPDATE parcels_packages\n" +
                                                    "SET package_status= ? , updated_by = ? , parcels_deliveries_id = ? \n"
                                                    + "WHERE id = ?;";

                                            JsonArray params = new JsonArray()
                                                    .add(packageStatusCode)
                                                    .add(updateBy)
                                                    .add(parcelsAddresseeId)
                                                    .add(parcelPackageId);

                                            conn.updateWithParams(updatePackage, params, replyUpdate -> {
                                                try {
                                                    if (replyUpdate.succeeded()) {
                                                        if (packageStatusCode.equals(PARCEL_STATUS.DELIVERED_WITH_INCIDENCES.ordinal())) {
                                                            parcelPackage.put("updateBy", updateBy);
                                                            this.insertParcelIncidence(conn, parcelPackage, parcelId).whenComplete((result, stThrow) -> {
                                                                try{
                                                                    if (stThrow != null) {
                                                                        future.completeExceptionally(stThrow);
                                                                    } else {
                                                                        result.put("parcel_package_id", parcelPackage.getInteger("id"));
                                                                        future.complete(result);
                                                                    }
                                                                } catch (Exception e){
                                                                    future.completeExceptionally(e);
                                                                }
                                                            });
                                                        } else {
                                                            future.complete(parcelPackage);
                                                        }
                                                    } else {
                                                        future.completeExceptionally(replyUpdate.cause());
                                                    }
                                                } catch (Exception e){
                                                    future.completeExceptionally(e);
                                                }
                                            });
                                        }
                                    } catch (Exception e){
                                        future.completeExceptionally(e);
                                    }
                                });
                            }
                        } catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private CompletableFuture<Boolean> checkParcelPackageBelongsParcel(SQLConnection conn, Integer parcelId, Integer parcelPackageId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonArray param = new JsonArray().add(parcelPackageId);
            conn.queryWithParams("SELECT parcel_id FROM parcels_packages WHERE id = ?;", param, reply ->{
                try{
                    if(reply.succeeded()){
                        Integer referenceParcelId = reply.result().getRows().get(0).getInteger("parcel_id");
                        if(referenceParcelId.equals(parcelId)){
                            future.complete(true);
                        } else {
                            future.completeExceptionally(new Throwable("The parcel_package_id:"+parcelPackageId+" does not match the parcel_id"));
                        }
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch(Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    private CompletableFuture<Boolean> checkParcelDestiny(SQLConnection conn, Integer parcelId, Integer referenceTerminalId){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            JsonArray param = new JsonArray().add(parcelId);
            conn.queryWithParams("SELECT id, terminal_destiny_id FROM parcels WHERE id = ?;", param, reply ->{
                try{
                    if(reply.succeeded()){
                        Integer terminalDestinyId = reply.result().getRows().get(0).getInteger("terminal_destiny_id");
                        if(terminalDestinyId.equals(referenceTerminalId)){
                            future.complete(true);
                        } else {
                            future.completeExceptionally(new Throwable("The package is not in the destination terminal"));
                        }
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch(Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<Integer> checkEmployeeBranchParcelPackage(SQLConnection conn, Integer parcelId, Integer empBranchofficeId){
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            JsonArray param = new JsonArray().add(parcelId);
            conn.queryWithParams("SELECT \n" +
                    "   ppt.id, \n" +
                    "   ppt.terminal_id, \n" +
                    "   ppt.action,\n" +
                    "   p.terminal_destiny_id,\n" +
                    "   (ppt.terminal_id IN (SELECT bprc.receiving_branchoffice_id FROM branchoffice_parcel_receiving_config bprc WHERE bprc.of_branchoffice_id = p.terminal_destiny_id)) AS in_replacement_terminal\n" +
                    "FROM parcels_packages_tracking ppt\n" +
                    "INNER JOIN parcels p ON p.id = ppt.parcel_id\n" +
                    "WHERE ppt.parcel_id = ? \n" +
                    "AND ppt.action IN('move', 'downloaded', 'arrived') \n" +
                    "ORDER BY ppt.created_at DESC \n" +
                    "LIMIT 1;", param, reply ->{
                try{
                    if(reply.succeeded()){
                        JsonObject result = reply.result().getRows().get(0);
                        Integer referenceTerminalId = result.getInteger("terminal_id");
                        String referenceAction = result.getString("action");
                        Integer terminalDestinyId = result.getInteger(_TERMINAL_DESTINY_ID);
                        boolean inReplacementTerminal = result.getInteger("in_replacement_terminal") > 0;
                        if (referenceTerminalId.intValue() != empBranchofficeId.intValue() || ((referenceTerminalId.intValue() != terminalDestinyId) && !inReplacementTerminal)){
                            future.completeExceptionally(new Throwable("Packages can not be delivered to this branch"));
                        } else {
                            if(referenceAction.equals("move") || referenceAction.equals("downloaded") || referenceAction.equals("arrived")){
                                future.complete(terminalDestinyId);
                            } else {
                                future.completeExceptionally(new Throwable("The package has not been arrived, downloaded or moved between departments"));
                            }
                        }
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch(Exception e){
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<JsonObject> insertParcelIncidence(SQLConnection conn,JsonObject parcelPackage, int parcelId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonArray incidences = parcelPackage.getJsonArray("packages_incidences");

            JsonObject parcelIncidence = new JsonObject();
            JsonObject parcelPackageTracking = new JsonObject();

            List<String> parcelPackageTrackings = new ArrayList<>();
            List<String> parcelIncidences = new ArrayList<>();

            for (int i = 0; i < incidences.size(); i++) {
                JsonObject incidence = incidences.getJsonObject(i);

                parcelIncidence.put("parcel_id", parcelId);
                parcelIncidence.put("parcel_package_id",parcelPackage.getInteger("id"));
                parcelIncidence.put("incidence_id",incidence.getInteger("incidence_id"));
                parcelIncidence.put("notes",incidence.getString("notes"));
                parcelIncidence.put("status",parcelPackage.getInteger("package_status"));
                parcelIncidence.put("created_by",parcelPackage.getInteger("updateBy"));
                parcelIncidences.add(this.generateGenericCreate("parcels_incidences", parcelIncidence));


                parcelPackageTracking.put("parcel_id", parcelId);
                parcelPackageTracking.put("parcel_package_id",parcelPackage.getInteger("id"));
                parcelPackageTracking.put("action","closed");
                parcelPackageTracking.put("notes",incidence.getString("notes"));
                parcelPackageTracking.put("status",parcelPackage.getInteger("package_status"));
                parcelPackageTracking.put("created_by",parcelPackage.getInteger("updateBy"));
                parcelPackageTrackings.add(this.generateGenericCreate("parcels_packages_tracking", parcelPackageTracking));

            }

            conn.batch(parcelIncidences, (AsyncResult<List<Integer>> replyInsert) -> {
                try {
                    if (replyInsert.succeeded()) {

                        this.insertParcelPackageTrackingDeliver(conn, parcelPackageTrackings, parcelPackage).whenComplete((result, stThrow) -> {
                            try {
                                if (stThrow != null) {
                                    future.completeExceptionally(stThrow);
                                } else {
                                    result.put("parcel_package_id", parcelPackage.getInteger("id"));
                                    future.complete(result);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                future.completeExceptionally(ex);
                            }
                        });

                    } else {
                        future.completeExceptionally(replyInsert.cause());
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<JsonObject> insertParcelPackageTrackingDeliver(SQLConnection conn, List<String> parcelPackageTrackings,JsonObject parcelPackage) {

        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            conn.batch(parcelPackageTrackings, (AsyncResult<List<Integer>> replyInsert ) -> {
                try {
                    if (replyInsert.succeeded()) {
                        future.complete(parcelPackage);
                    } else {
                        future.completeExceptionally(replyInsert.cause());
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }

    private Double getIva(Double amount, Double ivaPercent){
        Double iva = 0.00;

        iva = amount - (amount / (1 + (ivaPercent/100)));

        return iva;
    }

    private void updatePrinted(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer updatedBy = body.getInteger("updated_by");
        String trackingCode = body.getString("parcel_tracking_code");
        JsonArray params = new JsonArray().add(trackingCode);
        String QUERY = QUERY_PARCEL_BY_TRACKING_CODE;
        String prefix = trackingCode.substring(0, 2);
        Set<String> parcelPrefixes = new HashSet<>(Arrays.asList("GP", "G1", "G2", "G3", "G4"));
        Set<String> packagePrefixes = new HashSet<>(Arrays.asList("P1", "P2", "P3", "P4"));
        if (parcelPrefixes.contains(prefix)) {
            QUERY = String.format(QUERY, "a.parcel_tracking_code = ?");
        } else if (packagePrefixes.contains(prefix)) {
            QUERY = String.format(QUERY, "pp.package_code = ?");
        } else {
            QUERY = String.format(QUERY, "a.waybill = ?");
        }
        String finalQUERY = QUERY;
        this.startTransaction(message, conn -> {
            conn.queryWithParams(finalQUERY, params, reply -> {
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    JsonObject parcel = reply.result().getRows().get(0);
                    // insert tracking for printing and update was_printed to the package
                    Future<UpdateResult> f1 = Future.future();
                    Future<UpdateResult> f2 = Future.future();

                    JsonObject tracking = new JsonObject().put("parcel_id", parcel.getInteger("id"))
                            .put("action", "printed")
                            .put("created_by", updatedBy);

                    String insert = this.generateGenericCreate("parcels_packages_tracking", tracking);
                    GenericQuery delete = this.generateGenericUpdate("parcels", new JsonObject()
                            .put("id", parcel.getInteger("id"))
                            .put("updated_by", updatedBy)
                            .put("updated_at", sdfDataBase(new Date()))
                    );

                    conn.update(insert, f1.completer());
                    conn.updateWithParams(delete.getQuery(), delete.getParams(), f2.completer());

                    CompositeFuture.all(f1, f2).setHandler(replyFuture -> {
                        try {
                            if (replyFuture.failed()){
                                throw new Exception(replyFuture.cause());
                            }
                            this.commit(conn, message, parcel);


                        }catch(Exception e ){
                            e.printStackTrace();
                            this.rollback(conn, replyFuture.cause() ,message);
                        }
                    });

                }catch (Exception ex){
                    ex.printStackTrace();
                    this.rollback(conn, reply.cause() ,message);
                }
            });
        });
    }

    private void getPackages(Message<JsonObject> message) {
        JsonArray params = new JsonArray();
        JsonObject body = message.body();

        params.add(body.getInteger("parcel_id"));

        this.dbClient.queryWithParams(QUERY_PARCEL_PACKAGES, params, reply -> {
            try {
                if (reply.succeeded()) {
                    List<JsonObject> packages = reply.result().getRows();
                    List<CompletableFuture<JsonObject>> incidencesTasks = new ArrayList<>();
                    final int len = packages.size();
                    for (JsonObject parcelPackage : packages) {
                        incidencesTasks.add(insertIncidences(parcelPackage, new JsonObject()));
                    }

                    CompletableFuture<Void> allIncidences = CompletableFuture.allOf(incidencesTasks.toArray(new CompletableFuture[len]));
                    allIncidences.whenComplete((s, t) -> {
                        try {
                            if (t != null) {
                                reportQueryError(message, t.getCause());
                            } else {
                                message.reply(new JsonArray(packages));
                            }
                        } catch (Exception e) {
                            reportQueryError(message, e.getCause());
                        }
                    });

                } else {
                    reportQueryError(message, reply.cause());
                }
            } catch (Exception e) {
                reportQueryError(message, e.getCause());
            }
        });
    }

    private void  scanningPackagesReport(Message<JsonObject> message){
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String QUERY = QUERY_SCANNING_PACKAGE_REPORT;
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer originCityId = body.getInteger("origin_city_id");
            Integer originTerminalId = body.getInteger("origin_terminal_id");
            String shippingType = body.getString("shipping_type");
            if(shippingType != null){
                QUERY = QUERY.replace("{trackingJoin}",QUERY_JOIN_SHIPMENT_TRACKING);
                QUERY = QUERY.replace("{filterShipping}", " INNER JOIN parcels_packages AS pp ON pp.parcel_id = pa.id AND pp.shipping_type = ? ");
                //params.add(shippingType);
            }else{
                QUERY = QUERY.replace("{trackingJoin}", "SELECT * FROM shipments_parcel_package_tracking GROUP BY shipment_id, parcel_package_id");
                QUERY = QUERY.replace("{filterShipping}", "");
            }
            if(shippingType != null){
                params.add(initDate).add(endDate).add(shippingType).add(originTerminalId).add(originTerminalId).add(shippingType).add(originTerminalId).add(originTerminalId).add(shippingType).add(initDate).add(endDate).add(originTerminalId);
            }else{
                params.add(initDate).add(endDate).add(originTerminalId).add(originTerminalId).add(originTerminalId).add(originTerminalId).add(initDate).add(endDate).add(originTerminalId);
            }
            if(originCityId != null){
                QUERY += " AND ct.id = ? ";
                params.add(originCityId);
            }
            QUERY += " GROUP BY sr.config_route_id, sr.config_schedule_id;";
            this.dbClient.queryWithParams(QUERY, params , reply ->{
                try{
                    if(reply.failed()){
                        throw  new Exception(reply.cause());
                    }
                    List<JsonObject> resultScanningPackages = reply.result().getRows();
                    JsonObject resp = new JsonObject();

                    if(resultScanningPackages.isEmpty()){
                        resp.put("results_scanning", new JsonArray());
                        message.reply(resp);
                    }else{
                        List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                        resultScanningPackages.forEach(r -> {
                            tasks.add( this.getScanningDateRoute(r, originTerminalId));
                        });
                        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((s, t)  ->{
                           try{
                               if (t != null) {
                                   throw new Exception(t);
                               }
                               resp.put("results_scanning", new JsonArray(resultScanningPackages));
                               message.reply(resp);
                            } catch (Exception e){
                                reportQueryError(message, e);
                            }
                        });
                    }
                }catch (Exception e){
                    reportQueryError(message, e.getCause());
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }

    private CompletableFuture<JsonObject> getScanningDateRoute(JsonObject scanningObject, Integer terminalOrigin){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.dbClient.queryWithParams(QUERY_SCANNING_PACKAGE_TRAVEL_DATE, new JsonArray().add(scanningObject.getInteger("schedule_route_id")).add(terminalOrigin), reply -> {
            try {
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                List<JsonObject> result = reply.result().getRows();
                scanningObject.put("travel_date", result.isEmpty() ? null: result.get(0).getString("travel_date"));
                this.dbClient.queryWithParams(QUERY_SCANNING_PACKAGE_ARRIVAL_DATE, new JsonArray().add(scanningObject.getInteger("schedule_route_id")).add(terminalOrigin), replyArrival->{
                    try{
                        List<JsonObject> resultArrival = replyArrival.result().getRows();
                        scanningObject.put("arrival_date", resultArrival.isEmpty() ? null: resultArrival.get(0).getString("arrival_date"));
                        future.complete(scanningObject);
                    }catch (Exception e){
                        e.printStackTrace();
                        future.completeExceptionally(e);
                    }
                });
            } catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void getPackagesSummary(Message<JsonObject> message) {
        JsonArray params = new JsonArray();
        JsonObject body = message.body();

        params.add(body.getInteger("parcel_id"));

        this.dbClient.queryWithParams(QUERY_PARCEL_PACKAGES_SUMMARY, params, reply -> {
            try{
                if(reply.failed()){
                    throw new Exception(reply.cause());
                }

                List<JsonObject> packages = reply.result().getRows();
                message.reply(new JsonArray(packages));
            }catch (Exception ex){
                ex.printStackTrace();
                reportQueryError(message, reply.cause());
            }
        });

    }
    private void unregisteredPackageReport(Message<JsonObject> message){
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String QUERY = QUERY_UNREGISTERED_PACKAGE_REPORT;
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer originCityId = body.getInteger("origin_city_id");
            Integer originTerminalId = body.getInteger("origin_terminal_id");
            Integer destinyCityId = body.getInteger("destiny_city_id");
            Integer destinyTerminalId = body.getInteger("destiny_terminal_id");
            String shippingType = body.getString("shipping_type");
            params.add(initDate).add(endDate);
            if(originCityId != null){
                QUERY += " AND ob.city_id = ?";
                params.add(originCityId);
            }
            if(originTerminalId != null){
                QUERY += " AND p.terminal_origin_id = ?";
                params.add(originTerminalId);
            }
            if(destinyTerminalId != null){
                QUERY += " AND p.terminal_destiny_id = ?";
                params.add(destinyTerminalId);
            }
            if(destinyCityId != null){
                QUERY += " AND db.city_id = ?";
                params.add(destinyCityId);
            }
            if(shippingType != null){
                QUERY += " AND pp.shipping_type = ?";
                params.add(shippingType);
            }

            QUERY +=" group by pp.package_type_id,pp.package_price_id,package_status";

            this.dbClient.queryWithParams(QUERY, params , reply ->{
                try{
                    if(reply.failed()){
                        throw  new Exception(reply.cause());
                    }
                    List<JsonObject> resultUnregisteredPackages = reply.result().getRows();
                    if (resultUnregisteredPackages.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(resultUnregisteredPackages));
                    }
                }catch (Exception e){
                    reportQueryError(message, e.getCause());
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }

    }

    private void stockReport (Message<JsonObject> message) {
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String QUERY = QUERY_STOCK_REPORT;

            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");

            params.add(initDate).add(endDate);

            if(body.getInteger("terminal_city_id") != null){
                QUERY += " AND b.city_id = ?";
                params.add(body.getInteger("terminal_city_id"));
            }
            if(body.getInteger("terminal_id") != null){
                QUERY += " AND t.terminal_id = ?";
                params.add(body.getInteger("terminal_id"));
            }
            if(body.getInteger("terminal_origin_city_id") != null){
                QUERY += " AND ob.city_id = ?";
                params.add(body.getInteger("terminal_origin_city_id"));
            }
            if(body.getInteger("terminal_origin_id") != null){
                QUERY += " AND p.terminal_origin_id = ?";
                params.add(body.getInteger("terminal_origin_id"));
            }
            if(body.getInteger("terminal_destiny_city_id") != null){
                QUERY += " AND db.city_id = ?";
                params.add(body.getInteger("terminal_destiny_city_id"));
            }
            if(body.getInteger("terminal_destiny_id") != null){
                QUERY += " AND p.terminal_destiny_id = ?";
                params.add(body.getInteger("terminal_destiny_id"));
            }
            if(body.getString("shipping_type") != null){
                QUERY += " AND pp.shipping_type = ?";
                params.add(body.getString("shipping_type"));
            }
            if(body.getString("shipping_parcel") != null){
                QUERY += " AND p.shipment_type = ?";
                params.add(body.getString("shipping_parcel"));
            }


            QUERY = QUERY.concat(REPORT_ORDER_BY);

            this.dbClient.queryWithParams(QUERY, params, reply ->{
                try {
                    if (reply.succeeded()) {
                        List<JsonObject> resultParcel = reply.result().getRows();
                        if (resultParcel.isEmpty()) {
                            message.reply(new JsonArray());
                        } else {
                            message.reply(new JsonArray(resultParcel));
                        }
                    } else {
                        reportQueryError(message, reply.cause());
                    }
                } catch (Exception e){
                    reportQueryError(message, e.getCause());
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }

    private void transitPackageReport(Message<JsonObject> message){
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String QUERY = QUERY_TRANSIT_PACKAGE_REPORT;
            String QUERY_TOTAL = QUERY_TOTAL_TRANSIT_PACKAGE_REPORT;
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer originCityId = body.getInteger("origin_city_id");
            Integer originTerminalId = body.getInteger("origin_terminal_id");
            Integer destinyCityId = body.getInteger("destiny_city_id");
            Integer destinyTerminalId = body.getInteger("destiny_terminal_id");
            String shippingType = body.getString("shipping_type");
            params.add(initDate).add(endDate);
            if(originCityId != null){
                QUERY += " AND ob.city_id = ?";
                QUERY_TOTAL += " AND ob.city_id = ?";
                params.add(originCityId);
            }
            if(originTerminalId != null){
                QUERY += " AND p.terminal_origin_id = ?";
                QUERY_TOTAL += " AND p.terminal_origin_id = ?";
                params.add(originTerminalId);
            }
            if(destinyTerminalId != null){
                QUERY += " AND p.terminal_destiny_id = ?";
                QUERY_TOTAL += " AND p.terminal_destiny_id = ?";
                params.add(destinyTerminalId);
            }
            if(destinyCityId != null){
                QUERY += " AND db.city_id = ?";
                QUERY_TOTAL += " AND db.city_id = ?";
                params.add(destinyCityId);
            }
            if(shippingType != null){
                QUERY += " AND pp.shipping_type = ?";
                QUERY_TOTAL += " AND pp.shipping_type = ?";
                params.add(shippingType);
            }

            QUERY +=" GROUP BY p.id,pp.package_type_id,pp.contains ";
            JsonArray paramsCount = params.copy();
            String queryFields = QUERY_TRANSIT_PACKAGE_REPORT_FIELDS.concat(QUERY);
            String queryCount = QUERY_TRANSIT_PACKAGE_REPORT_COUNT.concat(QUERY).concat(" ) AS f");
           if(body.getInteger("page") != null){
               Integer page = body.getInteger("page",1);
               Integer limit = body.getInteger("limit", MAX_LIMIT);
               if (limit > MAX_LIMIT) {
                   limit = MAX_LIMIT;
               }
               int skip = limit * (page-1);
               queryFields = queryFields.concat(" LIMIT ?,? ");
               params.add(skip).add(limit);
           }
            String finalQueryFields = queryFields;
            String QUERY_TOTALS = QUERY_TOTAL;
            this.dbClient.queryWithParams(queryCount, paramsCount, replyCount ->{
               try{
                    if(replyCount.failed()){
                        throw new Exception(replyCount.cause());
                    }
                   Integer count = replyCount.result().getRows().get(0).getInteger("items", 0);
                   this.dbClient.queryWithParams(finalQueryFields, params , reply ->{
                       try{
                           if(reply.failed()){
                               throw  new Exception(reply.cause());
                           }
                           List<JsonObject> resultParcelContingency = reply.result().getRows();
                           JsonObject totalResult = new JsonObject();
                            if(body.getInteger("page")!=null){
                                totalResult
                                        .put("count", count)
                                        .put("items", resultParcelContingency.size())
                                        .put("results", resultParcelContingency);
                                message.reply(totalResult);
                            }else{
                                this.dbClient.queryWithParams(QUERY_TOTALS.concat(" GROUP BY p.terminal_destiny_id;"), params, repply ->{
                                    try{
                                        if(repply.failed()){
                                            throw  new Exception(repply.cause());
                                        }
                                        List<JsonObject> resultTotals = repply.result().getRows();

                                        JsonObject result = new JsonObject()
                                                .put("result", resultParcelContingency)
                                                .put("totals", resultTotals);
                                        message.reply(result);
                                    }catch (Exception e ){
                                        reportQueryError(message, e.getCause());
                                    }
                                });
                            }
                       }catch (Exception e){
                           reportQueryError(message, e.getCause());
                       }
                   });
               }catch (Exception e){
                   e.printStackTrace();
                   reportQueryError(message, e.getCause());
               }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }

    }
    private void transitTotalsPackageReport(Message<JsonObject> message){
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String QUERY_TOTAL = QUERY_TOTAL_TRANSIT_PACKAGE_REPORT;
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer originCityId = body.getInteger("origin_city_id");
            Integer originTerminalId = body.getInteger("origin_terminal_id");
            Integer destinyCityId = body.getInteger("destiny_city_id");
            Integer destinyTerminalId = body.getInteger("destiny_terminal_id");
            String shippingType = body.getString("shipping_type");
            params.add(initDate).add(endDate);
            if(originCityId != null){
                QUERY_TOTAL += " AND ob.city_id = ?";
                params.add(originCityId);
            }
            if(originTerminalId != null){
                QUERY_TOTAL += " AND p.terminal_origin_id = ?";
                params.add(originTerminalId);
            }
            if(destinyTerminalId != null){
                QUERY_TOTAL += " AND p.terminal_destiny_id = ?";
                params.add(destinyTerminalId);
            }
            if(destinyCityId != null){
                QUERY_TOTAL += " AND db.city_id = ?";
                params.add(destinyCityId);
            }
            if(shippingType != null){
                QUERY_TOTAL += " AND pp.shipping_type = ?";
                params.add(shippingType);
            }

            String QUERY_TOTALS = QUERY_TOTAL;
            this.dbClient.queryWithParams(QUERY_TOTALS.concat(" GROUP BY p.terminal_destiny_id;"), params, repply ->{
                try{
                    if(repply.failed()){
                        throw  new Exception(repply.cause());
                    }
                    List<JsonObject> resultTotals = repply.result().getRows();

                    JsonObject result = new JsonObject()
                            .put("totals", resultTotals);
                    message.reply(result);
                }catch (Exception e ){
                    reportQueryError(message, e.getCause());
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }
    private void cancelParcelReport(Message<JsonObject> message){
        try{
            JsonObject body = message.body();
            boolean flagTotals = body.getBoolean("flag_totals");
            JsonArray params = new JsonArray();
            String QUERY = flagTotals ? PARCEL_CANCEL_REPORT_ORDER_TOTALS : PARCEL_CANCEL_REPORT_ORDER;
            String shipmentType = body.getString("shipment_type");
            Integer terminalCityId = body.getInteger("terminal_city_id");
            Integer terminalId = body.getInteger("terminal_id");
            Integer terminalOriginCityId = body.getInteger("terminal_origin_city_id");
            Integer terminalOriginId = body.getInteger("terminal_origin_id");
            Integer terminalDestinyCityId = body.getInteger("terminal_destiny_city_id");
            Integer terminalDestinyId = body.getInteger("terminal_destiny_id");
            String cancelType = body.getString("cancel_type");
            Integer reasonId = body.getInteger("reason_id");
            String responsable = body.getString("responsable");
            Boolean isCanceledDate =  body.containsKey("is_canceled_date") ? body.getBoolean("is_canceled_date") : true;
            String compareDate = " p.canceled_at BETWEEN ? AND ? ";
            String shippingType = body.getString("shipping_type");
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");

            params.add(initDate).add(endDate);

            if(!isCanceledDate){
                compareDate = " p.created_at BETWEEN ? AND ? ";
            }
            QUERY = QUERY.concat(compareDate);

            if(cancelType != null){
                QUERY += " AND pcr.cancel_type = ?";
                params.add(cancelType);
            }
            if(responsable != null){
                QUERY += " AND pcr.responsable = ?";
                params.add(responsable);
            }
            if(reasonId != null){
                QUERY += " AND p.parcels_cancel_reason_id = ?";
                params.add(reasonId);
            }
            if(terminalCityId != null){
                QUERY += " AND b.city_id = ?";
                params.add(terminalCityId);
            }
            if(terminalId != null){
                QUERY += " AND p.branchoffice_id = ?";
                params.add(terminalId);
            }
            if(terminalOriginCityId != null){
                QUERY += " AND ob.city_id = ?";
                params.add(terminalOriginCityId);
            }
            if(terminalOriginId != null){
                QUERY += " AND p.terminal_origin_id = ?";
                params.add(terminalOriginId);
            }
            if(terminalDestinyCityId != null){
                QUERY += " AND db.city_id = ?";
                params.add(terminalDestinyCityId);
            }
            if(terminalDestinyId != null){
                QUERY += " AND p.terminal_destiny_id = ?";
                params.add(terminalDestinyId);
            }
            if(shipmentType != null){
                QUERY += " AND p.shipment_type = ?";
                params.add(shipmentType);
            }
            if(shippingType != null){
                QUERY += " AND pp.shipping_type = ?";
                params.add(shippingType);
            }
            QUERY = QUERY.concat(QUERY_CANCEL_PARCEL_REPORT);

            if (!flagTotals){
                Integer limit = body.getInteger(LIMIT);
                Integer page = body.getInteger(PAGE);
                List<Future> taskList = new ArrayList<>();
                if(page != null){
                    Future f1 = Future.future();
                    String QUERY_COUNT = "SELECT COUNT(*) AS count FROM ("+QUERY+") AS parcels_cancel_report";
                    taskList.add(f1);
                    this.dbClient.queryWithParams(QUERY_COUNT, params.copy(), f1.completer());
                    QUERY += " LIMIT ? OFFSET ? ";
                    params.add(limit).add((page - 1) * limit);
                }

                Future f2 = Future.future();
                this.dbClient.queryWithParams(QUERY, params, f2.completer());
                taskList.add(f2);
                CompositeFuture.all(taskList).setHandler(reply -> {
                    try {
                        if (reply.failed()){
                            throw new Exception(reply.cause());
                        }

                        JsonObject result = new JsonObject();
                        Integer index = taskList.size() == 1 ? 0 : 1;

                        List<JsonObject> parcelsList = reply.result().<ResultSet>resultAt(index).getRows();

                        if (parcelsList.isEmpty()) {
                            if(page != null) {
                                result.put("count", 0)
                                        .put("items", parcelsList.size())
                                        .put(RESULTS, parcelsList);
                                message.reply(result);
                            }else{
                                message.reply(new JsonArray(parcelsList));
                            }
                        } else {
                            Integer count = 0;
                            if(page != null){
                                count = reply.result().<ResultSet>resultAt(0).getRows().get(0).getInteger("count");
                            }

                            List<CompletableFuture<JsonObject>> parcelTask = new ArrayList<>();

                            parcelsList.forEach(parcel -> {
                                parcel.put("shipping_type" , shippingType);
                                parcelTask.add(getSalesPackages(parcel));
                            });

                            Integer finalCount = count;
                            CompletableFuture.allOf(parcelTask.toArray(new CompletableFuture[parcelTask.size()])).whenComplete((ps, pt) -> {
                                try {
                                    if (pt != null) {
                                        throw new Exception(pt);
                                    } else {
                                        if(page != null){
                                            result.put("count", finalCount)
                                                    .put("items", parcelsList.size())
                                                    .put(RESULTS, parcelsList);

                                            message.reply(result);
                                        }else{
                                            message.reply(new JsonArray(parcelsList));
                                        }
                                    }
                                } catch (Exception e){
                                    e.printStackTrace();
                                    reportQueryError(message, e);
                                }
                            });

                        }

                    } catch (Exception e){
                        e.printStackTrace();
                        reportQueryError(message, e);
                    }
                });
            } else {
                this.dbClient.queryWithParams(QUERY, params, reply -> {
                    try {
                        if(reply.failed()){
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

        }catch (Exception e){
            e.printStackTrace();
            reportQueryError(message, e.getCause());
        }
    }
    private void salesReport (Message<JsonObject> message) {
        try{
            JsonObject body = message.body();
            boolean flagTotals = body.getBoolean("flag_totals");
            boolean flagTranshipment = body.getBoolean("flag_transhipment");
            boolean flagWebService = body.getBoolean("flag_web_service");
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
            Boolean showGPPprice = body.getBoolean("show_gpp_price");
            Boolean showPrepayPrice = body.getBoolean("show_prepay_price");
            String userProfileName = body.getString("user_profile_name");
            JsonObject complementQueryData = new JsonObject()
                    .put("sellers", sellers)
                    .put("branchoffice_id", branchofficeId)
                    .put("customer_parcel_type", customerParcelType);

            if(userProfileName != null && userProfileName.equals("Externo document") && sellerName != null) {
                // es query del POS, por documentador externo, buscar traer todas las ventas del cliente que le corresponde
                complementQueryData.put("is_external_document", true);
                complementQueryData.put("external_document_user_id", sellerName);
            }

            this.getCustomersByCriteriaForParcelReport(complementQueryData).whenComplete((resultCustomersIds, errorCustomersIds) -> {
                if (errorCustomersIds != null) {
                    reportQueryError(message, errorCustomersIds.getCause());
                } else {
                    String QUERY = flagTranshipment ? QUERY_SALES_REPORT_TRANSHIPMENTS : QUERY_SALES_REPORT_V2;
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
                        QUERY += " AND ob.city_id = ?";
                        params.add(terminalOriginCityId);
                    }
                    if(terminalOriginId != null){
                        QUERY += " AND p.terminal_origin_id = ?";
                        params.add(terminalOriginId);
                    }
                    if(terminalDestinyCityId != null){
                        QUERY += " AND db.city_id = ?";
                        params.add(terminalDestinyCityId);
                    }
                    if(terminalDestinyId != null){
                        if (flagTranshipment) {
                            QUERY += " AND pp.id IN (SELECT \n" +
                                    "       IF(ppt2.action = 'ready_to_transhipment', ppt2.parcel_package_id, 0) AS parcel_package_id \n" +
                                    "   FROM parcels_packages_tracking ppt2\n" +
                                    "   INNER JOIN parcels_transhipments ptr2 ON ptr2.parcel_package_id = ppt2.parcel_package_id\n" +
                                    "   WHERE ppt2.terminal_id = ?\n" +
                                    "   GROUP BY parcel_package_id\n" +
                                    "   ORDER BY ppt2.id DESC)";
                        } else {
                            QUERY += " AND p.terminal_destiny_id = ?";
                        }
                        params.add(terminalDestinyId);
                    }
                    if(shippingType != null){
                        QUERY += " AND pp.shipping_type = ?";
                        params.add(shippingType);
                    }
                    if(shippingParcelType != null){
                        QUERY += " AND p.shipment_type = ?";
                        params.add(shippingParcelType);
                    }
                    if(showGPPprice != null){
                        String gppPriceCalculation;
                        if (flagTotals) {
                            // solo sumamos el costo unitario de la guia
                            gppPriceCalculation = " ROUND(((p_gpp.total_amount / p_gpp.total_count_guipp) * p.total_packages), 2)";
                        } else {
                            // sumar costo unitario + extras de la guia ya canjeada
                            gppPriceCalculation = " ROUND(((p_gpp.total_amount / p_gpp.total_count_guipp) * p.total_packages) + p.total_amount, 2)";
                        }

                        QUERY = QUERY.replace("{GPP_PRICE_COL}", gppPriceCalculation + " AS gpp_price")
                                .replace("{GPP_PRICE_JOINS}", " LEFT JOIN parcels_prepaid_detail ppd ON ppd.parcel_id = p.id\n " +
                                        "LEFT JOIN parcels_prepaid p_gpp ON p_gpp.id = ppd.parcel_prepaid_id");
                    } else {
                        QUERY = QUERY.replace("{GPP_PRICE_COL}", "NULL AS gpp_price")
                                .replace("{GPP_PRICE_JOINS}", "");
                    }

                    // OBTENER PAQUETES POR USUARIO QUE REGISTRO LA VENTA DEL PAQUETE
                    if(sellerName != null && complementQueryData.getInteger("external_document_user_id") == null){
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

                    if (resultCustomersIds.getBoolean("has_criteria") || customers != null) {
                        String[] customerIds = resultCustomersIds.getString("customers_ids").split(",");
                        StringBuilder placeholders = new StringBuilder();
                        List<Integer> dynamicParams = new ArrayList<>();

                        for (String customerId : customerIds) {
                            customerId = customerId.trim();
                            if (!customerId.isEmpty()) {
                                dynamicParams.add(Integer.parseInt(customerId));
                                placeholders.append("?,");
                            }
                        }
                        if (placeholders.length() > 0) {
                            placeholders.setLength(placeholders.length() - 1);
                        } else {
                            placeholders.append("?");
                            dynamicParams.add(-1);
                        }

                        // (customer_id, sender_id y/o addressee_id)
                        // Default a customer_id, si no esta presente
                        JsonArray customerTypes = body.getJsonArray("customer_type", new JsonArray().add("customer_id"));

                        List<String> filters = new ArrayList<>();
                        List<Integer> repeatedParams = new ArrayList<>();

                        if (resultCustomersIds.getBoolean("has_criteria") && complementQueryData.getBoolean("is_external_document")) {
                            QUERY = QUERY.concat(" AND (p.customer_id IN (" + placeholders + ") OR p.created_by = ?)");
                            repeatedParams.addAll(dynamicParams);
                            for (Integer param : repeatedParams) {
                                params.add(param);
                            }
                            params.add(sellerName);
                        } else {
                            for (int i = 0; i < customerTypes.size(); i++) {
                                String type = customerTypes.getString(i);
                                if (type.equals("customer_id")) {
                                    filters.add("p.customer_id IN (" + placeholders + ")");
                                    repeatedParams.addAll(dynamicParams);
                                }
                                if (type.equals("sender_id")) {
                                    filters.add("p.sender_id IN (" + placeholders + ")");
                                    repeatedParams.addAll(dynamicParams);
                                }
                                if (type.equals("addressee_id")) {
                                    filters.add("p.addressee_id IN (" + placeholders + ")");
                                    repeatedParams.addAll(dynamicParams);
                                }
                            }

                            if (!filters.isEmpty()) {
                                QUERY = QUERY.concat(" AND (" + String.join(" OR ", filters) + ")");
                                JsonArray repeatedParamsJson = new JsonArray();
                                repeatedParams.forEach(repeatedParamsJson::add);
                                params.addAll(repeatedParamsJson);
                            }
                        }

                        if (resultCustomersIds.getBoolean("has_criteria") &&
                                complementQueryData.getInteger("branchoffice_id") != null &&
                                includeGeneralPublic != null) {
                            QUERY = QUERY.concat(" AND (p.customer_id IN (" + placeholders +") OR p.terminal_origin_id = ?)");
                            params.add(complementQueryData.getInteger("branchoffice_id"));
                        }
                    }

                    if(!resultCustomersIds.getBoolean("has_criteria") && includeGeneralPublic != null) {
                        QUERY += " AND cc.parcel_type IS NULL";
                    }

                    if(flagWebService) {
                        // WEB SERVICE SALES REPORT
                        QUERY += "AND p.purchase_origin = 'web service'";
                        QUERY = QUERY.replace("{WS_JOINS}", "LEFT JOIN integration_partner_session AS ips ON ips.id = p.integration_partner_session_id\n" +
                                "LEFT JOIN integration_partner AS ip ON ip.id = ips.integration_partner_id\n");
                        QUERY = QUERY.replace("{PARTNER_INTEGRATION_COL_DEF}","ip.name AS partner_integration_name");

                        // mostrar el precio de la venta, incluyendo aquellas que fueron pagos por adelantado
                        if(showPrepayPrice == null || !showPrepayPrice){
                            QUERY = QUERY.replace("{TOTAL_AMOUNT_DEF}", "CASE \n" +
                                    "        WHEN ip.payment_type = 'prepay' THEN 0\n" +
                                    "        ELSE p.total_amount\n" +
                                    "    END AS total_amount");
                        } else {
                            QUERY = QUERY.replace("{TOTAL_AMOUNT_DEF}", "p.total_amount");
                        }
                    } else {
                        QUERY = QUERY.replace("{TOTAL_AMOUNT_DEF}", "p.total_amount")
                                .replace("{WS_JOINS}","")
                                .replace("{PARTNER_INTEGRATION_COL_DEF}","NULL AS partner_integration_name");
                    }

                    if (!flagTotals){
                        QUERY = QUERY.concat(" GROUP BY p.parcel_tracking_code ").concat(SALES_REPORT_ORDER_BY);
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
                        QUERY = QUERY.concat(showGPPprice != null ? QUERY_EXCLUDE_ZERO_AMOUNT_WITH_GPP_COST : QUERY_EXCLUDE_ZERO_AMOUNT);
                        QUERY = QUERY.concat(" GROUP BY p.parcel_tracking_code ").concat(SALES_REPORT_ORDER_BY);
                        QUERY = QUERY_SALES_REPORT_TOTALS + QUERY + ") AS t;";

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

    private CompletableFuture<JsonObject> getCustomersByCriteriaForParcelReport(JsonObject data){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        Boolean hasSellers = data.getJsonArray("sellers") != null;
        Boolean hasBranchoffice = data.getInteger("branchoffice_id") != null;
        Boolean hasCustomerParcelType = data.getString("customer_parcel_type") != null;
        Boolean isExternalDocumentUser = data.getBoolean("is_external_document", false);
        String QUERY = QUERY_GET_CUSTOMERS_BY_USER_SELLER_ID;

        if(!hasSellers && !hasBranchoffice && !hasCustomerParcelType && !isExternalDocumentUser) {
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

            if(isExternalDocumentUser) {
                QUERY += " AND id IN (SELECT DISTINCT(p.customer_id) FROM parcels p where p.parcel_status != 4 and p.pays_sender = 1 and p.created_by = ?)";
                params.add(data.getInteger("external_document_user_id"));
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

    private void contingencyReport(Message<JsonObject> message){
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String QUERY = QUERY_CONTINGENCY_REPORT;
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer terminal_id = body.getInteger("terminal_id");
            String shipping_type = body.getString("shipping_type");
            Integer terminalCityId = body.getInteger("terminal_city_id");
            params.add(initDate).add(endDate);
                if( terminal_id != null){
                    QUERY += " AND ppt.terminal_id = ?";
                    params.add(terminal_id);
                }
                if(shipping_type  != null){
                    QUERY += " AND pp.shipping_type = ?";
                    params.add(shipping_type);
                }
                if(terminalCityId != null){
                    QUERY += " AND b.city_id = ?";
                    params.add(terminalCityId);
                }
                QUERY += " GROUP BY ppt.id";
            JsonArray paramsCount = params.copy();
            String queryFields = QUERY_CONTINGENCY_REPORT_FIELDS.concat(QUERY);
            String queryCount = QUERY_CONTINGENCY_REPORT_COUNT.concat(QUERY).concat(" ) AS f");
            if(body.getInteger("page")!=null){
                Integer page = body.getInteger("page", 1);
                Integer limit = body.getInteger("limit", MAX_LIMIT);
                if (limit > MAX_LIMIT) {
                    limit = MAX_LIMIT;
                }
                int skip = limit * (page-1);
                queryFields = queryFields.concat(" LIMIT ?,? ");
                params.add(skip).add(limit);
            }
            String finalQueryFields = queryFields;
            this.dbClient.queryWithParams(queryCount, paramsCount, replyCount ->{
               try{
                   if(replyCount.failed()){
                       throw  new Exception(replyCount.cause());
                   }
                   Integer count = replyCount.result().getRows().get(0).getInteger("items", 0);
                   this.dbClient.queryWithParams(finalQueryFields, params , reply ->{
                       try{
                           if(reply.failed()){
                               throw  new Exception(reply.cause());
                           }
                           List<JsonObject> resultParcelContingency = reply.result().getRows();
                           if(body.getInteger("page") != null){
                               JsonObject totalResult = new JsonObject();
                               totalResult
                                       .put("count", count)
                                       .put("items", resultParcelContingency.size())
                                       .put("results", resultParcelContingency);
                               message.reply(totalResult);
                           }else{
                               if (resultParcelContingency.isEmpty()) {
                                   message.reply(new JsonArray());
                               } else {
                                   message.reply(new JsonArray(resultParcelContingency));
                               }
                           }
                       }catch (Exception e){
                           reportQueryError(message, e.getCause());
                       }
                   });
               } catch (Exception e){
                   e.printStackTrace();
                   reportQueryError(message, e.getCause());
               }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }

    //WEBDEV
    //WEBDEV
    private void salesMonth (Message<JsonObject> message) {
        try{
            JsonObject body = message.body();
            JsonArray params = new JsonArray();
            String rangoXfechaPago =  body.getString("RPAGO").toString();
            String QUERY = QUERY_SALES_REPORT_MONTH;


            if(rangoXfechaPago.equals("true")) //movdev
                QUERY=QUERY_SALES_REPORT_FECHA_PAGO;



            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            params.add(initDate).add(endDate);

            if(body.getInteger("terminal_city_id") != null){
                QUERY += " AND b.city_id = ?";
                params.add(body.getInteger("terminal_city_id"));
            }
            if(body.getInteger("terminal_id") != null){
                QUERY += " AND p.branchoffice_id = ?";
                params.add(body.getInteger("terminal_id"));
            }
            if(body.getInteger("terminal_origin_city_id") != null){
                QUERY += " AND ob.city_id = ?";
                params.add(body.getInteger("terminal_origin_city_id"));
            }
            if(body.getInteger("terminal_origin_id") != null){
                QUERY += " AND p.terminal_origin_id = ?";
                params.add(body.getInteger("terminal_origin_id"));
            }
            if(body.getInteger("terminal_destiny_city_id") != null){
                QUERY += " AND db.city_id = ?";
                params.add(body.getInteger("terminal_destiny_city_id"));
            }
            if(body.getInteger("terminal_destiny_id") != null){
                QUERY += " AND p.terminal_destiny_id = ?";
                params.add(body.getInteger("terminal_destiny_id"));
            }
            //Webdev
            if(body.getInteger("customer_id") != null){
                QUERY += " AND p.customer_id = ?";
                params.add(body.getInteger("customer_id"));
            }
            if(body.getInteger("rangoDesde") != null & body.getInteger("rangoHasta") != null){
                QUERY += " AND p.customer_id BETWEEN ? AND ?";
                params.add(body.getInteger("rangoDesde")).add(body.getInteger("rangoHasta"));
            }
            //WebDev
            if(body.getInteger("status_parcel") != null){
                QUERY += " AND p.parcel_status = ?";
                params.add(body.getInteger("status_parcel"));
            }
            //Webdev
            //if(body.getString("shipping_type") != null){
            //    QUERY += " AND pp.shipping_type = ?";
            //    params.add(body.getString("shipping_type"));
            //}


            if(rangoXfechaPago.equals("true")) //movdev
                QUERY = QUERY.concat(SALES_REPORT_ORDER_BYxFP);
            else
                QUERY = QUERY.concat(SALES_REPORT_ORDER_BY);

            this.dbClient.queryWithParams(QUERY, params, reply ->{
                try {


                    if (reply.succeeded()) {

                        List<JsonObject> resultParcel = reply.result().getRows();

                        if (resultParcel.isEmpty()) {
                            message.reply(new JsonArray());
                        } else {
                            List<CompletableFuture<JsonObject>> parcelTask = new ArrayList<>();

                            resultParcel.forEach(parcel -> {
                                parcelTask.add(getSalesPackagesMonth(parcel));
                                parcelTask.add(getPaymentsInfo(parcel));
                            });


                            CompletableFuture.allOf(parcelTask.toArray(new CompletableFuture[parcelTask.size()])).whenComplete((ps, pt) -> {
                                try {
                                    if (pt != null) {
                                        reportQueryError(message, pt.getCause());
                                    } else {


                                        message.reply(new JsonArray(resultParcel));
                                    }
                                } catch (Exception e){
                                    reportQueryError(message, e.getCause());
                                }
                            });
                        }
                    } else {
                        reportQueryError(message, reply.cause());
                    }
                } catch (Exception e){
                    reportQueryError(message, e.getCause());
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }

    private CompletableFuture<JsonObject> getSalesPackagesMonth(JsonObject parcel){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(parcel.getInteger("id"));
        this.dbClient.queryWithParams(QUERY_SALES_REPORT_PACKAGES_MONTH, params, handler->{
            try {
                if (handler.succeeded()) {
                    List<JsonObject> resultsTracking = handler.result().getRows();
                    if (!resultsTracking.isEmpty()) {
                        parcel.put("packages", resultsTracking);
                    } else {
                        parcel.put("packages", new JsonArray());
                    }
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


    private CompletableFuture<JsonObject> getSalesPackages(JsonObject parcel){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(parcel.getInteger("id"));
        String shippingType = parcel.getString("shipping_type");
        String QUERY = QUERY_SALES_REPORT_PACKAGES;
        if(shippingType != null){
            params.add(shippingType);
            QUERY += " AND pp.shipping_type = ?";
        }

        QUERY += " GROUP BY pp.shipping_type, pp.package_type_id, pp.package_price_id";

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

    /**
     * Contingency arrival process where you update the status
     * of the packages and the parcel to which they belong
     * @param message
     */
    private void arrivalContingency(Message<JsonObject> message){
        JsonObject body = message.body();
        String notes = body.getString("notes");
        Integer terminalId = body.getInteger("terminal_id");
        Integer created_by = body.getInteger("created_by");
        JsonArray packages = (JsonArray) body.getValue(PACKAGES);
        JsonArray returnArray = new JsonArray();
        this.startTransaction(message, conn -> {
            this.getParcelsDetail(conn, packages, terminalId).whenComplete((resultParcelsDetail, errorParcelsDetail) -> {
               try {
                   if (errorParcelsDetail != null){
                       throw errorParcelsDetail;
                   }
                   this.checkPackagesArrivalContingency(conn, packages, terminalId, returnArray, resultParcelsDetail, notes, created_by).whenComplete((resultCheckPackagesArrivalContingency, errorCheckPackagesArrivalContingency) ->{
                       try {
                           if (errorCheckPackagesArrivalContingency != null){
                               throw errorCheckPackagesArrivalContingency;
                           }
                           this.checkParcelArrivalContingency(conn, terminalId, new JsonArray(resultParcelsDetail)).whenComplete((resultCheckParcelArrivalContingency, errorCheckParcelArrivalContingency) -> {
                               try {
                                   if (errorCheckParcelArrivalContingency != null){
                                       throw errorCheckParcelArrivalContingency;
                                   }else{
                                       this.checkParcelArrivalContingencyManifestRadEad(conn, new JsonArray(resultParcelsDetail),created_by).whenComplete((resultCheckParcelArrivalContingencyManifestRadEad, errorCheckParcelArrivalContingencyManifestRadEad) -> {
                                           try {
                                               if (errorCheckParcelArrivalContingencyManifestRadEad != null){
                                                   throw errorCheckParcelArrivalContingencyManifestRadEad;
                                               }
                                               this.commit(conn, message, new JsonObject().put("updated", true).put("result", returnArray));
                                           } catch (Throwable t){
                                               t.printStackTrace();
                                               this.rollback(conn, t, message);
                                           }
                                       });

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
        });
    }

    /**
     * Obtains parcels list with packages
     * @param conn
     * @param packages Packages reference
     * @return Parcels list
     */
    private CompletableFuture<List<JsonObject>> getParcelsDetail(SQLConnection conn, JsonArray packages, Integer terminalId){
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        this.getParcelsIdList(conn, packages, terminalId).whenComplete((resultGetParcelsIdList, errorGetParcelsIdList) -> {
            try {
                if (errorGetParcelsIdList != null) {
                    throw errorGetParcelsIdList;
                }

                List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();
                for (int i = 0; i < resultGetParcelsIdList.size(); i++){
                    JsonObject parcel = resultGetParcelsIdList.get(i);
                    tasks.add(this.getPackagesDetail(conn, parcel));
                }
                CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((resultParcelsDetail, errorParcelsDetail) -> {
                    try {
                        if (errorParcelsDetail != null){
                            throw errorParcelsDetail;
                        }
                        future.complete(resultGetParcelsIdList);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Obtains parcels list by packages array
     * @param conn
     * @param packages
     * @return Parcels id list
     */
    private CompletableFuture<List<JsonObject>> getParcelsIdList(SQLConnection conn, JsonArray packages, Integer terminalId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            String paramString = "(";
            for(int i=0; i<packages.size(); i++){
                if (i < packages.size() - 1){
                    paramString = paramString.concat(packages.getInteger(i).toString()).concat(",");
                } else {
                    paramString = paramString.concat(packages.getInteger(i).toString());
                }
            }
            paramString = paramString.concat(") GROUP BY pp.parcel_id;");
            conn.queryWithParams(QUERY_GET_PARCELS_ID_LIST.concat(paramString), new JsonArray().add(terminalId), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }

                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()) {
                        throw new Exception("Parcels id not found");
                    }

                    for (JsonObject parcel : result) {
                        Integer terminalOriginId = parcel.getInteger(_TERMINAL_ORIGIN_ID);
                        if(terminalOriginId.equals(terminalId)) {
                            throw new Exception("The terminal is the same as the origin");
                        }
                    }

                    future.complete(result);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    /**
     * Obtains packages of parcel
     * @param conn
     * @param parcel Parcel reference
     * @return Packages of parcel
     */
    private CompletableFuture<JsonArray> getPackagesDetail(SQLConnection conn, JsonObject parcel){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        Integer parcelId = parcel.getInteger(ID);
        conn.queryWithParams("SELECT id, package_status FROM parcels_packages WHERE parcel_id = ?;", new JsonArray().add(parcelId), reply -> {
           try {
               if (reply.failed()){
                   throw reply.cause();
               }
               List<JsonObject> result = reply.result().getRows();
               parcel.put(PACKAGES, result);
               future.complete(new JsonArray(result));
           } catch (Throwable t){
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    /**
     * Packages iterator for check the package tracking
     * @param conn
     * @param packages Packages array to check
     * @param terminalId Terminal reference
     * @param returnArray Final result array
     * @param parcelsList
     * @return
     */
    private CompletableFuture<Boolean> checkPackagesArrivalContingency(SQLConnection conn, JsonArray packages, Integer terminalId, JsonArray returnArray, List<JsonObject> parcelsList, String notes, Integer created_by) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        List<CompletableFuture<JsonArray>> tasks = new ArrayList<>();

        for(int i = 0; i < packages.size(); i++){
            Integer pack = packages.getInteger(i);
            tasks.add(checkPackageTracking(conn, terminalId, pack, returnArray, parcelsList, notes, created_by));
        }

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()])).whenComplete((result, error) -> {
            try {
                if (error != null){
                    throw error;
                }
                future.complete(true);
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;

    }

    /**
     * Obtains the last record in package tracking and compare if the
     * package is in the reference terminal or the package status is updated
     * @param conn
     * @param terminalId Reference terminal
     * @param parcelPackageId
     * @param returnArray Final result array
     * @param parcelsList
     * @return Final result array
     */
    private CompletableFuture<JsonArray> checkPackageTracking(SQLConnection conn, Integer terminalId, Integer parcelPackageId, JsonArray returnArray, List<JsonObject> parcelsList, String notes, Integer created_by){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(parcelPackageId);
        conn.queryWithParams(QUERY_GET_PACKAGE_TRACKING, params, reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> packagesTracking = reply.result().getRows();
                if (packagesTracking.isEmpty()){
                    this.comparePackageDestiny(conn, terminalId, parcelPackageId, parcelsList, notes, created_by).whenComplete((resultComparePackageDestiny, errorComparePackageDestiny) -> {
                        try {
                            if (errorComparePackageDestiny != null){
                                throw errorComparePackageDestiny;
                            }
                            returnArray.add(resultComparePackageDestiny);
                            future.complete(returnArray);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
                } else {
                    JsonObject packageTrack = packagesTracking.get(0);
                    Integer packTerminalId = packageTrack.getInteger(_TERMINAL_ID);
                    PARCELPACKAGETRACKING_STATUS trackingStatus = PARCELPACKAGETRACKING_STATUS.fromValue(packageTrack.getString(_ACTION));
                    if(packTerminalId.equals(terminalId) && trackingStatus.notValidArrivalContingency()) {
                        returnArray.add(new JsonObject().put("parcel_package_id", parcelPackageId).put(STATUS, "in_terminal"));
                        future.complete(returnArray);
                    } else {
                        this.comparePackageDestiny(conn, terminalId, parcelPackageId, parcelsList, notes, created_by).whenComplete((resultComparePackageDestiny, errorComparePackageDestiny) -> {
                            try {
                                if (errorComparePackageDestiny != null){
                                    throw errorComparePackageDestiny;
                                }
                                returnArray.add(resultComparePackageDestiny);
                                future.complete(returnArray);
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
                    }
                }
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Compare the destination of the package with the reference terminal to determine the status of the package,
     * insert the activity record and update the package status
     * @param conn
     * @param terminalId Reference terminal
     * @param parcelPackageId
     * @param parcelsList
     * @return Package info result
     */
    private CompletableFuture<JsonObject> comparePackageDestiny(SQLConnection conn, Integer terminalId, Integer parcelPackageId, List<JsonObject> parcelsList, String notes, Integer created_by){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_PACKAGE_DESTINY, new JsonArray().add(terminalId).add(terminalId).add(parcelPackageId), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                List<JsonObject> results = reply.result().getRows();
                if (results.isEmpty()){
                    throw new Exception(parcelPackageId + " parcel_package_id not exists");
                }
                JsonObject resultPackage = results.get(0);
                Integer parcelId = resultPackage.getInteger("parcel_id");
                Integer parcelTranshipmentId = resultPackage.getInteger("parcel_transhipment_id");
                boolean isInReplacementTerminal = resultPackage.getInteger("is_in_replacement_terminal") == 1;
                Integer terminalDestinyId = resultPackage.getInteger("terminal_destiny_id");
                boolean receiveTranshipments = resultPackage.getBoolean("receive_transhipments");
                PACKAGE_STATUS packageStatus = PACKAGE_STATUS.values()[resultPackage.getInteger("package_status")];

               if (packageStatus.equals(PACKAGE_STATUS.ARRIVED) || packageStatus.equals(PACKAGE_STATUS.CANCELED)
                       || packageStatus.equals(PACKAGE_STATUS.DELIVERED) || packageStatus.equals(PACKAGE_STATUS.DELIVERED_CANCEL)) {
                   throw new Exception(parcelPackageId + " parcel_package_id have status " + packageStatus.name().toLowerCase());
               }

                JsonObject parcelReference = this.getJsonObjectReference(parcelsList, parcelId);
                List<JsonObject> parcelPackagesReference = new ArrayList<>();
                parcelReference.getJsonArray(PACKAGES).forEach(p -> parcelPackagesReference.add((JsonObject) p));

                String packageStatusName;
                int packageStatusOrdinal;
                if (terminalId.equals(terminalDestinyId) || isInReplacementTerminal){
                    packageStatusName = PACKAGE_STATUS.ARRIVED.name().toLowerCase();
                    packageStatusOrdinal = PACKAGE_STATUS.ARRIVED.ordinal();
                } else if (Objects.nonNull(parcelTranshipmentId) || receiveTranshipments){
                    packageStatusName = PACKAGE_STATUS.READY_TO_TRANSHIPMENT.name().toLowerCase();
                    packageStatusOrdinal = PACKAGE_STATUS.READY_TO_TRANSHIPMENT.ordinal();
                } else {
                    packageStatusName = PACKAGE_STATUS.LOCATED.name().toLowerCase();
                    packageStatusOrdinal = PACKAGE_STATUS.LOCATED.ordinal();
                }
                Integer finalTerminalId = isInReplacementTerminal ? terminalDestinyId : terminalId;
                this.insertTrackingContingency(conn, parcelId, parcelPackageId, finalTerminalId, packageStatusName, notes, created_by)
                        .whenComplete((resultInsertTrackingContingency, errorInsertTrackingContingency) -> {
                            try {
                                if (errorInsertTrackingContingency != null) {
                                    throw errorInsertTrackingContingency;
                                }
                                this.updatePackageStatus(conn, parcelPackageId, packageStatusOrdinal).whenComplete((packageUpdated, errorUpdatePackageStatus) -> {
                                    try {
                                        if (errorUpdatePackageStatus != null){
                                            throw errorUpdatePackageStatus;
                                        }
                                        JsonObject packageReference = this.getJsonObjectReference(parcelPackagesReference, parcelPackageId);
                                        packageReference.mergeIn(packageUpdated, true);

                                        future.complete(new JsonObject()
                                                .put("parcel_package_id", parcelPackageId)
                                                .put(STATUS, packageStatusName));
                                    } catch (Throwable t){
                                        future.completeExceptionally(t);
                                    }
                                });
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Obtain object resulting from the comparison
     * @param compareList List to compare
     * @param idToCompare Id to compare
     * @return Result object
     */
    private JsonObject getJsonObjectReference(List<JsonObject> compareList, Integer idToCompare){
        List<JsonObject> list = compareList.stream()
                .filter(obj -> obj.getInteger(ID).equals(idToCompare))
                .collect(toList());
        return list.get(0);
    }

    /**
     * Insert record of where the package arrived with a certain status
     * @param conn
     * @param parcelId Parcel to which the package belongs
     * @param parcelPackageId Parcel package id arrived
     * @param terminalId Terminal where the package arrived
     * @param actionName Package status name
     * @return Register's flag
     */
    private CompletableFuture<Boolean> insertTrackingContingency(SQLConnection conn, Integer parcelId, Integer parcelPackageId, Integer terminalId, String actionName, String notes, Integer created_by){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonObject insertObj = new JsonObject()
                .put("parcel_id", parcelId)
                .put("parcel_package_id", parcelPackageId)
                .put("action", actionName)
                .put("is_contingency", true)
                .put("terminal_id", terminalId)
                .put("notes",notes)
                .put("created_by",created_by);
        String insert = this.generateGenericCreate("parcels_packages_tracking", insertObj);
        conn.update(insert, reply ->{
            try {
                if (reply.failed()){
                   throw reply.cause();
                }
                future.complete(reply.succeeded());
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Update package status
     * @param conn
     * @param parcelPackageId
     * @param action Package status
     * @return Object updated
     */
    private CompletableFuture<JsonObject> updatePackageStatus(SQLConnection conn, Integer parcelPackageId, Integer action){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject updatePackage = new JsonObject()
                .put(ID, parcelPackageId)
                .put("package_status", action);

        String update = this.generateGenericUpdateString("parcels_packages", updatePackage);
        conn.update(update, reply ->{
            try {
                if (reply.failed()){
                    throw reply.cause();
                }
                future.complete(updatePackage);
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Parcels iterator for update status
     * @param conn
     * @param terminalId
     * @param parcelsList
     * @return
     */
    private CompletableFuture<JsonArray> checkParcelArrivalContingency(SQLConnection conn, Integer terminalId, JsonArray parcelsList){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(parcelsList.stream()
                .map(parcel -> updateParcelStatusContingency(conn, terminalId, (JsonObject) parcel))
                .toArray(CompletableFuture[]::new))
                .whenComplete((s, tt) -> {
                    try {
                        if (tt != null) {
                            throw tt;
                        }
                        future.complete(parcelsList);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
        return future;
    }

    /**
     * Get a query to update the parcel status
     * @param locatedPackages Quantity located packages
     * @param arrivedPackages Quantity arrived packages
     * @param totalPackages Quantity total packages
     * @param parcelId Parcel to update
     * @return Update parcel status query
     */
    private String getUpdateParcelContingencyString(Integer locatedPackages, Integer arrivedPackages, Integer totalPackages, Integer parcelId){
        String updateParcel = null;
        if (!locatedPackages.equals(0) && totalPackages.equals(locatedPackages)){
            updateParcel = this.generateGenericUpdateString("parcels",
                    new JsonObject()
                            .put(ID, parcelId)
                            .put("parcel_status", PARCEL_STATUS.LOCATED.ordinal()));
        } else if(locatedPackages > 0 && totalPackages > locatedPackages && arrivedPackages.equals(0)){
            updateParcel = this.generateGenericUpdateString("parcels",
                    new JsonObject()
                            .put(ID, parcelId)
                            .put("parcel_status", PARCEL_STATUS.LOCATED_INCOMPLETE.ordinal()));
        } else if (!arrivedPackages.equals(0) && totalPackages.equals(arrivedPackages)) {
            updateParcel = this.generateGenericUpdateString("parcels",
                    new JsonObject()
                            .put(ID, parcelId)
                            .put("parcel_status", PARCEL_STATUS.ARRIVED.ordinal()));
        } else if (arrivedPackages > 0 && totalPackages > arrivedPackages){
            updateParcel = this.generateGenericUpdateString("parcels",
                    new JsonObject()
                            .put(ID, parcelId)
                            .put("parcel_status", PARCEL_STATUS.ARRIVED_INCOMPLETE.ordinal()));
        }
        return updateParcel;
    }

    /**
     * Get total packages as well as those arrived and located
     * @param packages Packages to compare
     * @return Total packages, arrived and located
     */
    private CompletableFuture<JsonObject> getContingencyInfoByParcel(JsonArray packages){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonObject contingencyInfo = new JsonObject();
        int arrived = this.countPackgesByStatus(packages, PACKAGE_STATUS.ARRIVED.ordinal());
        int located = this.countPackgesByStatus(packages, PACKAGE_STATUS.LOCATED.ordinal());
        int total = packages.size();
        contingencyInfo
                .put(PARCEL_STATUS.LOCATED.name().toLowerCase(), located)
                .put(PARCEL_STATUS.ARRIVED.name().toLowerCase(), arrived)
                .put(TOTAL, total);
        future.complete(contingencyInfo);
        return future;
    }

    /**
     * Packages counter by status
     * @param packages Packages to compare
     * @param status Package status to compare
     * @return Counter result
     */
    private int countPackgesByStatus(JsonArray packages, int status){
        int count = 0;
        for (int i = 0; i < packages.size(); i++){
            JsonObject pack = packages.getJsonObject(i);
            int packageStatus = pack.getInteger("package_status");
            if (packageStatus == status) {
                count++;
            }
        }
        return count;
    }

    /**
     * Update parcel status
     * @param conn
     * @param parcel Parcel to update
     * @return
     */
    private CompletableFuture<Boolean> updateParcelStatusContingency(SQLConnection conn, Integer terminalId, JsonObject parcel){
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Integer terminalDestinyId = parcel.getInteger(_TERMINAL_DESTINY_ID);
        boolean isInReplacementTerminal = parcel.getInteger("is_in_replacement_terminal") == 1;
        if(terminalId.equals(terminalDestinyId) || isInReplacementTerminal) {
            JsonArray parcelPackages = parcel.getJsonArray(PACKAGES);
            this.getContingencyInfoByParcel(parcelPackages).whenComplete((contingencyInfo, errorContingencyInfo) -> {
                try {
                    if (errorContingencyInfo != null){
                        throw errorContingencyInfo;
                    }
                    Integer totalPackages = contingencyInfo.getInteger(TOTAL);
                    Integer locatedPackages = contingencyInfo.getInteger(PARCEL_STATUS.LOCATED.name().toLowerCase());
                    Integer arrivedPackages = contingencyInfo.getInteger(PARCEL_STATUS.ARRIVED.name().toLowerCase());

                    String updateParcel = this.getUpdateParcelContingencyString(locatedPackages, arrivedPackages, totalPackages, parcel.getInteger(ID));

                    if (updateParcel != null){
                        conn.update(updateParcel, replyUpdate ->{
                            try {
                                if (replyUpdate.failed()){
                                    throw replyUpdate.cause();
                                }
                                future.complete(replyUpdate.succeeded());
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
                    } else {
                        future.complete(true);
                    }
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } else {
            GenericQuery updateParcel = this.generateGenericUpdate("parcels", new JsonObject()
                    .put(ID, parcel.getInteger(ID))
                    .put("parcel_status", PARCEL_STATUS.IN_TRANSIT.ordinal()));
            conn.updateWithParams(updateParcel.getQuery(), updateParcel.getParams(), replyUpdate ->{
                try {
                    if (replyUpdate.failed()){
                        throw replyUpdate.cause();
                    }
                    future.complete(true);
                } catch (Throwable t){
                    future.completeExceptionally(t);
                }
            });
        }
        return future;
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

    private void registerPostalCode(Message<JsonObject> message){
        this.startTransaction(message, (SQLConnection conn) -> {
            try{
                //JsonObject postal = message.body().copy();
                JsonObject copy = message.body().copy();
                copy.put(CREATED_AT,sdfDataBase(new Date()));

                this.insertPostalCode(conn,copy).whenComplete((res,error) -> {
                    try{
                        if(error != null){
                            this.rollback(conn, error, message);
                        } else {
                            this.commit(conn, message , res);
                        }
                    } catch (Exception e){
                        this.rollback(conn,error,message);
                    }
                });

            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn,t , message);
            }
        });
    }

    private CompletableFuture<JsonObject> insertPostalCode(SQLConnection conn, JsonObject params){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String insert = this.generateGenericCreate("parcel_coverage",params);

        conn.update(insert,(AsyncResult<UpdateResult> reply) -> {
           try{
               if(reply.succeeded()){
                   future.complete(reply.result().toJson());
               } else {
                   future.completeExceptionally(reply.cause());
               }
           } catch (Exception e){
               future.completeExceptionally(e);
           }
        });
        return future;
    }

    private void searchValidCodes(Message<JsonObject> message){
        try {
            JsonObject body = message.body();

            String QUERY = QUERY_GET_VALID_CODES;

            //JsonArray params = new JsonArray().add(initDate).add(endDate);


            this.dbClient.query(QUERY, reply -> {
               try{
                   if (reply.failed()){
                       throw reply.cause();
                   }
                   List<JsonObject> codes = reply.result().getRows();
                   if (codes.isEmpty()){
                       throw new Exception("Codes not found");
                   }
                   message.reply(new JsonArray(codes));

               }  catch (Throwable t){
                   t.printStackTrace();
                   reportQueryError(message, t);
               }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void branchofficesWParcelCoverage(Message<JsonObject> message){
        try {
            this.dbClient.query(QUERY_GET_BRANCHOFFICE_W_PARCEL_COVERAGE, reply -> {
               try{
                   if (reply.failed()){
                       throw reply.cause();
                   }
                   List<JsonObject> codes = reply.result().getRows();
                   message.reply(new JsonArray(codes));

               }  catch (Throwable t){
                   t.printStackTrace();
                   reportQueryError(message, t);
               }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void searchValidCodesV2(Message<JsonObject> message){
        try {
            String QUERY = QUERY_GET_VALID_CODES;
            JsonObject body = message.body();
            Integer zipCode = body.getInteger("zip_code");
            Integer branchofficeId = body.getInteger(_BRANCHOFFICE_ID);
            JsonArray params = new JsonArray();
            if(Objects.nonNull(zipCode)) {
                QUERY += "  AND pc.zip_code LIKE CONCAT(?, '%')";
                params.add(zipCode);
            }
            if(Objects.nonNull(branchofficeId)) {
                QUERY += "  AND pc.branchoffice_id = ?";
                params.add(branchofficeId);
            }
            if(Objects.isNull(zipCode) && Objects.isNull(branchofficeId)) {
                throw new Exception("The params zip_code or branchoffice_id cannot be null");
            }
            QUERY += " GROUP BY pc.id";

            this.dbClient.queryWithParams(QUERY, params, reply -> {
               try{
                   if (reply.failed()){
                       throw reply.cause();
                   }
                   List<JsonObject> codes = reply.result().getRows();
                   message.reply(new JsonArray(codes));

               }  catch (Throwable t){
                   t.printStackTrace();
                   reportQueryError(message, t);
               }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void zipCodeIsOnCoverage(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            String zipCode = body.getString(ZIP_CODE);
            JsonArray params = new JsonArray().add(zipCode);


            this.dbClient.queryWithParams(QUERY_GET_ZIP_CODE_COVERAGE, params, reply -> {
                try{
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> codes = reply.result().getRows();
                    if (codes.isEmpty()){
                        throw new Exception("Zip code not found");
                    }
                    JsonObject code = codes.get(0);
                    message.reply(code);

                }  catch (Throwable t){
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void updatePostalCode(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer status=Integer.parseInt(body.getString("status"));
        JsonArray params = new JsonArray()
                .add(status)
                .add(sdfDataBase(new Date()))
                .add(Integer.parseInt(body.getString("updated_by"))).add(body.getInteger("id"));

        this.dbClient.queryWithParams(QUERY_UPDATE_POSTAL_CODE, params, reply -> {
            if (reply.succeeded()) {

                message.reply(reply.succeeded());

            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private CompletableFuture<JsonObject> insertService(SQLConnection conn, JsonObject parcel , Integer createdBy, JsonObject serviceObject , Integer parcelId) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            JsonObject service = new JsonObject();
            if(serviceObject.getBoolean("is_rad") != null || serviceObject.getBoolean("is_ead") != null || serviceObject.getBoolean("is_rad_ead") != null ){

                service.put("parcel_id", parcelId);
                service.put("amount", serviceObject.getDouble("service_amount"));
                service.put("id_type_service", serviceObject.getInteger("id_type_service"));
                service.put("zip_code", serviceObject.getInteger("zip_code"));
                service.put("created_by", createdBy);
                service.put("created_at", sdfDataBase(new Date()));
                // && serviceObject.getBoolean("is_ead") == null
                if(serviceObject.getBoolean("is_rad") != null){
                    service.put("confirme_rad", 0);
                    service.put("status", 1);
                }

                if(  serviceObject.getBoolean("is_rad_ead") != null){
                    service.put("confirme_rad", 1);
                    service.put("status", 1);
                }

                if(serviceObject.getBoolean("is_ead") != null ){
                    service.put("confirme_rad", 1);
                    service.put("status", 1);
                }

                String insert = this.generateGenericCreate("parcels_rad_ead", service);

                conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
                    try{
                        if (reply.succeeded()) {

                            future.complete(service);

                        } else {
                            future.completeExceptionally(reply.cause());
                        }
                    } catch (Exception e){
                        future.completeExceptionally(e);
                    }
                });
            }
            else{
                future.complete(service);
            }


        } catch (Throwable ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }

        return future;
    }

    private CompletableFuture<JsonObject> setRadEadStatus(SQLConnection conn, Integer parcelId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        JsonArray params = new JsonArray().add(parcelId);
        conn.queryWithParams(GET_PARCEL_RAD_EAD_BY_PARCELID,params, replySelect -> {
            try{
                if (replySelect.succeeded()){
                    List<JsonObject> resultSelect = replySelect.result().getRows();
                    JsonObject objectRadEad = new JsonObject();
                    if(resultSelect.isEmpty()){
                       // future.completeExceptionally(replySelect.cause());
                        future.complete(objectRadEad);
                    }

                    objectRadEad.put("id",resultSelect.get(0).getInteger("id"));
                    objectRadEad.put("status", 7);
                    String queryUpdate = this.generateGenericUpdateString("parcels_rad_ead" , objectRadEad);

                    this.dbClient.query(queryUpdate , replyUpdate -> {
                        try {
                            if(replyUpdate.succeeded()){
                                //this.commit(conn, message , objectRadEad);
                                future.complete(objectRadEad);
                            } else {
                                future.completeExceptionally(replyUpdate.cause());
                            }
                           // future.completeExceptionally(e);

                        } catch (Exception  e){
                            future.completeExceptionally(e);
                        }
                    });

                }
            } catch (Exception e){
               future.completeExceptionally(e);
            }
        });
        return future;
    }


    private CompletableFuture<JsonArray> checkParcelArrivalContingencyManifestRadEad(SQLConnection conn, JsonArray parcelsList, int create_by){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        CompletableFuture.allOf(parcelsList.stream()
                .map(parcel -> updateParcelRadEadStatusContingency(conn, (JsonObject) parcel,create_by))
                .toArray(CompletableFuture[]::new))
                .whenComplete((s, tt) -> {
                    try {
                        if (tt != null) {
                            throw tt;
                        }
                        future.complete(parcelsList);
                    } catch (Throwable t){
                        future.completeExceptionally(t);
                    }
                });
        return future;
    }


    private CompletableFuture<Boolean> updateParcelRadEadStatusContingency(SQLConnection conn, JsonObject parcel, int create_by){
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        JsonArray parcelPackages = parcel.getJsonArray(PACKAGES);

        int idParcel=parcel.getInteger("id");
        int updateBy=create_by;
        conn.query(GET_PARCEL_RAD_EAD_ID_CONTINGENCY.concat( String.valueOf(idParcel)), reply -> {
            try {
                if (reply.failed()){
                    throw reply.cause();
                }else{
                    if(reply.result().getNumRows()>0)
                    {
                        JsonArray params = new JsonArray()
                                .add(updateBy)
                                .add(idParcel);


                        conn.updateWithParams(UPDATE_STATUS_PARCELS_RAD_EAD_CONTINGENCY,params, (AsyncResult<UpdateResult> parcelReply) -> {
                            try {
                                if (parcelReply.failed()) {
                                    throw parcelReply.cause();
                                }
                                future.complete(parcelReply.succeeded());
                            } catch (Throwable t) {
                                future.completeExceptionally(t);
                            }
                        });
                    }else{
                        future.complete(reply.succeeded());
                    }



                }
                // future.complete(reply.result().getRows());
            } catch (Throwable t){
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private void getLettersPorteComplement(Message<JsonObject> message){
        try {
            JsonObject body = message.body();
            Integer terminalOriginId=Integer.parseInt(body.getString("terminal_origin_id"));
            String date_init=body.getString("date_init");
            String date_end=body.getString("date_end");
            String QUERY="";
            if(date_init.equals("null") && date_end.equals("null")){
                QUERY = GET_PARCEL_INVOICE_COMPLEMENT+terminalOriginId;

            }
            else{
                QUERY = GET_PARCEL_INVOICE_COMPLEMENT_FILTER_DATE+terminalOriginId+" AND (p.created_at>='"+date_init+" 00:00:00' AND p.created_at<='"+date_end+" 23:59:59')" ;

            }

            this.dbClient.queryWithParams(QUERY, null, reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
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

    private void generatePDF(Message<JsonObject> message){
        try{
            JsonArray parameters = new JsonArray();
            parameters.add(message.body().getString("parcel_prepaid_id"));
            String query = GET_PARCELS_PREPAID;
            query = query.concat(" LIMIT ? ");
            Integer end = 0;
            Integer start = 0;
            Integer total = 1000;
            if(message.body().getString("end") != null) {
                end = Integer.valueOf(message.body().getString("end", "1000"));
            }
            if(message.body().getString("start") != null) {
                query = query.concat(" OFFSET ?");
                start = Integer.valueOf(message.body().getString("start", "0")) - 1;
            }

            total = end - start;
            parameters.add(total).add(start);
            this.dbClient.queryWithParams(query, parameters, handler -> {
               try{
                   List<JsonObject> result = handler.result().getRows();
                   String absolutePathfile = new File("").getAbsolutePath() + "/files/Guia.jrxml";
                   JasperReport report = JasperCompileManager.compileReport(absolutePathfile);
                   Map<String, Object> map = new HashMap<>();
                   JasperPrint printResult = JasperFillManager.fillReport(report, map, new JREmptyDataSource());
                   printResult.removePage(0);
                   for(int i=0;i <= result.size();i++) {
                       if(i == result.size()){
                           message.reply(JasperExportManager.exportReportToPdf(printResult));
                           break;
                       }

                       JsonObject resultObject = result.get(i);

                       Map<String, Object> mapR = new HashMap<>();
                       mapR.put("guiapp_code", resultObject.getString("guiapp_code"));
                       mapR.put("tracking_code", resultObject.getString("tracking_code"));
                       mapR.put("fecha", resultObject.getString("created_at"));
                       mapR.put("hora", resultObject.getString("hour"));
                       mapR.put("rango_km", resultObject.getString("rango"));
                       mapR.put("shipment_type", resultObject.getString("shipment_type"));
                       mapR.put("payment_condition", resultObject.getString("payment_condition"));
                       mapR.put("remitente", resultObject.getString("name"));
                       mapR.put("tel", resultObject.getString("phone"));
                       mapR.put("tarifa", resultObject.getString("tarifa"));
                       mapR.put("peso", resultObject.getString("peso"));
                       mapR.put("qrcode", getQRCodeImage(resultObject.getString("guiapp_code"), 500 , 500));
                       if( i >= result.size()-1  || i == result.size()){
                           JRPrintPage singlePage = JasperFillManager.fillReport(report, mapR, new JREmptyDataSource(1) ).getPages().get(0);
                           printResult.addPage(singlePage);
                           singlePage = null;
                           message.reply(JasperExportManager.exportReportToPdf(printResult));
                           break;
                       }
                       JsonObject resultObject2 = result.get(i+1);
                       mapR.put("guiapp_code_2", resultObject2.getString("guiapp_code"));
                       mapR.put("tracking_code_2", resultObject2.getString("tracking_code"));
                       mapR.put("fecha_2", resultObject2.getString("created_at"));
                       mapR.put("hora_2", resultObject2.getString("hour"));
                       mapR.put("rango_km_2", resultObject2.getString("rango"));
                       mapR.put("shipment_type_2", resultObject2.getString("shipment_type"));
                       mapR.put("payment_condition_2", resultObject2.getString("payment_condition"));
                       mapR.put("remitente_2", resultObject2.getString("name"));
                       mapR.put("tel_2", resultObject2.getString("phone"));
                       mapR.put("tarifa_2", resultObject2.getString("tarifa"));
                       mapR.put("peso_2", resultObject2.getString("peso"));
                       mapR.put("qrcode_2", getQRCodeImage(resultObject2.getString("guiapp_code"), 500 , 500)  );

                       JRPrintPage singlePage = JasperFillManager.fillReport(report, mapR, new JREmptyDataSource(1) ).getPages().get(0);
                       printResult.addPage(singlePage);
                       i++;
                       mapR = null;
                       singlePage = null;
                   }
                   printResult = null;
                   report = null;
                   result = null;
               } catch (Exception e){
                   e.printStackTrace();
               }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void accumulatedParcelByAdviserReport (Message<JsonObject> message) {
        try{
            JsonObject body = message.body();
            Integer terminalCityId = body.getInteger("terminal_city_id");
            Integer terminalId = body.getInteger("terminal_id");
            String groupBy = body.getString("group_by");
            String shippingParcelType = body.getString("shipping_parcel");
            JsonArray sellers = body.getJsonArray("seller_array");
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            JsonObject complementQueryData = new JsonObject()
                    .put("sellers", sellers)
                    .putNull("branchoffice_id")
                    .putNull("customer_parcel_type");

            this.getCustomersByCriteriaForParcelReport(complementQueryData).whenComplete((resultCustomersIds, errorCustomersIds) -> {
                if (errorCustomersIds != null) {
                    reportQueryError(message, errorCustomersIds.getCause());
                } else {
                    String QUERY = QUERY_ACCUMULATED_BY_ADVISER_REPORT;
                    String WHERE_PARCELS = "p.parcel_status != 4 AND p.total_amount > 0 AND p.created_at BETWEEN ? AND ?";
                    String WHERE_PARCELS_PREPAID = "pp.parcel_status != 4 AND pp.total_amount > 0 AND pp.created_at BETWEEN ? AND ? AND c.user_seller_id IS NOT NULL";

                    JsonArray paramsParcels = new JsonArray();
                    JsonArray paramsPrepaid = new JsonArray();

                    paramsParcels.add(initDate).add(endDate);
                    paramsPrepaid.add(initDate).add(endDate);

                    if(terminalCityId != null){
                        WHERE_PARCELS += " AND b.city_id = ?";
                        WHERE_PARCELS_PREPAID += " AND b.city_id = ?";
                        paramsParcels.add(terminalCityId);
                        paramsPrepaid.add(terminalCityId);
                    }
                    if(terminalId != null){
                        WHERE_PARCELS += " AND p.branchoffice_id = ?";
                        WHERE_PARCELS_PREPAID += " AND pp.branchoffice_id = ?";
                        paramsParcels.add(terminalId);
                        paramsPrepaid.add(terminalId);
                    }
                    if(shippingParcelType != null){
                        WHERE_PARCELS+= " AND p.shipment_type = ?";
                        WHERE_PARCELS_PREPAID+= " AND pp.shipment_type = ?";
                        paramsParcels.add(shippingParcelType);
                        paramsPrepaid.add(shippingParcelType);
                    }

                    if (resultCustomersIds.getBoolean("has_criteria")) {
                        String[] customerIds = resultCustomersIds.getString("customers_ids").split(",");
                        if (customerIds.length > 0 && !customerIds[0].isEmpty()) {
                            String placeholders = String.join(",", Collections.nCopies(customerIds.length, "?"));
                            for (String customerId : customerIds) {
                                paramsParcels.add(Integer.parseInt(customerId.trim()));
                                paramsPrepaid.add(Integer.parseInt(customerId.trim()));
                            }

                            WHERE_PARCELS += " AND (CASE " +
                                    "WHEN p.pays_sender = 0 " +
                                    "AND cc.user_seller_id IS NULL " +
                                    "AND cc.branchoffice_id IS NULL " +
                                    "AND cc.parcel_type IS NULL " +
                                    "AND cs.user_seller_id IS NOT NULL " +
                                    "AND cs.branchoffice_id IS NOT NULL " +
                                    "AND cs.parcel_type IS NOT NULL " +
                                    "THEN p.sender_id ELSE p.customer_id END) IN (" + placeholders + ")";

                            WHERE_PARCELS_PREPAID += " AND pp.customer_id IN (" + placeholders + ")";
                        }
                    }

                    WHERE_PARCELS += " AND (cc.user_seller_id IS NOT NULL " +
                            "OR (p.pays_sender = 0 " +
                            "AND cc.user_seller_id IS NULL AND cc.branchoffice_id IS NULL AND cc.parcel_type IS NULL " +
                            "AND cs.user_seller_id IS NOT NULL AND cs.branchoffice_id IS NOT NULL AND cs.parcel_type IS NOT NULL))";

                    QUERY = QUERY.replace("{WHERE_PARCELS}", WHERE_PARCELS)
                            .replace("{WHERE_PARCELS_PREPAID}", WHERE_PARCELS_PREPAID);

                    JsonArray params = new JsonArray();
                    params.addAll(paramsParcels);
                    params.addAll(paramsPrepaid);

                    this.dbClient.queryWithParams(QUERY, params, reply ->{
                        try{
                            if(reply.succeeded()){
                                List<JsonObject> resultList = reply.result().getRows();
                                JsonObject result = new JsonObject().put(RESULTS, resultList);
                                message.reply(result);

                            } else {
                                message.reply(new JsonArray());
                            }
                        } catch(Exception e){
                            reportQueryError(message, e.getCause());
                        }
                    });
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }

    private void accumulatedParcelByAdviserReportDetail(Message<JsonObject> message) {
        try{
            JsonObject body = message.body();
            Integer adviserId = body.getInteger("adviser_id");
            Integer terminalCityId = body.getInteger("terminal_city_id");
            Integer terminalId = body.getInteger("terminal_id");
            String shippingParcelType = body.getString("shipping_parcel");
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");

            String QUERY = QUERY_ACCUMULATED_BY_ADVISER_REPORT_DETAIL;
            String WHERE_PARCELS = "p.parcel_status != 4 AND p.total_amount > 0 AND p.created_at BETWEEN ? AND ?";
            String WHERE_PARCELS_PREPAID = "pp.parcel_status != 4 AND pp.total_amount > 0 AND pp.created_at BETWEEN ? AND ? AND c.user_seller_id = ?";

            JsonArray paramsParcels = new JsonArray();
            JsonArray paramsPrepaid = new JsonArray();

            paramsParcels.add(initDate).add(endDate);
            paramsPrepaid.add(initDate).add(endDate).add(adviserId);

            if(terminalCityId != null){
                WHERE_PARCELS += " AND b.city_id = ?";
                WHERE_PARCELS_PREPAID += " AND b.city_id = ?";
                paramsParcels.add(terminalCityId);
                paramsPrepaid.add(terminalCityId);
            }
            if(terminalId != null){
                WHERE_PARCELS += " AND p.branchoffice_id = ?";
                WHERE_PARCELS_PREPAID += " AND pp.branchoffice_id = ?";
                paramsParcels.add(terminalId);
                paramsPrepaid.add(terminalId);
            }
            if(shippingParcelType != null){
                WHERE_PARCELS+= " AND p.shipment_type = ?";
                WHERE_PARCELS_PREPAID+= " AND pp.shipment_type = ?";
                paramsParcels.add(shippingParcelType);
                paramsPrepaid.add(shippingParcelType);
            }

            // CUANDO UN CLIENTE CONVENIO/GUIAPP ENVÍA FXC Y CLIENTE DESTINO ES PUBLICO EN GENERAL:
            WHERE_PARCELS += " AND (cc.user_seller_id = ? " +
                    "OR (p.pays_sender = 0 AND cc.user_seller_id IS NULL AND cc.branchoffice_id IS NULL " +
                    "AND cc.parcel_type IS NULL AND cs.user_seller_id = ? AND cs.branchoffice_id IS NOT NULL " +
                    "AND cs.parcel_type IS NOT NULL))";
            paramsParcels.add(adviserId).add(adviserId);

            QUERY = QUERY.replace("{WHERE_PARCELS}", WHERE_PARCELS)
                    .replace("{WHERE_PARCELS_PREPAID}", WHERE_PARCELS_PREPAID);

            JsonArray params = new JsonArray();
            params.addAll(paramsParcels);
            params.addAll(paramsPrepaid);

            this.dbClient.queryWithParams(QUERY, params, reply ->{
                try{
                    if(reply.succeeded()){
                        List<JsonObject> resultList = reply.result().getRows();
                        JsonObject result = new JsonObject().put(RESULTS, resultList);
                        message.reply(result);

                    } else {
                        message.reply(new JsonArray());
                    }
                } catch(Exception e){
                    reportQueryError(message, e.getCause());
                }
            });
        }catch (Exception e){
            reportQueryError(message, e.getCause());
        }
    }

    private void generatePDFPrepaidSite(Message<JsonObject> message){
        try{
            JsonArray parameters = new JsonArray();
            String query = GET_AVAILABLE_PARCELS_PREPAID_SITE;
            Integer toPrint = message.body().getInteger("to_print");;
            Integer customerId = message.body().getInteger("customer_id");
            Integer rangeId = message.body().getInteger("range_id");
            Integer kmId = message.body().getInteger("km_id");
            parameters.add(customerId);

            if(rangeId != null && rangeId != 0) {
                query += " AND d.id = ?";
                parameters.add(rangeId);
            }
            if(kmId != null && kmId != 0) {
                query += " AND pkm.id = ?";
                parameters.add(kmId);
            }

            query += " ORDER BY p.id LIMIT ?";
            parameters.add(toPrint);

            this.dbClient.queryWithParams(query, parameters, handler -> {
                try{
                    List<JsonObject> result = handler.result().getRows();
                    String absolutePathfile = new File("").getAbsolutePath() + "/files/Guia.jrxml";
                    JasperReport report = JasperCompileManager.compileReport(absolutePathfile);
                    Map<String, Object> map = new HashMap<>();
                    JasperPrint printResult = JasperFillManager.fillReport(report, map, new JREmptyDataSource());
                    printResult.removePage(0);
                    for(int i=0;i <= result.size();i++) {
                        if(i == result.size()){
                            message.reply(JasperExportManager.exportReportToPdf(printResult));
                            break;
                        }

                        JsonObject resultObject = result.get(i);

                        Map<String, Object> mapR = new HashMap<>();
                        mapR.put("guiapp_code", resultObject.getString("guiapp_code"));
                        mapR.put("tracking_code", resultObject.getString("tracking_code"));
                        mapR.put("fecha", resultObject.getString("created_at"));
                        mapR.put("hora", resultObject.getString("hour"));
                        mapR.put("rango_km", resultObject.getString("rango"));
                        mapR.put("shipment_type", resultObject.getString("shipment_type"));
                        mapR.put("payment_condition", resultObject.getString("payment_condition"));
                        mapR.put("remitente", resultObject.getString("name"));
                        mapR.put("tel", resultObject.getString("phone"));
                        mapR.put("tarifa", resultObject.getString("tarifa"));
                        mapR.put("peso", resultObject.getString("peso"));
                        mapR.put("qrcode", getQRCodeImage(resultObject.getString("guiapp_code"), 500 , 500));
                        if( i >= result.size()-1  || i == result.size()){
                            JRPrintPage singlePage = JasperFillManager.fillReport(report, mapR, new JREmptyDataSource(1) ).getPages().get(0);
                            printResult.addPage(singlePage);
                            singlePage = null;
                            message.reply(JasperExportManager.exportReportToPdf(printResult));
                            break;
                        }
                        JsonObject resultObject2 = result.get(i+1);
                        mapR.put("guiapp_code_2", resultObject2.getString("guiapp_code"));
                        mapR.put("tracking_code_2", resultObject2.getString("tracking_code"));
                        mapR.put("fecha_2", resultObject2.getString("created_at"));
                        mapR.put("hora_2", resultObject2.getString("hour"));
                        mapR.put("rango_km_2", resultObject2.getString("rango"));
                        mapR.put("shipment_type_2", resultObject2.getString("shipment_type"));
                        mapR.put("payment_condition_2", resultObject2.getString("payment_condition"));
                        mapR.put("remitente_2", resultObject2.getString("name"));
                        mapR.put("tel_2", resultObject2.getString("phone"));
                        mapR.put("tarifa_2", resultObject2.getString("tarifa"));
                        mapR.put("peso_2", resultObject2.getString("peso"));
                        mapR.put("qrcode_2", getQRCodeImage(resultObject2.getString("guiapp_code"), 500 , 500)  );

                        JRPrintPage singlePage = JasperFillManager.fillReport(report, mapR, new JREmptyDataSource(1) ).getPages().get(0);
                        printResult.addPage(singlePage);
                        i++;
                        mapR = null;
                        singlePage = null;
                    }
                    printResult = null;
                    report = null;
                    result = null;
                } catch (Exception e){
                    e.printStackTrace();
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static byte[] getQRCodeImage(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageConfig con = new MatrixToImageConfig( 0xFF000002 , 16777215 ) ;

        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream,con);
        byte[] pngData = pngOutputStream.toByteArray();
        return pngData;
    }

    private static final String GET_PARCEL_INVOICE_COMPLEMENT="SELECT (SELECT package_status FROM parcels_packages where parcel_id=p.id limit 1) as status_packages_embarque, concat(p.sender_name,' ',p.sender_last_name ) as name_sender,\n" +
            "             p.created_at,b1.prefix as terminalOrigin,pic.xml, b2.prefix as terminaldestiny, \n" +
            "             p.shipment_type, p.parcel_tracking_code,pic.id as parcel_invoice_complement_id,pic.cfdi_body,p.total_packages\n" +
            "            from parcel_invoice_complement as pic\n" +
            "            inner join parcels as p on pic.id_parcel=p.id\n" +
            "            inner join branchoffice as b1 on b1.id = p.terminal_origin_id\n" +
            "            inner join branchoffice as b2 on b2.id = p.terminal_destiny_id\n" +
            "            where (p.parcel_status=0 or  p.parcel_status=1) and p.parcel_status!=4 and pic.status_cfdi!=5 and pic.status_cfdi!=4 and  pic.tipo_cfdi=2 and  p.terminal_origin_id=";
    private static final String GET_PARCEL_INVOICE_COMPLEMENT_FILTER_DATE="SELECT p.created_at as \"fechaDocumentacion\",(SELECT package_status FROM parcels_packages where parcel_id=p.id limit 1) as status_packages_embarque, concat(p.sender_name,' ',p.sender_last_name ) as name_sender,\n" +
            "                         p.created_at,b1.prefix as terminalOrigin,pic.xml, b2.prefix as terminaldestiny, \n" +
            "                         p.shipment_type, p.parcel_tracking_code,pic.id as parcel_invoice_complement_id,pic.cfdi_body,p.total_packages\n" +
            "                        from parcel_invoice_complement as pic\n" +
            "                        inner join parcels as p on pic.id_parcel=p.id\n" +
            "                        inner join branchoffice as b1 on b1.id = p.terminal_origin_id\n" +
            "                        inner join branchoffice as b2 on b2.id = p.terminal_destiny_id\n" +
            "                        where  pic.status_cfdi!=4  and p.parcel_status!=4 and pic.status_cfdi!=5 and pic.tipo_cfdi=2 and p.terminal_origin_id=";

    private static final String UPDATE_STATUS_PARCELS_RAD_EAD_CONTINGENCY = "UPDATE parcels_rad_ead \n " +
            " SET  status = 8 ,\n " +
            "confirme_rad = 1 ,\n "+
            "updated_by = ? \n " +
            " WHERE parcel_id = ? AND status != 4 ;";
    private static final String GET_PARCEL_RAD_EAD_ID_CONTINGENCY  = "select * from parcels_rad_ead where parcel_id = ";
    private static final String QUERY_GET_TIMELY_DELIVERY_DETAILS_REPORT = "SELECT\n" +
            "  bo.prefix AS terminal_origin_prefix,\n" +
            "  bd.prefix AS terminal_destiny_prefix,\n" +
            "  p.waybill,\n" +
            "  p.parcel_tracking_code,\n" +
            "  p.created_at AS documentation_date,\n" +
            "  srd.travel_date AS programed_departure_date,\n" +
            "  (SELECT created_at FROM parcels_packages_tracking WHERE parcel_id = p.id AND action = 'intransit' LIMIT 1) AS real_departure_date,\n" +
            "  srd.arrival_date AS programed_arrival_date,\n" +
            "  (SELECT created_at FROM parcels_packages_tracking WHERE parcel_id = p.id AND action = 'arrived' LIMIT 1) AS real_arrival_date,\n" +
            "  p.delivered_at AS customer_deliver,\n" +
            "  TIMESTAMPDIFF(DAY, p.created_at, p.delivered_at) AS real_delivery_time,\n" +
            "  TIMESTAMPDIFF(HOUR, p.created_at, p.delivered_at) AS difference_hours,\n" +
            "  IF(TIMESTAMPDIFF(HOUR, p.created_at, p.delivered_at) <= TIMESTAMPDIFF(HOUR, p.created_at, p.promise_delivery_date), 24, " +
             " CEILING((TIMESTAMPDIFF(HOUR, p.created_at, p.delivered_at) - TIMESTAMPDIFF(HOUR, p.created_at, p.promise_delivery_date) + 24) / 24) * 24 ) AS commercial_commitment\n" +
            " FROM parcels p\n" +
            " LEFT JOIN schedule_route_destination srd ON srd.id = p.schedule_route_destination_id\n" +
            " LEFT JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            " LEFT JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            " WHERE (p.delivered_at IS NOT NULL AND DATE(CONVERT_TZ(p.delivered_at, '+00:00', ?)) BETWEEN ? AND ?)\n" +
            " AND p.delivered_at > p.promise_delivery_date AND p.parcel_status IN (2, 3) \n" +
            " AND p.is_internal_parcel IS FALSE \n";

    private static final String QUERY_GET_PARCELS_ID_LIST= "SELECT\n" +
            "   pp.parcel_id AS id,\n" +
            "   p.terminal_origin_id,\n" +
            "   p.terminal_destiny_id,\n" +
            "   pt.id AS parcel_transhipment_id,\n" +
            "   IF((SELECT COUNT(bprc.id) FROM branchoffice_parcel_receiving_config bprc \n" +
            "       WHERE bprc.of_branchoffice_id = p.terminal_destiny_id\n" +
            "       AND bprc.receiving_branchoffice_id = ? AND bprc.status = 1) > 0, TRUE, FALSE) AS is_in_replacement_terminal\n" +
            " FROM parcels_packages pp\n" +
            " INNER JOIN parcels p ON p.id = pp.parcel_id\n" +
            " LEFT JOIN parcels_transhipments pt ON pt.parcel_id = p.id AND pt.parcel_package_id = pp.id\n" +
            " WHERE pp.id IN ";

    private static final String QUERY_GET_PACKAGE_DESTINY = "SELECT\n" +
            "   pp.id,\n" +
            "   pp.package_status,\n" +
            "   p.id AS parcel_id,\n" +
            "   p.terminal_destiny_id,\n" +
            "   pt.id AS parcel_transhipment_id,\n" +
            "   IF((SELECT COUNT(bprc.id) FROM branchoffice_parcel_receiving_config bprc \n" +
            "       WHERE bprc.of_branchoffice_id = p.terminal_destiny_id\n" +
            "       AND bprc.receiving_branchoffice_id = ? AND bprc.status = 1) > 0, TRUE, FALSE) AS is_in_replacement_terminal,\n" +
            "   b.receive_transhipments\n" +
            " FROM parcels_packages pp\n" +
            " LEFT JOIN parcels p ON p.id = pp.parcel_id\n" +
            " LEFT JOIN parcels_transhipments pt ON pt.parcel_id = p.id AND pt.parcel_package_id = pp.id\n" +
            " LEFT JOIN branchoffice b ON b.id = ?\n" +
            " WHERE pp.id = ?;";

    private static final String QUERY_SALES_REPORT_PACKAGES_MONTH = "SELECT pp.parcel_id, " +
        " COUNT(pp.parcel_id) AS packages, pp.shipping_type, pp.package_type_id, pt.name AS package_type, pp.package_price_id, " +
        " ppr.name_price AS package_range, SUM(pp.weight) AS weight, SUM(pp.height + pp.width + pp.length) AS volumen_lineal, SUM(pp.height * pp.width * pp.length) / 1000000 AS volumen,\n" +
        "SUM(pp.total_amount) as totalPaquete\n" +
        " FROM parcels_packages AS pp \n" +
        " INNER JOIN package_types AS pt ON pt.id = pp.package_type_id\n" +
        " INNER JOIN package_price AS ppr ON ppr.id = pp.package_price_id \n" +
        " WHERE pp.parcel_id = ?\n" +
        " GROUP BY pp.shipping_type, pp.package_type_id, pp.package_price_id";
    private static final String QUERY_GET_PACKAGE_TRACKING = "SELECT\n" +
            "   ppt.id,\n" +
            "   ppt.parcel_id,\n" +
            "   ppt.parcel_package_id,\n" +
            "   ppt.terminal_id,\n" +
            "   ppt.action\n" +
            " FROM parcels_packages_tracking ppt\n" +
            " LEFT JOIN parcels_packages pp ON pp.id = ppt.parcel_package_id\n" +
            " LEFT JOIN parcels p ON p.id = ppt.parcel_id\n" +
            " WHERE parcel_package_id = ?\n" +
            " AND ppt.action NOT IN (" +
            "'" + PARCELPACKAGETRACKING_STATUS.PRINTED.getValue() + "'," +
            "'" + PARCELPACKAGETRACKING_STATUS.INCIDENCE.getValue() + "'" +
            ")\n" +
            " AND pp.status = 1\n" +
            " ORDER BY id DESC LIMIT 1;";
    //QUERY web dev
    private static final String QUERY_SALES_REPORT_MONTH = "SELECT p.id, " +
        " p.branchoffice_id AS terminal_id, b.name AS terminal_name, b.prefix AS terminal_prefix, b.city_id AS terminal_city_id, c.name AS terminal_city, " +
        " p.shipment_type, p.customer_id, CONCAT(cc.first_name, ' ', cc.last_name) AS customer_name, p.sender_name, p.sender_last_name, \n" +
        " p.pays_sender, p.payment_condition, p.boarding_pass_id, p.parcel_tracking_code, p.waybill,\n" +
        " p.terminal_origin_id, ob.prefix AS terminal_origin_prefix, ob.city_id AS terminal_origin_city_id, oc.name AS terminal_origin_city, \n" +
        " p.terminal_destiny_id, db.prefix AS terminal_destiny_prefix, db.city_id AS terminal_destiny_city_id, dc.name AS terminal_destiny_city, \n" +
        " p.parcel_status, p.amount, p.discount, p.has_insurance, p.insurance_amount, p.extra_charges, p.iva, p.parcel_iva, p.total_amount, p.created_at\n" +
        " FROM parcels AS p \n" +
        " LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id)\n" +
        " LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
        " LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
        " LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
        " LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
        " LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
        " LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
        " WHERE p.created_at BETWEEN ? AND ? and cc.id != (select value from general_setting where field = 'internal_customer') ";

    private static final String QUERY_EXPENSE_CONCEPT_RETURN = "SELECT\n"
            + "	id\n"
            + "FROM\n"
            + "	expense_concept\n"
            + "WHERE\n"
            + "	name = ?";

    private static final String QUERY_PARCEL_BY_TRACKING_CODE = "SELECT \n" +
            "    a.*, \n" +
            "    CONCAT(b.prefix, a.parcel_tracking_code) AS parcel_code,\n" +
            "    b.prefix AS terminal_origin_prefix, \n" +
            "    b.name AS terminal_origin_name, \n" +
            "    b.address AS terminal_origin_address,\n" +
            "    c.prefix AS terminal_destiny_prefix, \n" +
            "    c.name AS terminal_destiny_name, \n" +
            "    c.address AS terminal_destiny_address,\n" +
            "    ca_sender.address AS sender_address,\n" +
            "    ca_addressee.address AS addressee_address,\n" +
            "    i.policy_number, \n" +
            "    i.insurance_carrier, \n" +
            "    SUM(t.total) AS paid,\n" +
            "    srd.travel_date,\n" +
            "    srd.arrival_date,\n" +
            "    srd.started_at,\n" +
            "    srd.finished_at,\n" +
            "    CONCAT(e.name, ' ', e.last_name) AS created_name,\n" +
            "    iv.document_id,\n" +
            "    iv.media_document_pdf_name,\n" +
            "    iv.media_document_xml_name,\n" +
            "    COUNT(debp.id) AS debt_payments_quantity,\n" +
            "    SUM(debp.amount) AS debt_payments,\n" +
            "    CONCAT(cu.first_name, ' ', cu.last_name) AS customer_full_name,\n" +
            "    t.cash_out_id,\n" +
            "    IF(\n" +
            "        (\n" +
            "            SELECT pic.show_prices_in_letter_porte \n" +
            "            FROM parcels_init_config pic\n" +
            "            INNER JOIN employee e2 ON e2.id = pic.employee_id\n" +
            "            INNER JOIN users u ON u.id = e2.user_id\n" +
            "            WHERE u.id = a.created_by\n" +
            "        ) = 0, \n" +
            "        TRUE, \n" +
            "        FALSE\n" +
            "    ) AS hide_costs\n" +
            "FROM parcels a\n" +
            "INNER JOIN parcels_packages pp ON pp.parcel_id = a.id\n" +
            "INNER JOIN branchoffice b ON b.id = a.terminal_origin_id\n" +
            "INNER JOIN branchoffice c ON c.id = a.terminal_destiny_id\n" +
            "LEFT JOIN schedule_route_destination srd ON srd.id = a.schedule_route_destination_id\n" +
            "LEFT JOIN employee e ON e.user_id = a.created_by\n" +
            "LEFT JOIN customer_addresses ca_sender ON ca_sender.id = a.sender_address_id\n" +
            "LEFT JOIN customer_addresses ca_addressee ON ca_addressee.id = a.addressee_address_id\n" +
            "LEFT JOIN insurances i ON i.id = a.insurance_id\n" +
            "LEFT JOIN tickets t ON t.parcel_id = a.id\n" +
            "LEFT JOIN invoice iv ON iv.id = a.invoice_id\n" +
            "LEFT JOIN debt_payment debp ON debp.parcel_id = a.id\n" +
            "LEFT JOIN customer cu ON cu.id = a.customer_id\n" +
            "WHERE %s\n" +
            "GROUP BY a.id, e.name, e.last_name\n" +
            "ORDER BY a.created_at DESC;\n";

    private static final  String QUERY_PARCEL_BY_TRACKING_CODE_GPP = "SELECT a.*, CONCAT(b.prefix, a.parcel_tracking_code) AS parcel_code,\n" +
            "b.prefix as terminal_origin_prefix, b.name as terminal_origin_name, b.address AS terminal_origin_address, \n" +
            "c.prefix as terminal_destiny_prefix, c.name as terminal_destiny_name, \n" +
            "ca_sender.address AS sender_address, \n" +
            "ca_addressee.address AS addressee_address, \n" +
            "i.policy_number, i.insurance_carrier, SUM(t.total) AS paid, \n" +
            "srd.travel_date, \n" +
            "srd.arrival_date, \n" +
            "srd.started_at, \n" +
            "srd.finished_at, \n" +
            "CONCAT(e.name, ' ', e.last_name) as created_name,\n" +
            "iv.document_id,\n" +
            "iv.media_document_pdf_name,\n" +
            "iv.media_document_xml_name,\n" +
            "COUNT(debp.id) AS debt_payments_quantity,\n" +
            "SUM(debp.amount) AS debt_payments, \n" +
            "t.cash_out_id\n" +
            "FROM parcels a \n" +
            "inner join branchoffice b on b.id = a.terminal_origin_id \n" +
            "inner join branchoffice c on c.id = a.terminal_destiny_id \n" +
            "left JOIN schedule_route_destination AS srd ON srd.id = a.schedule_route_destination_id \n" +
            "left join employee e on e.user_id = a.created_by \n" +
            "LEFT JOIN customer_addresses ca_sender ON ca_sender.id = a.sender_address_id \n" +
            "LEFT JOIN customer_addresses ca_addressee ON ca_addressee.id = a.addressee_address_id \n" +
            "left join insurances as i ON i.id = a.insurance_id \n" +
            "LEFT JOIN tickets AS t ON t.parcel_id = a.id \n" +
            "LEFT JOIN invoice AS iv ON iv.id = a.invoice_id\n" +
            "LEFT JOIN debt_payment debp ON debp.parcel_id = a.id\n" +
            "INNER JOIN parcels_prepaid_detail ppd ON ppd.parcel_id = a.id\n" +
            "where ppd.guiapp_code = ?\n" +
            "GROUP BY a.id, e.name, e.last_name\n" +
            "order by a.created_at;";

    private static final String QUERY_CHECK_TRACKING_CODE = "SELECT * FROM parcels WHERE id = ?";

    private static final  String QUERY_PARCELS_PACKAGES_BY_ID_PARCEL = "SELECT a.*, c.name as package_type_name, d.name_price AS package_price_name " +
            "FROM parcels_packages a \n" +
            "left join package_types as c ON c.id = a.package_type_id " +
            "left join package_price as d ON d.id = a.package_price_id " +
            "where a.parcel_id= ? and a.status = 1 order by a.created_at";

    private static final  String QUERY_PARCELS_PACKAGES_BY_ID_PARCEL_PRINT = "SELECT \n" +
            "\tcount(a.id) as count,  \n" +
            "    a.parcel_id,  \n" +
            "    a.shipping_type,  \n" +
            "    a.package_type_id,  \n" +
            "    a.package_price_id,  \n" +
            "    a.price as price,  \n" +
            "    a.package_price_km_id,  \n" +
            "    a.price_km as price_km,  \n" +
            "    SUM(a.amount) as amount,  \n" +
            "    SUM(a.discount) as discount, \n" +
            "    SUM(a.iva) as iva,  \n" +
            "    SUM(a.total_amount) as total_amount,  \n" +
            "    SUM(a.weight) as weight,  \n" +
            "    SUM(a.height) as height,  \n" +
            "    SUM(a.width) as width,  \n" +
            "    SUM(a.length) as length,  \n" +
            "    a.excess_price as excess_price,  \n" +
            "    SUM(a.excess_cost) as excess_cost, \n" +
            "    SUM(a.excess_discount) as excess_discount, \n" +
            "    a.pets_sizes_id,  \n" +
            "    a.contains,  \n" +
            "    c.name as package_type_name, \n" +
            "    d.name_price AS package_price_name, \n" +
            "    (SELECT count(id) FROM parcels_incidences where parcel_package_id = a.id) AS incidences   \n" +
            "FROM parcels_packages a  \n" +
            "left join package_types as c ON c.id = a.package_type_id  \n" +
            "left join package_price as d ON d.id = a.package_price_id\n" +
            "where a.parcel_id = ?\n" +
            "\tand a.status = 1  \n" +
            "group by a.parcel_id, a.contains, a.package_type_id, a.package_price_id, a.shipping_type, a.price, \n" +
            "a.package_price_km_id, a.price_km, a.excess_price, a.pets_sizes_id;";

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
            " WHERE a.parcel_package_id = ? ORDER BY a.created_at;";

    private static final String QUERY_CHECK_PENDANT_PACKAGES = "SELECT * FROM parcels_packages \n" +
            "WHERE parcel_id = ?\n" +
            "AND package_status IN (" + PARCEL_STATUS.DOCUMENTED.ordinal() + ", "+ PARCEL_STATUS.IN_TRANSIT.ordinal() +", "+ PARCEL_STATUS.ARRIVED.ordinal() + ");";

    private static final String QUERY_PARCEL_PACKAGES = "SELECT \n" +
            "   pp.*, \n" +
            "   pp.id AS parcel_package_id, \n" +
            "   p.waybill,\n" +
            "   pprice.name_price AS package_price_name\n" +
            "FROM parcels_packages pp\n" +
            "INNER JOIN package_price pprice ON pprice.id = pp.package_price_id\n" +
            "JOIN parcels AS p ON p.id=pp.parcel_id\n" +
            "WHERE pp.parcel_id = ? ";

    private static final String QUERY_PARCEL_PACKAGES_SUMMARY = "SELECT COUNT(pp.id) AS quantity, pp.package_type_id, pt.name FROM parcels_packages pp \n" +
            "LEFT JOIN package_types AS pt on pt.id = pp.package_type_id \n" +
            "WHERE pp.parcel_id = ? \n" +
            "GROUP BY pp.package_type_id";

    private static final String QUERY_CHECK_INCIDENTED_PACKAGES = QUERY_PARCEL_PACKAGES +
            "AND pp.package_status = " + PARCEL_STATUS.DELIVERED_WITH_INCIDENCES.ordinal() + ";";

    private  static  final String QUERY_PACKINGS_BY_ID ="SELECT * FROM packings WHERE id = ? LIMIT 1;";

    private static final String QUERY_GET_PARCEL_PACKAGE_BY_PARCEL_ID ="SELECT \n" +
            " id, waybill, customer_id, branchoffice_id, boarding_pass_id, is_customer, \n" +
            " total_packages, shipment_type, \n" +
            " sender_id, sender_name, sender_last_name, sender_phone, sender_email, sender_zip_code, sender_address_id, terminal_origin_id, \n" +
            " terminal_destiny_id, has_invoice, num_invoice, exchange_rate_id, \n" +
            " has_insurance, insurance_value, insurance_amount, amount, total_amount, has_multiple_addressee, payment_condition,\n" +
            " addressee_id, addressee_name, addressee_last_name, addressee_phone, addressee_email, addressee_zip_code, addressee_address_id, \n" +
            " pays_sender, parcel_status, status, created_at,\n" +
            " schedule_route_destination_id, purchase_origin, insurance_id, invoice_id, invoice_is_global, debt, canceled_by\n" +
            "FROM parcels\n" +
            "WHERE id = ?";
    // para que pase deploy
    private static final String QUERY_GET_PARCEL_CITY_SENDER = "SELECT \n" +
            "city_id \n" +
            "FROM customer_addresses AS ca \n" +
            "LEFT JOIN parcels AS p \n" +
            "ON ca.id = p.sender_address_id \n" +
            "WHERE ca.customer_id = ?;";

    private static final String QUERY_GET_PARCEL_CITY_ADDRESSEE = "SELECT \n" +
            "city_id \n" +
            "FROM customer_addresses AS ca \n" +
            "LEFT JOIN parcels AS p \n" +
            "ON ca.id = p.addressee_address_id \n" +
            "WHERE ca.customer_id = ?;";

    private static final String QUERY_GET_TERMINAL_CITY = "SELECT \n"+
            "city_id \n"+
            "FROM branchoffice\n"+
            "WHERE id = ?;";

    private static final String QUERY_GET_PETSSIZES = "SELECT * FROM pets_sizes where id = ? and status=1 ";

    private static final String QUERY_PARCEL_ADVANCED_SEARCH = "SELECT DISTINCT\n" +
            " p.id,\n" +
            " p.parcel_tracking_code,\n" +
            " p.sender_name,\n" +
            " p.sender_last_name,\n" +
            " p.addressee_name,\n" +
            " p.addressee_last_name,\n" +
            " IF(p.customer_id IS NULL, 'Público en general', CONCAT(cu.first_name, ' ', cu.last_name)) AS customer_name,\n" +
            " p.parcel_status,\n" +
            " p.status,\n" +
            " p.pays_sender,\n" +
            " bo.id AS terminal_origin_id, \n" +
            " bo.prefix AS terminal_origin_prefix, \n" +
            " bo.name AS terminal_origin_name, \n" +
            " bd.id AS terminal_destiny_id, \n" +
            " bd.prefix AS terminal_destiny_prefix, \n" +
            " bd.name AS terminal_destiny_name, \n" +
            " p.created_at\n" +
            " FROM parcels AS p\n" +
            " LEFT JOIN parcels_packages AS pp ON p.id = pp.parcel_id\n" +
            " LEFT JOIN branchoffice AS bo ON p.terminal_origin_id = bo.id\n" +
            " LEFT JOIN branchoffice AS bd ON p.terminal_destiny_id = bd.id\n" +
            " LEFT JOIN customer cu ON cu.id = p.customer_id\n" +
            " WHERE \n" +
            " p.pays_sender = ? \n" +
            " AND p.sender_name LIKE CONCAT('%', ?, '%') \n" +
            " AND p.sender_last_name LIKE CONCAT('%', ?, '%') \n" +
            " AND p.addressee_name LIKE CONCAT('%', ?, '%') \n" +
            " AND p.addressee_last_name LIKE CONCAT('%', ?, '%') \n" +
            " AND bo.id = ? \n" +
            " AND bd.id = ? \n" +
            " AND DATE(p.created_at) BETWEEN DATE(?) AND DATE(?) \n" +
            " AND p.parcel_status != ?;";

    private static final String QUERY_PARCEL_ADVANCED_SEARCH_WITH_CREATED_AT = "SELECT DISTINCT\n" +
            " p.id,\n" +
            " p.parcel_tracking_code,\n" +
            " p.sender_name,\n" +
            " p.sender_last_name,\n" +
            " p.addressee_name,\n" +
            " p.addressee_last_name,\n" +
            " p.parcel_status,\n" +
            " p.status,\n" +
            " p.pays_sender,\n" +
            " bo.id AS terminal_origin_id, \n" +
            " bo.prefix AS terminal_origin_prefix, \n" +
            " bo.name AS terminal_origin_name, \n" +
            " bd.id AS terminal_destiny_id, \n" +
            " bd.prefix AS terminal_destiny_prefix, \n" +
            " bd.name AS terminal_destiny_name, \n" +
            " p.created_at\n" +
            " FROM parcels AS p\n" +
            " LEFT JOIN parcels_packages AS pp ON p.id = pp.parcel_id\n" +
            " LEFT JOIN branchoffice AS bo ON p.terminal_origin_id = bo.id\n" +
            " LEFT JOIN branchoffice AS bd ON p.terminal_destiny_id = bd.id\n" +
            " WHERE \n" +
            " p.pays_sender = ? \n" +
            " AND p.sender_name LIKE CONCAT('%', ?, '%') \n" +
            " AND p.sender_last_name LIKE CONCAT('%', ?, '%') \n" +
            " AND p.addressee_name LIKE CONCAT('%', ?, '%') \n" +
            " AND p.addressee_last_name LIKE CONCAT('%', ?, '%') \n" +
            " AND bo.id = ? \n" +
            " AND bd.id = ? \n" +
            " AND date(p.created_at) = ? \n" +
            " AND p.parcel_status != ?;";

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

    private static final String QUERY_GET_ADDRESSEE_INFO_BY_PARCEL_ID = "SELECT addressee_name, addressee_last_name FROM parcels WHERE id = ?;";

    private static final String QUERY_SCANNING_PACKAGE_REPORT = "select s.schedule_route_id, s.terminal_id, bt.prefix AS terminal, sr.config_route_id, sr.config_schedule_id, CONCAT(bo.prefix,'-',bd.prefix, ' ', DATE_FORMAT(sr.travel_date,'%H:%i:%s') ) AS route,\n" +
            "sum(case when (sppt.status = 'loaded' ) then 1 else 0 end) AS send,\n" +
            "sum(case when (sppt.status = 'downloaded') then 1 else 0 end) received,\n" +
            "sr.code AS schedule_route_code, \n" +
            "(SELECT SUM(sppa.total_packages) FROM(\n" +
            "select DISTINCT (pa.id), pa.total_packages, spptt.shipment_id, sr.config_route_id, sr.config_schedule_id\n" +
            "      from (select id, config_route_id, config_schedule_id from schedule_route ssr where ssr.travel_date between ? and ?) AS sr\n" +
            "      INNER JOIN shipments AS ss ON ss.schedule_route_id = sr.id\n" +
            "      INNER JOIN shipments_parcel_package_tracking AS spptt ON spptt.shipment_id = ss.id\n" +
            "    INNER JOIN parcels AS pa ON pa.id = spptt.parcel_id\n" +
            "    {filterShipping}\n"+
            "    WHERE ss.terminal_id = ? and ss.shipment_type='load'  ) AS sppa WHERE sppa.config_route_id = sr.config_route_id AND sppa.config_schedule_id = sr.config_schedule_id) AS totals_send,\n" +
            "(SELECT COUNT(sppa.parcel_package_id) FROM(\n" +
            "select DISTINCT (spptt.parcel_package_id), ss.schedule_route_id, sr.config_route_id, sr.config_schedule_id\n" +
            "\t  from schedule_route AS sr\n" +
            "      INNER JOIN shipments AS ss ON ss.schedule_route_id = sr.id\n" +
            "      INNER JOIN shipments_parcel_package_tracking AS spptt ON spptt.shipment_id = ss.id\n" +
            "      INNER JOIN config_route AS crt ON crt.id = sr.config_route_id\n"+
            "      INNER JOIN parcels AS pa ON pa.id = spptt.parcel_id\n" +
            "      {filterShipping}\n"+
            "    WHERE ss.terminal_id != ? and crt.terminal_origin_id != ? and ss.shipment_type='load' and pa.terminal_destiny_id=? ) AS sppa WHERE sppa.config_route_id = sr.config_route_id AND sppa.config_schedule_id = sr.config_schedule_id ) AS totals_received\n" +
            "from schedule_route as sr\n" +
            "INNER JOIN shipments AS s ON s.schedule_route_id=sr.id\n" +
            "INNER JOIN ({trackingJoin}) as sppt ON sppt.shipment_id=s.id\n" +
            "INNER JOIN parcels AS p ON p.id = sppt.parcel_id\n" +
            "INNER JOIN config_route AS cr ON cr.id=sr.config_route_id\n" +
            "INNER JOIN branchoffice AS bt ON bt.id = s.terminal_id\n" +
            "INNER JOIN city AS ct ON ct.id = bt.city_id\n" +
            "INNER JOIN branchoffice AS bo ON bo.id = cr.terminal_origin_id\n" +
            "INNER JOIN city AS co ON co.id = bo.city_id\n" +
            "INNER JOIN branchoffice AS bd ON bd.id = cr.terminal_destiny_id\n" +
            "where sr.travel_date BETWEEN ? AND ?\n" +
            "AND s.terminal_id=?   ";

    private static final String QUERY_JOIN_SHIPMENT_TRACKING = "SELECT sppt.* \n" +
     "FROM shipments_parcel_package_tracking AS sppt\n" +
            "INNER JOIN parcels AS p ON p.id = sppt.parcel_id\n"+
     "INNER JOIN parcels_packages AS pp ON pp.parcel_id = p.id \n"+
     "WHERE pp.shipping_type = ? GROUP BY shipment_id, parcel_package_id";
    private static final String QUERY_INSURANCE_VALIDATION = "SELECT id FROM insurances WHERE ? BETWEEN init AND end AND status = 1;";

    private static final String QUERY_UNREGISTERED_PACKAGE_REPORT = "SELECT  pp.id,pp.created_at, p.customer_id, CONCAT(cu.first_name, ' ',cu.last_name ) AS full_name, p.waybill,p.parcel_tracking_code, \n"+
            "p.shipment_type, count(*) AS packages,pp.package_type_id , pt.name AS package_type_name,pp.package_price_id,ppr.name_price AS package_price_name,p.payment_condition ,  p.terminal_origin_id , b.name AS terminal_origin_name,p.terminal_destiny_id , db.name AS terminal_destiny_name,p.pays_sender, pp.package_status ,  p.parcel_status  , sr.schedule_status \n"+
            "   FROM parcels_packages AS pp \n"+
            "   LEFT JOIN parcels AS p ON pp.parcel_id = p.id \n"+
            "   LEFT JOIN customer AS cu ON cu.id = p.customer_id \n"+
            "   LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id) \n"+
            "   LEFT JOIN city AS c ON (c.id = b.city_id) \n"+
            "   LEFT JOIN package_types pt ON pt.id = pp.package_type_id \n"+
            "   LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id) \n"+
            "   LEFT JOIN city AS oc ON (oc.id = ob.city_id) \n"+
            "   LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id) \n"+
            "   LEFT JOIN city AS dc ON (dc.id = db.city_id) \n"+
            "   LEFT JOIN customer AS cc ON cc.id = p.customer_id \n"+
            "   LEFT JOIN shipments_parcel_package_tracking AS sppt ON sppt.parcel_id = p.id \n"+
            "   LEFT JOIN shipments AS s ON s.id = sppt.shipment_id \n"+
            "   LEFT JOIN schedule_route AS sr ON sr.id = s.schedule_route_id \n" +
            "   LEFT JOIN package_price AS ppr ON ppr.id = pp.package_price_id \n"+
            "   WHERE pp.package_status = "+ PACKAGE_STATUS.MERCHANDISE_NOT_RECEIVED.ordinal() +" AND pp.updated_at  BETWEEN ? AND ?";

    private static final String QUERY_STOCK_REPORT = "SELECT t.parcel_id, t.parcel_package_id, t.action, \n" +
            " t.terminal_id, b.name AS terminal_name, b.prefix AS terminal_prefix, b.city_id AS terminal_city_id, c.name AS terminal_city, \n" +
            " t.created_at AS last_tracking, p.parcel_tracking_code, p.shipment_type, \n" +
            " p.terminal_origin_id, ob.prefix AS terminal_origin_prefix, ob.city_id AS terminal_origin_city_id, oc.name AS terminal_origin_city, \n" +
            " p.terminal_destiny_id, db.prefix AS terminal_destiny_prefix, db.city_id AS terminal_destiny_city_id, dc.name AS terminal_destiny_city, \n" +
            " p.addressee_id, p.addressee_name, p.addressee_last_name, p.pays_sender, p.created_at, p.total_amount AS parcel_amount,\n" +
            " pp.package_code, pp.total_amount AS package_amount, pp.shipping_type, pp.package_type_id, pt.name AS package_type_name,\n" +
            " pp.package_status\n" +
            " FROM parcels_packages_tracking as t\n" +
            " JOIN parcels AS p ON (t.parcel_id = p.id)\n" +
            " JOIN parcels_packages AS pp ON (t.parcel_package_id = pp.id) AND pp.package_status IN (8, 9)\n" +
            " LEFT JOIN package_types AS pt ON (pt.id = pp.package_type_id)\n" +
            " LEFT JOIN branchoffice AS b ON (b.id = t.terminal_id)\n" +
            " LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
            " LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
            " LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
            " LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
            " LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
            " WHERE t.id IN \n" +
            "   (SELECT MAX(ppt.id) FROM parcels_packages_tracking ppt\n" +
            "   WHERE ppt.parcel_package_id IS NOT NULL AND (ppt.action!='printed' AND ppt.action!='closed') AND ppt.parcel_id IN \n" +
            "       (SELECT subp.id FROM parcels subp where subp.status < 2 AND subp.created_at BETWEEN ? AND ?) \n" +
            "   GROUP BY ppt.parcel_package_id)\n" +
            " AND t.action='arrived'";

    private static final String QUERY_TRANSIT_PACKAGE_REPORT =
            "      FROM parcels_packages AS pp\n" +
            "       LEFT JOIN parcels AS p ON p.id = pp.parcel_id  \n" +
            "       LEFT JOIN schedule_route_destination AS srd ON srd.id = p.schedule_route_destination_id\n"+
            "       LEFT JOIN schedule_route AS sr ON sr.id = srd.schedule_route_id\n"+
            "       LEFT JOIN parcels_packages_tracking AS ppt ON ppt.parcel_package_id = pp.id AND ppt.action != \"printed\"\n" +
            "       LEFT JOIN customer AS cu ON cu.id = p.customer_id\n" +
            "       LEFT JOIN branchoffice AS b ON (b.id = p.terminal_origin_id)\n" +
            "       LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
            "       LEFT JOIN package_types pt ON pt.id = pp.package_type_id\n" +
            "       LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
            "       LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
            "       LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
            "       LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
            "       LEFT JOIN branchoffice AS dbt ON dbt.id = ppt.terminal_id\n" +
            "       LEFT JOIN city AS dct ON dct.id = dbt.city_id\n" +
            "       WHERE p.created_at between ? AND  ? AND pp.package_status IN (0,1,5,6,8) ";
    private static final String QUERY_TRANSIT_PACKAGE_REPORT_FIELDS = "SELECT  p.id, p.terminal_origin_id ,dct.name AS location, b.name AS terminal_origin_name,p.waybill,p.parcel_tracking_code, pp.created_at, p.terminal_destiny_id , db.name , p.customer_id, CONCAT(cu.first_name, ' ',cu.last_name ) AS full_name,\n" +
            "IFNULL(\n" +
            "       (SELECT SUM(total_amount) from parcels_packages where p.pays_sender = 0 AND p.id = parcels_packages.parcel_id),\n" +
            " 0.0\n" +
            "             ) AS fxc,\n" +
            "             IFNULL(\n" +
            "       (SELECT SUM(total_amount) from parcels_packages where p.pays_sender = 1 AND p.id = parcels_packages.parcel_id),\n" +
            "0.0\n" +
            "             ) AS paid, p.shipment_type,pp.package_type_id , pt.name ,  pp.contains ,\n" +
            "(SELECT COUNT(*) from parcels_packages where pp.package_status IN (0,1,5,6,8) AND p.id = parcels_packages.parcel_id  ) AS total \n";
    private static final String QUERY_TRANSIT_PACKAGE_REPORT_COUNT = "SELECT COUNT(*) AS items FROM ( SELECT p.id \n";
    private static final String QUERY_TOTAL_TRANSIT_PACKAGE_REPORT = " select p.terminal_destiny_id,COUNT( DISTINCT p.id) AS total_parcels ,COUNT(pp.id) AS total_packages\n" +
            " from parcels_packages AS pp \n" +
            "\t  LEFT JOIN parcels AS p ON p.id = pp.parcel_id  \n" +
            "       LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
            "       LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
            "       LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
            " WHERE p.created_at between ? AND ? AND pp.package_status IN (0,1,5,6,8) ";

    private static final String QUERY_CANCEL_PARCEL_REPORT = " AND (p.parcel_status = 4 OR p.parcel_status = 6) ORDER BY p.created_at";

    private static final String PARCEL_CANCEL_REPORT_ORDER = "SELECT p.id, " +
            " p.branchoffice_id AS terminal_id, b.name AS terminal_name, b.prefix AS terminal_prefix, b.city_id AS terminal_city_id, c.name AS terminal_city, " +
            " p.shipment_type, p.customer_id, CONCAT(cc.first_name, ' ', cc.last_name) AS customer_name, p.sender_name, p.sender_last_name, \n" +
            " p.pays_sender, p.payment_condition, p.boarding_pass_id, p.parcel_tracking_code, p.waybill,\n" +
            " p.terminal_origin_id, ob.prefix AS terminal_origin_prefix, ob.city_id AS terminal_origin_city_id, oc.name AS terminal_origin_city, \n" +
            " p.terminal_destiny_id, db.prefix AS terminal_destiny_prefix, db.city_id AS terminal_destiny_city_id, dc.name AS terminal_destiny_city, pcr.cancel_type,\n" +
            " p.parcel_status, p.amount, p.discount, p.has_insurance, p.insurance_amount, p.extra_charges, p.iva, p.parcel_iva, p.total_amount, p.created_at, pcr.responsable, pcr.name AS reason, pcr.id AS reason_id, u.id AS cancel_by, u.name AS cancel_by_name, p.cancel_code, p.updated_at, p.canceled_at\n" +
            " FROM parcels AS p \n" +
            " INNER JOIN parcels_cancel_reasons AS pcr ON pcr.id = p.parcels_cancel_reason_id\n"+
            " LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id)\n" +
            " LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
            " LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
            " LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
            " LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
            " LEFT JOIN users AS u ON u.id = p.canceled_by\n"+
            " LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
            " LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
            " WHERE  ";

    private static final String PARCEL_CANCEL_REPORT_ORDER_TOTALS = "SELECT " +
            "  SUM(IF(p.pays_sender IS TRUE AND p.payment_condition = 'cash', p.total_amount, 0)) AS cash,\n" +
            "  SUM(IF(p.payment_condition = 'credit', p.total_amount, 0)) AS credit,\n" +
            "  SUM(IF(p.pays_sender IS FALSE AND p.payment_condition = 'cash', p.total_amount, 0)) AS fxc,\n" +
            "  COUNT(p.id) AS total\n" +
            " FROM parcels AS p \n" +
            " INNER JOIN parcels_cancel_reasons AS pcr ON pcr.id = p.parcels_cancel_reason_id\n"+
            " LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id)\n" +
            " LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
            " LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
            " LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
            " LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
            " LEFT JOIN users AS u ON u.id = p.canceled_by\n"+
            " LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
            " LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
            " WHERE  ";

    private static final String QUERY_SALES_REPORT = "SELECT DISTINCT p.id, " +
            " p.branchoffice_id AS terminal_id, b.name AS terminal_name, b.prefix AS terminal_prefix, b.city_id AS terminal_city_id, c.name AS terminal_city, " +
            " p.shipment_type, p.customer_id, CONCAT(cc.first_name, ' ', cc.last_name) AS customer_name, p.sender_name, p.sender_last_name, \n" +
            " p.pays_sender, p.payment_condition, p.boarding_pass_id, p.parcel_tracking_code, p.waybill,\n" +
            " p.terminal_origin_id, ob.prefix AS terminal_origin_prefix, ob.city_id AS terminal_origin_city_id, oc.name AS terminal_origin_city, \n" +
            " p.terminal_destiny_id, db.prefix AS terminal_destiny_prefix, db.city_id AS terminal_destiny_city_id, dc.name AS terminal_destiny_city,p.cancel_code, \n" +
            " p.parcel_status, p.amount, p.discount, p.has_insurance, p.insurance_amount, p.extra_charges, p.iva, p.parcel_iva, p.total_amount, p.created_at,\n" +
            " concat_ws(' ', p.addressee_name, p.addressee_last_name ) as adreesse," +
            " p.is_internal_parcel" +
            " FROM parcels AS p \n" +
            " LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id)\n" +
            " LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
            " INNER JOIN parcels_packages AS pp ON pp.parcel_id = p.id\n"+
            " LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
            " LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
            " LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
            " LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
            " LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
            " WHERE p.parcel_status != 4  AND p.created_at BETWEEN ? AND ?";

    private static final String QUERY_SALES_REPORT_V2 = "SELECT DISTINCT p.id, " +
            " p.branchoffice_id AS terminal_id, b.name AS terminal_name, b.prefix AS terminal_prefix, b.city_id AS terminal_city_id, c.name AS terminal_city, " +
            " p.shipment_type, p.customer_id, CONCAT(cc.first_name, ' ', cc.last_name) AS customer_name, p.sender_name, p.sender_last_name, \n" +
            " p.pays_sender, p.payment_condition, p.boarding_pass_id, p.parcel_tracking_code, p.waybill,\n" +
            " p.terminal_origin_id, ob.prefix AS terminal_origin_prefix, ob.city_id AS terminal_origin_city_id, oc.name AS terminal_origin_city, \n" +
            " p.terminal_destiny_id, db.prefix AS terminal_destiny_prefix, db.city_id AS terminal_destiny_city_id, dc.name AS terminal_destiny_city,p.cancel_code, \n" +
            " p.parcel_status, p.created_at,\n" +
            " concat_ws(' ', p.addressee_name, p.addressee_last_name ) as adreesse," +
            " p.is_internal_parcel," +
            " p.notes as notes, \n" +
            " p.total_packages as total_packages, \n" +
            " p.amount,\n" +
            " p.discount,\n" +
            " p.has_insurance,\n" +
            " p.insurance_amount,\n" +
            " p.extra_charges,\n" +
            " p.iva,\n" +
            " p.parcel_iva,\n" +
            " p.delivered_at,\n" +
            " {TOTAL_AMOUNT_DEF},\n" +
            " {GPP_PRICE_COL},\n" +
            " {PARTNER_INTEGRATION_COL_DEF},\n" +
            PARCEL_AMOUNT_COLUMN_DEF + " AS t_amount\n" +
            " FROM parcels AS p \n" +
            " LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id)\n" +
            " LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
            " INNER JOIN parcels_packages AS pp ON pp.parcel_id = p.id\n"+
            " LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
            " LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
            " LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
            " LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
            " LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
            " {GPP_PRICE_JOINS} \n" +
            " {WS_JOINS} \n" +
            " WHERE p.parcel_status != 4  AND p.created_at BETWEEN ? AND ? ";

    private static final String QUERY_SALES_REPORT_TRANSHIPMENTS = "SELECT DISTINCT p.id, " +
            " p.branchoffice_id AS terminal_id, b.name AS terminal_name, b.prefix AS terminal_prefix, b.city_id AS terminal_city_id, c.name AS terminal_city, " +
            " p.shipment_type, p.customer_id, CONCAT(cc.first_name, ' ', cc.last_name) AS customer_name, p.sender_name, p.sender_last_name, \n" +
            " p.pays_sender, p.payment_condition, p.boarding_pass_id, p.parcel_tracking_code, p.waybill,\n" +
            " p.terminal_origin_id, ob.prefix AS terminal_origin_prefix, ob.city_id AS terminal_origin_city_id, oc.name AS terminal_origin_city, \n" +
            " p.terminal_destiny_id, db.prefix AS terminal_destiny_prefix, db.city_id AS terminal_destiny_city_id, dc.name AS terminal_destiny_city,p.cancel_code, \n" +
            " p.parcel_status, p.amount, p.discount, p.has_insurance, p.insurance_amount, p.extra_charges, p.iva, p.parcel_iva, p.total_amount, p.created_at,\n" +
            " concat_ws(' ', p.addressee_name, p.addressee_last_name ) as adreesse," +
            " p.is_internal_parcel" +
            " FROM parcels AS p \n" +
            " LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id)\n" +
            " LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
            " INNER JOIN parcels_packages AS pp ON pp.parcel_id = p.id\n"+
            " INNER JOIN parcels_transhipments AS pt ON pt.parcel_id = p.id AND pt.parcel_package_id = pp.id\n"+
            " LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
            " LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
            " LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
            " LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
            " LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
            " WHERE p.parcel_status != 4  AND p.created_at BETWEEN ? AND ? ";

    private static final String SELLER_NAME_ID = " AND p.created_by = ? ";

    private static final String CUSTOMER_ID_REPORT = " AND p.customer_id = ? ";

    private static final String QUERY_SALES_REPORT_TOTALS = "SELECT \n" +
            " SUM(IF(t.pays_sender IS TRUE AND t.payment_condition = 'cash', t.total_amount, 0)) AS cash,\n" +
            " SUM(IF(t.payment_condition = 'credit', t.total_amount, 0)) AS credit,\n" +
            " SUM(IF(t.pays_sender IS FALSE AND t.payment_condition = 'cash', t.total_amount, 0)) AS fxc,\n" +
            " COUNT(t.id) AS total,\n" +
            " SUM(IF(t.pays_sender IS TRUE AND t.payment_condition = 'cash', t.total_packages, 0)) AS cash_packages,\n" +
            " SUM(IF(t.payment_condition = 'credit', t.total_packages, 0)) AS credit_packages,\n" +
            " SUM(IF(t.pays_sender IS FALSE AND t.payment_condition = 'cash', t.total_packages, 0)) AS fxc_packages,\n" +
            " SUM(IF(t.pays_sender IS TRUE AND t.payment_condition = 'cash', t.gpp_price, 0)) AS cash_gpp,\n" +
            " SUM(IF(t.payment_condition = 'credit', t.gpp_price, 0)) AS credit_gpp,\n" +
            " SUM(IF(t.pays_sender IS FALSE AND t.payment_condition = 'cash', t.gpp_price, 0)) AS fxc_gpp\n" +
            " FROM (";

    private static final String QUERY_SALES_REPORT_FECHA_PAGO = "SELECT  DISTINCT p.id, " +  //RANGO POR FECHA DE PAGO MOVDEV
        " p.branchoffice_id AS terminal_id, b.name AS terminal_name, b.prefix AS terminal_prefix, b.city_id AS terminal_city_id, c.name AS terminal_city, " +
        " p.shipment_type, p.customer_id, CONCAT(cc.first_name, ' ', cc.last_name) AS customer_name, p.sender_name, p.sender_last_name, \n" +
        " p.pays_sender, p.payment_condition, p.boarding_pass_id, p.parcel_tracking_code, p.waybill,\n" +
        " p.terminal_origin_id, ob.prefix AS terminal_origin_prefix, ob.city_id AS terminal_origin_city_id, oc.name AS terminal_origin_city, \n" +
        " p.terminal_destiny_id, db.prefix AS terminal_destiny_prefix, db.city_id AS terminal_destiny_city_id, dc.name AS terminal_destiny_city, \n" +
        " p.parcel_status, p.amount, p.discount, p.has_insurance, p.insurance_amount, p.extra_charges, p.iva, p.parcel_iva, p.total_amount,     pay.created_at , p.created_at  as Fdocumentacion\n" +
        " FROM parcels AS p \n" +
        " LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id)\n" +
        " LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
        " LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
        " LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
        " LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
        " LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
        " LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
        " LEFT JOIN payment  AS pay ON pay.parcel_id = p.id\n"+
        " WHERE pay.created_at BETWEEN ? AND ? and cc.id != (select value from general_setting where field = 'internal_customer') ";

    private static final String SALES_REPORT_ORDER_BYxFP = " ORDER BY pay.created_at"; //movdev

    private static final String QUERY_SALES_REPORT_PAYMENT_INFO_INGRESOS = "SELECT\n" +  ///MOVDEV
        "   p.payment_method_id,\n" +
        "   p.payment_method,\n" +
        "   p.amount,\n" +
        "   pm.name,\n" +
        "   pm.is_cash,\n" +
        "   pm.alias,\n" +
        "   pm.icon,\n" +
        "   p.created_at \n" +
        " FROM payment p \n" +
        " LEFT JOIN payment_method pm ON pm.id = p.payment_method_id\n" +
        " WHERE p.parcel_id = ?;";

    private static final String QUERY_CONTINGENCY_REPORT =  "FROM parcels_packages_tracking AS ppt \n"+
            "   LEFT JOIN parcels AS p ON ppt.parcel_id = p.id\n"+
            "   LEFT JOIN parcels_packages AS pp ON pp.parcel_id = p.id\n"+
            "   LEFT JOIN branchoffice AS b ON (b.id = ppt.terminal_id)\n"+
            "   LEFT JOIN city AS c ON (c.id = b.city_id)\n"+
            "   LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n"+
            "   LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n"+
            "   LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n"+
            "   LEFT JOIN city AS dc ON (dc.id = db.city_id)\n"+
            "   LEFT JOIN customer AS cc ON cc.id = p.customer_id\n"+
            "   LEFT JOIN shipments_parcel_package_tracking AS sppt ON sppt.parcel_package_id = ppt.parcel_package_id \n"+
            "   LEFT JOIN shipments AS ship ON ship.id = sppt.shipment_id \n"+
            "   LEFT JOIN travel_logs AS tl ON tl.schedule_route_id = ship.schedule_route_id"+
            "   WHERE ppt.is_contingency = 1 AND ppt.created_at  BETWEEN ? AND ? ";
    private static final String QUERY_CONTINGENCY_REPORT_FIELDS = "SELECT ppt.is_contingency,p.id, "+
            "ppt.terminal_id AS terminal_id, b.name AS terminal_name, p.created_at, p.waybill, p.parcel_tracking_code , pp.package_code,tl.travel_log_code , ppt.notes AS reason\n";
    private static final String QUERY_CONTINGENCY_REPORT_COUNT = "SELECT COUNT(*) AS items FROM ( SELECT p.id ";
    private static final String REPORT_ORDER_BY = " ORDER BY terminal_id AND terminal_city_id ";

    private static final String SALES_REPORT_ORDER_BY = " ORDER BY p.created_at";

    private static final String QUERY_SALES_REPORT_PACKAGES = "SELECT pp.parcel_id, " +
            " COUNT(pp.parcel_id) AS packages, pp.shipping_type, pp.package_type_id, pt.name AS package_type, pp.package_price_id, " +
            " ppr.name_price AS package_range, SUM(pp.weight) AS weight, SUM(pp.height + pp.width + pp.length) AS volumen_lineal, SUM(pp.height * pp.width * pp.length) / 1000000 AS volumen\n" +
            " FROM parcels_packages AS pp \n" +
            " INNER JOIN package_types AS pt ON pt.id = pp.package_type_id\n" +
            " INNER JOIN package_price AS ppr ON ppr.id = pp.package_price_id \n" +
            " WHERE pp.parcel_id = ? \n";

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
            " WHERE p.parcel_id = ?;";

    private static final String QUERY_GET_PACKAGES_BY_PARCEL_ID = "SELECT shipping_type, is_valid, need_auth, need_documentation, " +
            " parcels_deliveries_id, package_type_id, weight, height, " +
            " width, length, notes, schedule_route_destination_id, contains " +
            " FROM parcels_packages " +
            " WHERE parcel_id = ?;";
            
    private static final String QUERY_GET_PACKINGS_BY_PARCEL_ID = "SELECT \n" +
            " packing_id, quantity\n" +
            " FROM parcels_packings \n" +
            " WHERE parcel_id = ?;";

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

            private static final String QUERY_COBRANZA_REPORT_GEN ="SELECT \n"+
            "p.id as id,\n"+
            "p.total_packages as total_packages,\n"+
            "p.parcel_tracking_code as tracking_code,\n"+
            "p.total_amount as total_amount,\n"+
            "p.created_at as parcel_created_at,\n"+
            "pay.created_at as payment_created_at,\n"+
            "DATE_FORMAT(DATE_SUB(pay.created_at, INTERVAL 7 HOUR),'%m/%d') as dayFormat,\n"+
            "pay.payment_method_id as payment_method_id,\n"+
            "pay.amount as payment_amount,\n"+
            "p.terminal_destiny_id,\n"+
            "b.prefix \n"+
            "from parcels p\n"+
            "inner join payment pay\n"+
            "on pay.parcel_id = p.id\n"+
            "left join branchoffice b\n"+
            "on b.id = p.terminal_destiny_id\n"+
            "where pay.created_at between ? and ? \n"+
            "and p.parcel_status = 2\n"+
            "and p.pays_sender = 0\n"+
            "order by b.id";

    private static final String QUERY_SALES_REPORT_GEN = "SELECT p.id, " +
            " p.branchoffice_id AS terminal_id, b.name AS terminal_name, b.prefix AS terminal_prefix, b.city_id AS terminal_city_id, c.name AS terminal_city, " +
            " p.shipment_type, p.customer_id, CONCAT(cc.first_name, ' ', cc.last_name) AS customer_name, p.sender_name, p.sender_last_name, \n" +
            " p.pays_sender, p.payment_condition, p.boarding_pass_id, p.parcel_tracking_code, p.waybill,\n" +
            " p.terminal_origin_id, ob.prefix AS terminal_origin_prefix, ob.city_id AS terminal_origin_city_id, oc.name AS terminal_origin_city, \n" +
            " p.terminal_destiny_id, db.prefix AS terminal_destiny_prefix, db.city_id AS terminal_destiny_city_id, dc.name AS terminal_destiny_city,p.cancel_code, \n" +
            " p.parcel_status, p.amount, p.discount, p.has_insurance, p.insurance_amount, p.extra_charges, p.iva, p.parcel_iva, p.total_amount, p.created_at,  \n" +
            " DATE_FORMAT(DATE_SUB(p.created_at,INTERVAL 7 HOUR),'%m/%d') as dayFormat \n" +
            " FROM parcels AS p \n" +
            " LEFT JOIN branchoffice AS b ON (b.id = p.branchoffice_id)\n" +
            " LEFT JOIN city AS c ON (c.id = b.city_id)\n" +
            " INNER JOIN parcels_packages AS pp ON pp.parcel_id = p.id\n"+
            " LEFT JOIN branchoffice AS ob ON (ob.id = p.terminal_origin_id)\n" +
            " LEFT JOIN city AS oc ON (oc.id = ob.city_id)\n" +
            " LEFT JOIN branchoffice AS db ON (db.id = p.terminal_destiny_id)\n" +
            " LEFT JOIN city AS dc ON (dc.id = db.city_id)\n" +
            " LEFT JOIN customer AS cc ON cc.id = p.customer_id\n" +
            " WHERE p.parcel_status != 4  AND p.created_at BETWEEN ? AND ? AND cc.id != (select value from general_setting where field = 'internal_customer')  ";

    private static final String QUERY_SALES_REPORT_PACKAGES_GEN = "SELECT pp.parcel_id, " +
            " COUNT(pp.parcel_id) AS packages, pp.shipping_type, pp.package_type_id, pt.name AS package_type, pp.package_price_id, " +
            " ppr.name_price AS package_range, SUM(pp.weight) AS weight, SUM(pp.height + pp.width + pp.length) AS volumen_lineal, SUM(pp.height * pp.width * pp.length) / 1000000 AS volumen\n," +
            " pp.total_amount as  sumPackage \n" +
            " FROM parcels_packages AS pp \n" +
            " INNER JOIN package_types AS pt ON pt.id = pp.package_type_id\n" +
            " INNER JOIN package_price AS ppr ON ppr.id = pp.package_price_id \n" +
            " WHERE pp.parcel_id = ? \n";

    private static final String QUERY_SCANNING_PACKAGE_TRAVEL_DATE = "SELECT \n" +
            "             srd.travel_date\n" +
            "            FROM schedule_route AS sr\n" +
            "            INNER JOIN schedule_route_destination AS srd ON srd.schedule_route_id = sr.id\n" +
            "            LEFT JOIN config_destination AS cd ON srd.config_destination_id=cd.id \n" +
            "            WHERE sr.id = ? and srd.terminal_origin_id = ? AND\n" +
            "            (cd.order_destiny - cd.order_origin) = 1";
    private static final String QUERY_SCANNING_PACKAGE_ARRIVAL_DATE = "SELECT \n" +
            "             srd.arrival_date\n" +
            "            FROM schedule_route AS sr\n" +
            "            INNER JOIN schedule_route_destination AS srd ON srd.schedule_route_id = sr.id\n" +
            "            LEFT JOIN config_destination AS cd ON srd.config_destination_id=cd.id \n" +
            "            WHERE sr.id = ? and srd.terminal_destiny_id = ? AND\n" +
            "            (cd.order_destiny - cd.order_origin) = 1";

    private static final String QUERY_GET_VALID_CODES = "SELECT \n" +
            "pc.zip_code,\n" +
            "pc.id,\n" +
            "pc.status,\n" +
            "bf.prefix,\n" +
            "bf.name,\n" +
            "sb.name as suburb_name,\n" +
            "sb.suburb_type ,\n" +
            "pc.suburb_id ,\n" +
            "pc.branchoffice_id \n" +
            "FROM parcel_coverage pc\n" +
            "LEFT JOIN suburb sb ON pc.suburb_id = sb.id \n" +
            "LEFT JOIN branchoffice bf ON pc.branchoffice_id = bf.id \n" +
            " WHERE bf.status = 1 AND pc.status IN (1, 2) \n";

    private static final String QUERY_GET_BRANCHOFFICE_W_PARCEL_COVERAGE = "SELECT \n" +
            "   pc.branchoffice_id,\n" +
            "   b.name,\n" +
            "   b.prefix\n" +
            "FROM parcel_coverage pc \n" +
            "INNER JOIN branchoffice b ON b.id = pc.branchoffice_id\n" +
            "WHERE\n" +
            "   pc.status = 1\n" +
            "   AND b.status = 1\n" +
            "GROUP BY pc.branchoffice_id";

    private static final String QUERY_GET_ZIP_CODE_COVERAGE = "SELECT \n" +
            "pc.zip_code,\n" +
            "pc.id,\n" +
            "pc.status,\n" +
            "bf.prefix,\n" +
            "bf.name,\n" +
            "sb.name as suburb_name,\n" +
            "sb.suburb_type ,\n" +
            "pc.suburb_id ,\n" +
            "pc.branchoffice_id \n" +
            "FROM parcel_coverage pc\n" +
            "LEFT JOIN suburb sb ON pc.suburb_id = sb.id \n" +
            "LEFT JOIN branchoffice bf ON pc.branchoffice_id = bf.id \n" +
            "WHERE bf.status =1 AND pc.status = 1 and pc.zip_code = ?";

    private static final String QUERY_UPDATE_POSTAL_CODE = "update parcel_coverage set status = ? , updated_at = ? ,updated_by = ?  where id = ?";

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

    private static final String QUERY_PARCELS_PREPAID_REMAKED_BY_PARCEL_ID = "SELECT \n" +
            "   GROUP_CONCAT(ppd.guiapp_code) AS guides\n" +
            "FROM parcels_prepaid_detail ppd\n" +
            "INNER JOIN parcels_packages pp ON pp.id = ppd.cancel_parcel_package_id\n" +
            "INNER JOIN parcels p ON p.id = pp.parcel_id\n" +
            "WHERE p.id = ? \n" +
            "AND ppd.guiapp_code LIKE 'GPPR%';";

    private static final String GET_PARCEL_RAD_EAD_BY_PARCELID = "select * from parcels_rad_ead where parcel_id = ?";
    private static final String QUERY_DISTANCE_BY_TERMINALS = "select id, terminal_origin_id, terminal_destiny_id, travel_time, distance_km, status from package_terminals_distance where ((terminal_origin_id  = ? AND terminal_destiny_id = ?) OR (terminal_origin_id  = ? AND terminal_destiny_id = ?) ) AND status = 1;";
    private static final String INSERT_TRACKING_ON_REGISTER = "INSERT INTO parcels_packages_tracking  (parcel_id , parcel_package_id  , action , terminal_id , notes, created_by)  VALUES (?, ? , ? , ?, ? , ? );";
    private static final String GET_CODE_GUIAPP = "Select  pp.id, pp.tracking_code as parcels_tracking_code, pp.shipment_type, \n" +
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
            " where (pp.status!=4 and pp.parcel_status=1 and ppd.status!=4 and ppd.parcel_status=1 ) and ppd.expire_at > NOW() and ppd.guiapp_code=?";
    private static final String GET_PARCELS_PREPAID = "select  ppd.id id_ppd, pp.tracking_code, ppd.guiapp_code,\n" +
            "DATE(pp.created_at) as created_at,\n" +
            "DATE_FORMAT(pp.created_at, \"%H:%i\") as hour,\n" +
            "CONCAT(pkm.min_km, '-', pkm.max_km) as rango,\n" +
            "pprice.name_price as tarifa ,\n" +
            "CONCAT(pprice.min_weight , '-' , pprice.max_weight) as peso,\n" +
            "concat(pprice.min_linear_volume, '-' , pprice.min_linear_volume) as m3,\n" +
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
            "            WHERE pp.id = ? AND ppd.parcel_status = 0 AND pp.expire_at > NOW() order by ppd.id ASC ";

    private static final String QUERY_GET_CUSTOMERS_BY_USER_SELLER_ID = "SELECT id FROM customer WHERE id > 0";

    private static final String QUERY_ACCUMULATED_BY_ADVISER_REPORT = "SELECT\n" +
            "    u.id as adviser_id,\n" +
            "    COALESCE(u.name, 'Público en general') AS adviser,\n" +
            "    SUM(CASE WHEN combined.query_type = 'prepaid' AND combined.payment_condition = 'cash' THEN combined.total_amount ELSE 0 END) AS total_prepaid_cash,\n" +
            "    SUM(CASE WHEN combined.query_type = 'prepaid' AND combined.payment_condition = 'credit' THEN combined.total_amount ELSE 0 END) AS total_prepaid_credit,\n" +
            "    SUM(CASE WHEN combined.query_type = 'parcel' AND combined.payment_condition = 'cash' THEN combined.total_amount ELSE 0 END) AS total_parcel_cash,\n" +
            "    SUM(CASE WHEN combined.query_type = 'parcel' AND combined.payment_condition = 'credit' THEN combined.total_amount ELSE 0 END) AS total_parcel_credit,\n" +
            "    SUM(CASE WHEN combined.query_type IN ('prepaid', 'parcel') THEN combined.total_amount ELSE 0 END) AS total_total\n" +
            "FROM\n" +
            "    (\n" +
            "        SELECT\n" +
            "           CASE \n" +
            "              WHEN p.pays_sender = 0 \n" +
            "                AND cc.user_seller_id IS NULL \n" +
            "                AND cc.branchoffice_id IS NULL \n" +
            "                AND cc.parcel_type IS NULL \n" +
            "                AND cs.user_seller_id IS NOT NULL \n" +
            "                AND cs.branchoffice_id IS NOT NULL \n" +
            "                AND cs.parcel_type IS NOT NULL \n" +
            "              THEN p.sender_id \n" +
            "              ELSE p.customer_id \n" +
            "            END AS customer_id," +
            "            p.total_amount,\n" +
            "            p.payment_condition,\n" +
            "            'parcel' AS query_type\n" +
            "        FROM\n" +
            "            parcels AS p\n" +
            "        JOIN customer AS cc ON p.customer_id = cc.id\n" +
            "        JOIN customer AS cs ON p.sender_id = cs.id\n" +
            "        LEFT JOIN branchoffice AS b ON b.id = p.branchoffice_id\n" +
            "        WHERE\n" +
                        "{WHERE_PARCELS}\n" +
            "        \n" +
            "        UNION ALL\n" +
            "        \n" +
            "        SELECT\n" +
            "            pp.customer_id,\n" +
            "            pp.total_amount,\n" +
            "            pp.payment_condition,\n" +
            "            'prepaid' AS query_type\n" +
            "        FROM\n" +
            "            parcels_prepaid AS pp\n" +
            "        JOIN customer AS c ON pp.customer_id = c.id\n" +
            "        LEFT JOIN branchoffice AS b ON b.id = pp.branchoffice_id\n" +
            "        WHERE\n" +
                        "{WHERE_PARCELS_PREPAID}\n" +
            "    ) AS combined\n" +
            "LEFT JOIN\n" +
            "    customer AS c ON combined.customer_id = c.id\n" +
            "LEFT JOIN\n" +
            "    users AS u ON c.user_seller_id = u.id\n" +
            "GROUP BY\n" +
            "    u.id, COALESCE(u.name, 'Público en general')\n" +
            "ORDER BY\n" +
            "    total_total DESC";

    private static final String QUERY_ACCUMULATED_BY_ADVISER_REPORT_DETAIL = "SELECT\n" +
            "    c.id AS customer_id,\n" +
            "    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,\n" +
            "    COALESCE(b.prefix, 'Sin asignar') AS place,\n" +
            "    SUM(CASE WHEN combined.query_type = 'prepaid' AND combined.payment_condition = 'cash' THEN combined.total_amount ELSE 0 END) AS total_prepaid_cash,\n" +
            "    SUM(CASE WHEN combined.query_type = 'prepaid' AND combined.payment_condition = 'credit' THEN combined.total_amount ELSE 0 END) AS total_prepaid_credit,\n" +
            "    SUM(CASE WHEN combined.query_type = 'parcel' AND combined.payment_condition = 'cash' THEN combined.total_amount ELSE 0 END) AS total_parcel_cash,\n" +
            "    SUM(CASE WHEN combined.query_type = 'parcel' AND combined.payment_condition = 'credit' THEN combined.total_amount ELSE 0 END) AS total_parcel_credit,\n" +
            "    SUM(CASE WHEN combined.query_type IN ('prepaid', 'parcel') THEN combined.total_amount ELSE 0 END) AS total_total\n" +
            "FROM\n" +
            "    (\n" +
            "        SELECT\n" +
            "            CASE \n" +
            "                WHEN p.pays_sender = 0 \n" +
            "                     AND cc.user_seller_id IS NULL \n" +
            "                     AND cc.branchoffice_id IS NULL \n" +
            "                     AND cc.parcel_type IS NULL \n" +
            "                     AND cs.user_seller_id IS NOT NULL \n" +
            "                     AND cs.branchoffice_id IS NOT NULL \n" +
            "                     AND cs.parcel_type IS NOT NULL \n" +
            "                THEN p.sender_id \n" +
            "                ELSE p.customer_id \n" +
            "            END AS customer_id,\n" +
            "            p.total_amount,\n" +
            "            p.payment_condition,\n" +
            "            'parcel' AS query_type\n" +
            "        FROM\n" +
            "            parcels AS p\n" +
            "        JOIN customer AS cc ON p.customer_id = cc.id\n" +
            "        JOIN customer AS cs ON p.sender_id = cs.id\n" +
            "        LEFT JOIN branchoffice AS b ON b.id = p.branchoffice_id\n" +
            "        WHERE\n" +
                     "{WHERE_PARCELS}\n" +
            "        \n" +
            "        UNION ALL\n" +
            "        \n" +
            "        SELECT\n" +
            "            pp.customer_id,\n" +
            "            pp.total_amount,\n" +
            "            pp.payment_condition,\n" +
            "            'prepaid' AS query_type\n" +
            "        FROM\n" +
            "            parcels_prepaid AS pp\n" +
            "        JOIN customer AS c ON pp.customer_id = c.id\n" +
            "        LEFT JOIN branchoffice AS b ON b.id = pp.branchoffice_id\n" +
            "        WHERE\n" +
                     "{WHERE_PARCELS_PREPAID}\n" +
            "    ) AS combined\n" +
            "JOIN\n" +
            "    customer AS c ON combined.customer_id = c.id\n" +
            "LEFT JOIN\n" +
            "    branchoffice AS b ON c.branchoffice_id = b.id\n" +
            "GROUP BY\n" +
            "    c.id, CONCAT(c.first_name, ' ', c.last_name), COALESCE(b.prefix, 'Sin asignar')\n" +
            "ORDER BY\n" +
            "    total_total DESC";

    private static final String GET_AVAILABLE_PARCELS_PREPAID_SITE = "SELECT\n" +
            "    pp.id AS id_ppd,\n" +
            "    p.tracking_code,\n" +
            "    pp.guiapp_code,\n" +
            "    DATE(p.created_at) AS created_at,\n" +
            "    DATE_FORMAT(p.created_at, \"%H:%i\") AS hour,\n" +
            "    CONCAT(pkm.min_km, '-', pkm.max_km) AS rango,\n" +
            "    d.name_price AS tarifa,\n" +
            "    CONCAT(d.min_weight, '-', d.max_weight) AS peso,\n" +
            "    CONCAT(d.min_linear_volume, '-', d.max_linear_volume) AS m3,\n" +
            "    p.shipment_type,\n" +
            "    p.payment_condition,\n" +
            "    CONCAT(c.first_name, ' ', c.last_name) AS name,\n" +
            "    c.phone,\n" +
            "    ventaBranch.name AS sucursal_venta\n" +
            "FROM parcels_prepaid_detail AS pp\n" +
            "INNER JOIN parcels_prepaid AS p ON p.id = pp.parcel_prepaid_id\n" +
            "INNER JOIN pp_price_km AS pkm ON pkm.id = pp.price_km_id\n" +
            "INNER JOIN pp_price AS d ON d.id = pp.price_id\n" +
            "INNER JOIN customer AS c ON c.id = p.customer_id\n" +
            "LEFT JOIN branchoffice AS ventaBranch ON ventaBranch.id = p.branchoffice_id\n" +
            "WHERE p.customer_id = ? AND pp.status = 1 AND pp.parcel_status = 0 AND pp.expire_at > NOW()";

    private static final String QUERY_EXCLUDE_ZERO_AMOUNT = " AND p.total_amount > 0";
    private static final String QUERY_EXCLUDE_ZERO_AMOUNT_WITH_GPP_COST = " AND (p.total_amount > 0 OR (p_gpp.total_amount / p_gpp.total_count_guipp) > 0)";

    private static final String GET_PARCELS_PACKAGES_SCANNER_TRACKING_BY_PARCEL_ID = "SELECT\n" +
            "\tppst.*,\n" +
            "    p.parcel_tracking_code,\n" +
            "    pp.package_code,\n" +
            "    IF(ppst.action = 'load', tll.travel_log_code, tld.travel_log_code) AS travel_log_code,\n" +
            "    cr.name AS config_route_name,\n" +
            "    b.name AS branchoffice_name,\n" +
            "    b.prefix AS branchoffice_prefix,\n" +
            "    v.alias AS vehicle_alias,\n" +
            "    v.economic_number AS vehicle_economic_number,\n" +
            "    t.name AS trailer_name,\n" +
            "    t.economic_number AS trailer_economic_number,\n" +
            "    u.name AS user_name\n" +
            "FROM parcels_packages_scanner_tracking ppst\n" +
            "INNER JOIN parcels p ON p.id = ppst.parcel_id\n" +
            "LEFT JOIN parcels_packages pp ON pp.id = ppst.parcel_package_id\n" +
            "LEFT JOIN shipments s ON s.id = ppst.shipment_id\n" +
            "LEFT JOIN travel_logs tll ON tll.load_id = s.id\n" +
            "LEFT JOIN travel_logs tld ON tld.download_id = s.id\n" +
            "LEFT JOIN schedule_route sr ON sr.id = ppst.schedule_route_id\n" +
            "LEFT JOIN vehicle v ON v.id = sr.vehicle_id\n" +
            "LEFT JOIN config_route cr ON cr.id = sr.config_route_id\n" +
            "LEFT JOIN branchoffice b ON b.id = ppst.branchoffice_id\n" +
            "LEFT JOIN trailers t ON t.id = ppst.trailer_id\n" +
            "LEFT JOIN users u ON u.id = ppst.created_by\n" +
            "WHERE ppst.parcel_id = ?;\n";
}
