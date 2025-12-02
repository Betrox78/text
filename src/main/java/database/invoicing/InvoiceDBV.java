package database.invoicing;

import database.commons.DBVerticle;
import database.commons.ExportGlobal;
import database.commons.GenericQuery;
import database.invoicing.handlers.parcelInvoiceDBV.ParcelInvoice;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.client.WebClient;
import org.json.CDL;
import org.json.JSONArray;
import service.commons.MailVerticle;
import utils.UtilsDate;
import utils.UtilsMoney;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.concurrent.atomic.AtomicInteger;


import static service.commons.Constants.*;

public class InvoiceDBV extends DBVerticle {

    public static final String REGISTER = "InvoiceDBV.registerDocument";
    public static final String CONFIRM_REGISTER = "InvoiceDBV.confirmRegisterDocument";
    public static final String SERVICE_DETAIL = "InvoiceDBV.serviceDetail";
    public static final String REGISTER_GLOBAL = "InvoiceDBV.registerGlobal";
    public static final String PROCESS_BY_BILLING = "InvoiceDBV.processByBilling";
    public static final String GET_UNBILLED = "InvoiceDBV.getUnbilled";
    public static final String ASSOCIATE = "InvoiceDBV.associateDocument";
    public static final String DISASSOCIATE = "InvoiceDBV.disassociateDocument";
    public static final String DISASSOCIATE_GLOBAL = "InvoiceDBV.disassociateGlobalBillingDocument";
    private static String URI = "api.cfdi.allabordo.com";
    private static String URI_PARCEL = "ptx.cfdi.allabordo.com";
    private static Boolean VALIDATE_PERIOD_INVOICE = true;
    public static Boolean REGISTER_INVOICES = false;
    private static String INVOICE_PREFIX = "DEV";
    private static String INVOICE_PARCEL_PREFIX = "DEV-PTX";
    public static final String EXPORT_GLOBAL = "InvoiceDBV.exportGlobal";
    public static final String GET_INVOICE_CSV = "InvoiceDBV.getInvoiceCsv";
    public static String CUSTOMER_INVOICE_PREFIX ="D";
    public static final String REGISTER_TIMBOX_SITE = "InvoiceDBV.registerTimboxInvoice";
    public static final String REGISTER_MULTIPLE_INVOICE = "InvoiceDBV.registerMultipleInvoice";
    public static final String GET_TIMBOX_INFO = "InvoiceDBV.getTimboxInfo";
    public static final String UPDATE_INVOICE_STATUS = "InvoiceDBV.updateInvoiceStatus";
    public static final String UPDATE_MULTIPLE_INVOICE_STATUS = "InvoiceDBV.updateMultipleInvoiceStatus";
    public static final String UPDATE_INVOICE_GLOBAL = "InvoiceDBV.updateInvoiceGlobal";
    public static final String DELETE_INVOICE_WITH_ERROR = "InvoiceDBV.deleteInvoiceWithError";
    public static final String DELETE_INVOICE_MULTIPLE_WITH_ERROR = "InvoiceDBV.deleteInvoiceMultipleWithError";
    public static final String DELETE_CCP_WITH_ERROR = "InvoiceDBV.deleteCCPWithError";
    public static final String UPDATE_CCP_STATUS = "InvoiceDBV.updateCCPStatus";
    public static final String GET_GLOBAL_INVOICE_XML = "InvoiceDBV.getGlobalInvoiceXML";
    public static final String GET_CUSTOMER_SERVICES = "InvoiceDBV.getCustomerServices";
    public static final String SEARCH_ADVANCED_CFDI = "InvoiceDBV.searchAdvancedCFDI";
    public static final String GET_INVOICE_XML = "InvoiceDBV.getInvoiceXML";
    public static final String GET_INVOICES_FOR_PAYMENT_COMPLEMENT = "InvoiceDBV.getInvoicesForPaymentComplement";
    public static final String GET_SERVICES_BY_INVOICE = "InvoiceDBV.getServicesByInvoice";
    public static final String ASSIGN_CUSTOMER_BILLING_INFO = "InvoiceDBV.assignCustomerBillingInfo";
    public static final String PG_SERIE = "PG";

    private static final Integer MAX_LIMIT = 30;

    public static final List<String> COURIER_RANGES = Arrays.asList("RS", "R1", "R2", "R3");
    ParcelInvoice parcelInvoice;

    public enum InvoiceInstance {
        GENERAL(1, "contpaq_id", "contpaq_status"),
        PARCEL(2, "contpaq_parcel_id", "contpaq_parcel_status");

        private Integer id;
        private String fieldId;
        private String fieldStatus;

        InvoiceInstance(Integer id, String fieldId, String fieldStatus) {
            this.id = id;
            this.fieldId = fieldId;
            this.fieldStatus = fieldStatus;
        }

        public static InvoiceInstance getInstanceById(Integer id) {
            switch (id) {
                case 1:
                    return GENERAL;
                case 2:
                    return PARCEL;
            }
            return  null;
        }

        public String getPrefix() {
            switch (this) {
                case GENERAL:
                    return INVOICE_PREFIX;
                case PARCEL:
                    return INVOICE_PARCEL_PREFIX;
            }
            return INVOICE_PREFIX;
        }

        public static InvoiceInstance getInstanceByService(ServiceTypes type) {
            if (type.equals(ServiceTypes.PARCEL)) {
                return PARCEL;
            }
            return  GENERAL;
        }

        public static String getHostByService(ServiceTypes type) {
            if (type.equals(ServiceTypes.PARCEL)) {
                return URI_PARCEL;
            }
            return  URI;
        }

        public String getFieldId() {
            return fieldId;
        }

        public String getFieldStatus() {
            return fieldStatus;
        }

        public Integer getId() {
            return id;
        }
    }

    private enum ServicePaymentMethod {
        CASH("cash", "01"),
        CARD("card", "04"),
        CHECK("check", "02"),
        TRANSFER("transfer", "03"),
        DEBIT("debit", "28"),
        DEPOSIT("deposit", "01"),
        CODI("codi", "03"); // TODO: No esta en el catalogo del SAT


        private String value;
        private String code;

        ServicePaymentMethod(String value, String code) {
            this.value = value;
            this.code = code;
        }

        private static ServicePaymentMethod getCodeByValue(String condition) {
            switch (condition) {
                case "card":
                    return CARD;
                case "transfer":
                    return TRANSFER;
                case "check":
                    return CHECK;
                case "debit":
                    return DEBIT;
                case "cash":
                default:
                    return CASH;
            }
        }

        String getCode() {
            return this.code;
        }

        String getValue() {
            return this.value;
        }
    }

    private enum GlobalRanges {
        RANGE1(1, 1, 10, UtilsDate.getLocalDate(), UtilsDate.getLocalDate()),
        RANGE2(2, 11, 20, UtilsDate.getLocalDate(), UtilsDate.getLocalDate()),
        RANGE3(3, 21, 30, UtilsDate.getLocalDate(), UtilsDate.getLocalDate()),
        DEFAULT(4, 1, 30, UtilsDate.getLocalDate(), UtilsDate.getLocalDate());

        private Integer id;
        private Integer initDay;
        private Integer endDay;
        private Date initDate;
        private Date endDate;

        GlobalRanges(Integer id, Integer initDay, Integer endDay, Date initDate, Date endDate) {
            this.id = id;
            this.initDay = initDay;
            this.endDay = endDay;
            this.initDate = initDate;
            this.endDate = endDate;
        }

        private static GlobalRanges getRangeById(Integer id) {
            switch (id) {
                case 1:
                    return RANGE1;
                case 2:
                    return RANGE2;
                case 3:
                    return RANGE3;
                default:
                    return DEFAULT;
            }
        }

        Date getInitDate() {
            return this.initDate;
        }

        Date getEndDate() {
            return this.endDate;
        }

        GlobalRanges setDates(Date date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.DAY_OF_MONTH, this.initDay);
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            this.initDate = UtilsDate.toClientDate(calendar.getTime());
            if(this.id.equals(1) || this.id.equals(2)) {
                calendar.set(Calendar.DAY_OF_MONTH, this.endDay);
            } else{
                calendar.add(Calendar.MONTH, 1);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.DATE, -1);
            }
            calendar.set(Calendar.HOUR, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            this.endDate = UtilsDate.toClientDate(calendar.getTime());
            return this;
        }

        Integer getId() {
            return this.id;
        }
    }

    private enum ServiceStatus {
        PENDING("pending"),
        PROGRESS("progress"),
        EXPIRED("expired"),
        ERROR("error"),
        DONE("done"),
        NOT_REQUIRED("not_required"),
        NO_ELEGIBLE("no_elegible");
        private String value;

        ServiceStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum ServiceTypes {
        BOARDING_PASS('B',"boarding_pass", "boarding_pass", "Boletos de autobuses",
                "SELECT bp.id, bp.amount, bp.discount, bp.iva, bp.total_amount, bp.invoice_id, bp.travel_return_date, bp.travel_date, bp.payment_condition, bp.invoice_is_global, t.created_at " +
                        "FROM boarding_pass bp LEFT JOIN tickets t ON t.boarding_pass_id = bp.id WHERE reservation_code = ?;",
                "boarding_pass_id", "reservation_code", "2", "contpaq_status","42017","(sv.status = 1 AND sv.boardingpass_status <> 0 AND sv.boardingpass_status <> 4)",
                "created_at", "travel_date", 0.0),
        RENTAL('R', "rental", "rental", "Renta de vans",
                "SELECT id, amount, discount, iva, total_amount, invoice_id, delivered_at, payment_condition, invoice_is_global " +
                        "FROM rental WHERE reservation_code = ?;",
                "rental_id", "reservation_code", "1", "contpaq_status","42017",
                "(sv.payment_status = '1' AND sv.status = 1)", "delivered_at", 0.0),
        PARCEL('G', "parcel","parcels", "Envío de paqueteria", QUERY_GET_PARCEL_INFO_FOR_INVOICE,
                "parcel_id", "parcel_tracking_code", "100", "contpaq_parcel_status", "5",
                "(sv.parcel_status != 4 AND (sv.pays_sender = 1 OR sv.parcel_status IN (2, 3)))",
                "created_at", 0.0),
        GUIA_PP('P', "guia_pp","parcels_prepaid", "Guias Prepagadas", QUERY_GET_PARCELS_PREPAID_INFO_FOR_INVOICE,
                "parcel_prepaid_id", "tracking_code", "69", "contpaq_parcel_status", "5",
                "sv.parcel_status != 4",
                "created_at", 0.0),
        PREPAID_BOARDING_PASS("BP","prepaid", "prepaid_package_travel", "Paquete de boletos prepago",
                              "SELECT ppt.id, ppt.amount, ppt.iva, 0 as discount, ppt.total_amount, ppt.invoice_id, ppt.invoice_is_global, ppt.payment_condition, t.created_at " +
                              "FROM prepaid_package_travel ppt LEFT JOIN tickets t ON t.prepaid_travel_id = ppt.id WHERE ppt.reservation_code = ?;",
                              "prepaid_travel_id", "reservation_code", "32", "contpaq_status","42017","(sv.status = 1 AND sv.boardingpass_status <> 0 AND sv.boardingpass_status <> 4)",
                              "created_at", 0.0);

        private char tag;
        private String tagS;
        private String table;
        private String type;
        private String name;
        private String query;
        private String reference;
        private String code;
        private String serviceCode;
        private String customerContpaqStatus;
        private String conceptCode;
        private String paymentValidation;
        private Double ivaWithHeldPercent;
        private String initValidAt;
        private String altInitValidAt = "undefined";

        ServiceTypes(char tag, String type, String table, String name, String query, String reference, String code, String serviceCode, String customerContpaqStatus, String conceptCode, String paymentValidation, String initValidAt, Double ivaWithHeldPercent) {
            this.tag = tag;
            this.type = type;
            this.table = table;
            this.name = name;
            this.query = query;
            this.reference = reference;
            this.code = code;
            this.serviceCode = serviceCode;
            this.customerContpaqStatus = customerContpaqStatus;
            this.ivaWithHeldPercent = ivaWithHeldPercent;
            this.conceptCode = conceptCode;
            this.paymentValidation = paymentValidation;
            this.initValidAt = initValidAt;
        }

        ServiceTypes(char tag, String type, String table, String name, String query, String reference, String code, String serviceCode, String customerContpaqStatus, String conceptCode, String paymentValidation, String initValidAt, String altInitValidAt, Double ivaWithHeldPercent) {
            this.tag = tag;
            this.type = type;
            this.table = table;
            this.name = name;
            this.query = query;
            this.reference = reference;
            this.code = code;
            this.serviceCode = serviceCode;
            this.customerContpaqStatus = customerContpaqStatus;
            this.ivaWithHeldPercent = ivaWithHeldPercent;
            this.conceptCode = conceptCode;
            this.paymentValidation = paymentValidation;
            this.initValidAt = initValidAt;
            this.altInitValidAt = altInitValidAt;
        }

        ServiceTypes(String tag, String type, String table, String name, String query, String reference, String code, String serviceCode, String customerContpaqStatus, String conceptCode, String paymentValidation, String initValidAt, Double ivaWithHeldPercent) {
            this.tagS = tag;
            this.type = type;
            this.table = table;
            this.name = name;
            this.query = query;
            this.reference = reference;
            this.code = code;
            this.serviceCode = serviceCode;
            this.customerContpaqStatus = customerContpaqStatus;
            this.conceptCode = conceptCode;
            this.paymentValidation = paymentValidation;
            this.initValidAt = initValidAt;
            this.ivaWithHeldPercent = ivaWithHeldPercent;
        }

        public static ServiceTypes getTypeByServiceName(String type) {
            switch (type) {
                case "boarding_pass":
                    return BOARDING_PASS;
                case "rental":
                    return RENTAL;
                case "parcel":
                    return PARCEL;
                case "guia_pp":
                    return GUIA_PP;
                case "prepaid":
                    return PREPAID_BOARDING_PASS;
            }
            return null;
        }

        public static ServiceTypes getTypeByServiceTag(char tag, String type) {
            switch (tag) {
                case 'B':
                    return BOARDING_PASS;
                case 'R':
                    return RENTAL;
                case 'P':
                    return GUIA_PP;
                case 'G':
                    return PARCEL;
            }
            return null;
        }

        public static ServiceTypes getTypeByReservationCode(String code, String type) {
            if (code.substring(0, 2).equals("BP")) {
                return PREPAID_BOARDING_PASS;
            } else if (code.substring(0, 1).equals("B")) {
                return BOARDING_PASS;
            } else if (code.substring(0, 1).equals("G")) {
                return PARCEL;
            } else if (code.substring(0, 2).equals("PP")) {
                return GUIA_PP;
            } else {
                return RENTAL;
            }
        }

        public static ServiceTypes getTypeByReservationCode(String code) {
            if (code.substring(0, 2).equals("BP")) {
                return PREPAID_BOARDING_PASS;
            } else if (code.substring(0, 1).equals("B")) {
                return BOARDING_PASS;
            } else if (code.substring(0, 1).equals("G")) {
                return PARCEL;
            } else if (code.substring(0, 2).equals("PP")) {
                return GUIA_PP;
            } else {
                return RENTAL;
            }
        }

        public static ServiceTypes getTypeByServiceTag(char tag) {
            switch (tag) {
                case 'B':
                    return BOARDING_PASS;
                case 'R':
                    return RENTAL;
                case 'G':
                    return PARCEL;
            }
            return null;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public char getTag() {
            return tag;
        }

        public String getTable() {
            return table;
        }

        public String getQuery() {
            return query;
        }

        public String getReference() {
            return reference;
        }

        public String getCode() {
            return code;
        }

        public Double getIvaWithHeldPercent() {
            return ivaWithHeldPercent;
        }

        public String getServiceCode() {
            return serviceCode;
        }
        public String getCustomerContpaqStatus() { return customerContpaqStatus; }
        public String getConceptCode() {
            return conceptCode;
        }
        public String getPaymentValidation() {
            return paymentValidation;
        }

        public String getInitValidAt() {
            if (!VALIDATE_PERIOD_INVOICE) {
                return "created_at";
            }
            return initValidAt;
        }

        public String getAltInitValidAt() {
            return altInitValidAt;
        }
    }

    public enum ServiceSeries {
        GUIA_PP(ServiceTypes.GUIA_PP, "GPP-CO", "GPP-CR", "G-GPP"),
        PARCEL (ServiceTypes.PARCEL,   "GC-CO",  "GC-CR",  "G-GC");

        private final ServiceTypes serviceType;
        private final String cashSeries;
        private final String creditSeries;
        private final String globalSeries;

        ServiceSeries(ServiceTypes serviceType, String cashSeries, String creditSeries, String globalSeries) {
            this.serviceType   = serviceType;
            this.cashSeries    = cashSeries;
            this.creditSeries  = creditSeries;
            this.globalSeries  = globalSeries;
        }

        public static String resolve(ServiceTypes st, String paymentCondition) {
            String key = paymentCondition == null ? "" : paymentCondition.toLowerCase();

            for (ServiceSeries ss : values()) {
                if (ss.serviceType == st) {
                    switch (key) {
                        case "cash":   return ss.cashSeries;
                        case "credit": return ss.creditSeries;
                        case "global": return ss.globalSeries;
                        default:
                            throw new IllegalArgumentException("Payment condition not supported: " + paymentCondition);
                    }
                }
            }
            return null;
        }
    }

    public enum LegalPerson {
        FISICA("fisico", 13, 0.0),
        MORAL("moral", 12, 0.04);

        private final String name;
        private final int rfcLength;
        private final Double ivaWithheldPercent;

        LegalPerson(String name, int rfcLength, Double ivaWithheldPercent) {
            this.name = name;
            this.rfcLength = rfcLength;
            this.ivaWithheldPercent = ivaWithheldPercent;
        }

        public String getName() {
            return name;
        }

        public int getRfcLength() {
            return rfcLength;
        }

        public Double getIvaWithheldPercent() {
            return ivaWithheldPercent;
        }

        public static LegalPerson fromRfc(String rfc) {
            if (rfc == null) {
                return null;
            } else if (rfc.length() == 13) {
                return FISICA;
            } else if (rfc.length() == 12) {
                return MORAL;
            } else {
                return null;
            }
        }

        public static LegalPerson fromLegalPerson(String legalPerson) {
            if (legalPerson == null) {
                return null;
            }
            for (LegalPerson lp : LegalPerson.values()) {
                if (lp.getName().equalsIgnoreCase(legalPerson)) {
                    return lp;
                }
            }
            return null;
        }

        public boolean isMoral() {
            return this == MORAL;
        }
    }

    @Override
    public String getTableName() {
        return "invoice";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        JsonObject _config = config();
        URI = _config.getString("invoice_server_host");
        URI_PARCEL = _config.getString("invoice_parcel_server_host");
        VALIDATE_PERIOD_INVOICE = _config.getBoolean("validate_period_invoice", true);
        REGISTER_INVOICES = _config.getBoolean("register_invoices", false);
        INVOICE_PREFIX = _config.getString("invoice_prefix", "DEV");
        INVOICE_PARCEL_PREFIX = _config.getString("invoice_parcel_prefix", "DEVPTX");
        CUSTOMER_INVOICE_PREFIX = _config.getString("customer_invoice_prefix", "D");
        parcelInvoice = new ParcelInvoice(this, _config);
    }

    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case SERVICE_DETAIL:
                this.serviceDetail(message);
                break;
            case REGISTER:
                this.register(message);
                break;
            case CONFIRM_REGISTER:
                this.confirmRegisterInvoice(message);
                break;
            case REGISTER_GLOBAL:
                this.registerGlobal(message);
                break;
            case PROCESS_BY_BILLING:
                this.processPendingInvoicesByBilling(message);
                break;
            case GET_UNBILLED:
                this.getUnbilled(message);
                break;
            case ASSOCIATE:
                this.associateDocument(message);
                break;
            case DISASSOCIATE:
                this.disassociateDocument(message);
                break;
            case DISASSOCIATE_GLOBAL:
                this.disassociateGlobalBillingDocument(message);
                break;
            case EXPORT_GLOBAL:
                this.exportGlobal(message);
                break;
            case GET_INVOICE_CSV:
                this.getInvoiceCsv(message);
                break;
            case ParcelInvoice.ACTION:
                parcelInvoice.handle(message);
                break;
            case ParcelInvoice.ACTION_INGRESO:
                parcelInvoice.handleIngreso(message);
                break;
            case ParcelInvoice.ACTION_CCP:
                parcelInvoice.handleCCP(message);
                break;
            case ParcelInvoice.ACTION_INGRESO_GLOBAL:
                parcelInvoice.handleIngresoGlobal(message);
                break;
            case REGISTER_TIMBOX_SITE:
                this.registerTimboxInvoice(message);
                break;
            case REGISTER_MULTIPLE_INVOICE:
                this.registerMultipleInvoice(message);
                break;
            case GET_TIMBOX_INFO:
                this.getTimboxInfo(message);
                break;
            case UPDATE_INVOICE_STATUS:
                this.updateInvoiceStatus(message);
                break;
            case UPDATE_MULTIPLE_INVOICE_STATUS:
                this.updateMultipleInvoiceStatus(message);
                break;
            case UPDATE_INVOICE_GLOBAL:
                this.updateInvoiceGlobal(message);
                break;
            case DELETE_INVOICE_WITH_ERROR:
                this.deleteInvoiceWithError(message);
                break;
            case DELETE_INVOICE_MULTIPLE_WITH_ERROR:
                this.deleteInvoiceMultipleWithError(message);
                break;
            case UPDATE_CCP_STATUS:
                this.updateCCPStatus(message);
                break;
            case DELETE_CCP_WITH_ERROR:
                this.deleteCCPWithError(message);
                break;
            case GET_GLOBAL_INVOICE_XML:
                this.getGlobalInvoiceXML(message);
                break;
            case GET_CUSTOMER_SERVICES:
                this.getCustomerServices(message);
                break;
            case GET_INVOICE_XML:
                this.getInvoiceXML(message);
                break;
            case GET_INVOICES_FOR_PAYMENT_COMPLEMENT:
                this.getInvoicesForPaymentComplement(message);
                break;
            case SEARCH_ADVANCED_CFDI:
                this.searchAdvancedCFDI(message);
                break;
            case GET_SERVICES_BY_INVOICE:
                this.getServicesByInvoice(message);
                break;
            case ASSIGN_CUSTOMER_BILLING_INFO:
                this.assignCustomerBillingInfo(message);
                break;
        }
    }

    /**
     * Get the type detail to invoice, it must validate if it is on date ranges
     * according to billing policies
     *
     * @param message Event bus message
     */
    private void serviceDetail(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String code = body.getString("code").toUpperCase();
            String serviceType = body.getString("type");

            doServiceDetail(code, serviceType, null).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    message.reply(reply.result());

                } catch (Exception e) {
                    e.printStackTrace();
                    reportQueryError(message, e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private Future<JsonObject> doServiceDetail(String code, String serviceType, JsonObject billingInfo) {
        Future<JsonObject> future = Future.future();
        try {
            Date today = UtilsDate.getLocalDate();
            ServiceTypes type = ServiceTypes.getTypeByReservationCode(code, serviceType);

            if (type == null) {
                throw new Exception("Service: invalid code");
            }

            this.dbClient.queryWithParams(type.getQuery(), new JsonArray().add(code), replyService -> {
                try {
                    if (replyService.failed()) {
                        throw new Exception(replyService.cause());
                    }

                    List<JsonObject> services = replyService.result().getRows();
                    if (services.isEmpty()) {
                        throw new Exception("Service: not found");
                    }

                    JsonObject service = services.get(0);

                    getServiceExtraParams(serviceType, service).setHandler(extraParamsR -> {
                        JsonObject extraParams = extraParamsR.result();
                        service.put("extra_data", extraParams);
                        service.put("reservation_code", code);
                        Integer invoiceId = service.getInteger("invoice_id");
                        String queryByService = getIvaQueryForService(serviceType);

                        this.dbClient.queryWithParams(queryByService, new JsonArray().add(service.getInteger("id")), replyIvaBranch -> {
                            try {
                                Double ivaOrNull = replyIvaBranch.result().getRows().get(0).getDouble("iva");
                                service.put("iva_percent", (ivaOrNull != null) ? ivaOrNull : 0.16);
                                JsonObject invoiceConditions = getInvoiceConditions(serviceType, service, extraParams, billingInfo);

                                if (invoiceId == null) {
                                    JsonArray paramsPayments = new JsonArray().add(service.getInteger("id"));
                                    String queryPayments = QUERY_SERVICE_GET_PAYMENTS.replace("{SERVICE_ID}", type.getReference());

                                    this.dbClient.queryWithParams(queryPayments, paramsPayments, replyPayments -> {
                                        try {
                                            if (replyPayments.failed()) {
                                                throw new Exception(replyPayments.cause());
                                            }

                                            List<JsonObject> payments = replyPayments.result().getRows();
                                            Optional<JsonObject> greaterPayment = payments.stream().reduce((item, last) ->
                                                    item.getInteger("total") > last.getDouble("total") ?
                                                            item : last);

                                            double totalPayments = 0.0;
                                            if (payments != null && !payments.isEmpty()) {
                                                for (JsonObject pay : payments) {
                                                    Double t = pay.getDouble("total");
                                                    if (t != null) {
                                                        totalPayments += t;
                                                    }
                                                }
                                            }

                                            String initValidAt = service.getString(type.getInitValidAt());
                                            if (initValidAt == null) {
                                                initValidAt = service.getString(type.getAltInitValidAt());
                                            }
                                            //Date dateInitValidAt = UtilsDate.toLocalDate(UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(initValidAt, "UTC"));
                                            Date dateInitValidAt = UtilsDate.toLocalDate(UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(initValidAt));
                                            // End of the month
                                            Date dateEndValidAt = UtilsDate.lastDayOfTheMonth(dateInitValidAt);
                                            // Plus two days
                                            dateEndValidAt = new Date(dateEndValidAt.getTime() + (1000 * 60 * 60 * 24 * 3));

                                            service.put("service", type.getName())
                                                    .put("amount", invoiceConditions.getDouble("amount"))
                                                    .put("service_type", serviceType)
                                                    .put("discount", service.getValue("discount"))
                                                    .put("payment_condition", service.getValue("payment_condition"))
                                                    .put("insurance_amount", service.getDouble("insurance_amount", 0.0))
                                                    .put("total_amount", invoiceConditions.getDouble("total_amount"))
                                                    .put("init_valid_at", initValidAt)
                                                    .put("end_valid_at", UtilsDate.sdfUTC(dateEndValidAt))
                                                    .put("iva", invoiceConditions.getDouble("iva"))
                                                    .put("iva_withheld", invoiceConditions.getDouble("iva_withheld"))
                                                    .put("iva_withheld_percent", invoiceConditions.getDouble("iva_withheld_percent"))
                                                    .put("invoice_status", invoiceConditions.getString("invoice_status"))
                                                    .put("invoice_id", invoiceId)
                                                    .put("paid", false)
                                                    .put("total_payment", totalPayments);

                                            // obtain payment info for invoice
                                            if (greaterPayment.isPresent()) {
                                                JsonObject paymentObject = greaterPayment.get();
                                                service.put("zip_code", paymentObject.getValue("zip_code"))
                                                        .put("payment_method", paymentObject.getString("payment_method"))
                                                        .put("paid", true);
                                            }

                                            // Period valid to invoice
                                            if (VALIDATE_PERIOD_INVOICE &&
                                                    (today.after(dateEndValidAt))) {
                                                service.put("invoice_status", ServiceStatus.EXPIRED.getValue());
                                            }

                                            if (type.getType().equals("parcel") &&
                                                    !service.getBoolean("pays_sender") &&
                                                    service.getString("payment_condition").equals("cash") &&
                                                    totalPayments == 0) {
                                                service.put("invoice_status", ServiceStatus.NO_ELEGIBLE.getValue());
                                                service.put("no_elegible_reason", "FxC no pagado");
                                            }

                                            future.complete(service);

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            future.fail(e);
                                        }
                                    });
                                } else {
                                    this.dbClient.queryWithParams(QUERY_INVOICE_BY_ID, new JsonArray().add(invoiceId), replyInvoice -> {
                                        try {
                                            if (replyInvoice.failed()) {
                                                throw new Exception(replyInvoice.cause());
                                            }

                                            List<JsonObject> invoices = replyInvoice.result().getRows();
                                            if (invoices.isEmpty()) {
                                                throw new Exception("Invoice: not found");
                                            }

                                            JsonObject invoice = invoices.get(0);
                                            String status = invoice.getString("invoice_status");
                                            if (status.equalsIgnoreCase(ServiceStatus.PENDING.getValue())) {
                                                status = ServiceStatus.PROGRESS.getValue();
                                            }

                                            Boolean isGlobal = invoice.getBoolean("is_global");
                                            if (isGlobal) {
                                                Double insuranceAmount = service.getDouble("insurance_amount", 0.0);
                                                Double total = service.getDouble("total_amount") - insuranceAmount;

                                                Double ivaWithheldPercent = type.getIvaWithHeldPercent();
                                                //Double ivaPercent = 0.16;

                                                Double subtotal = UtilsMoney.round(total / (1 + (service.getDouble("iva_percent") - ivaWithheldPercent)), 2);

                                                String initValidAt = service.getString(type.getInitValidAt());
                                                if (initValidAt == null) {
                                                    initValidAt = service.getString(type.getAltInitValidAt());
                                                }
                                                Date dateInitValidAt = UtilsDate.toLocalDate(UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(initValidAt, "UTC"));
                                                // End of the month
                                                Date dateEndValidAt = UtilsDate.lastDayOfTheMonth(dateInitValidAt);
                                                // Plus two days
                                                dateEndValidAt = new Date(dateEndValidAt.getTime() + (1000 * 60 * 60 * 24 * 3));

                                                service.put("service", type.getName())
                                                        .put("amount", subtotal)
                                                        .put("service_type", serviceType)
                                                        .put("discount", service.getValue("discount"))
                                                        .put("payment_condition", service.getValue("payment_condition"))
                                                        .put("insurance_amount", insuranceAmount)
                                                        .put("total_amount", total)
                                                        .put("init_valid_at", initValidAt)
                                                        .put("end_valid_at", UtilsDate.sdfUTC(dateEndValidAt))
                                                        .put("iva", UtilsMoney.round(subtotal * service.getDouble("iva_percent"), 2))
                                                        .put("iva_withheld", UtilsMoney.round(subtotal * ivaWithheldPercent, 2))
                                                        .put("invoice_status", ServiceStatus.EXPIRED.getValue())
                                                        .put("invoice_id", invoiceId)
                                                        .put("media_document_pdf_name", invoice.getString("media_document_pdf_name"))
                                                        .put("media_document_xml_name", invoice.getString("media_document_xml_name"));
                                            } else {
                                                service.put("service", type.getName())
                                                        .put("amount", invoice.getValue("amount"))
                                                        .put("service_type", serviceType)
                                                        .put("total_amount", invoice.getValue("total_amount"))
                                                        .put("iva", invoice.getValue("iva"))
                                                        .put("payment_condition", invoice.getValue("payment_condition"))
                                                        .put("init_valid_at", invoice.getValue("init_valid_at"))
                                                        .put("document_id", invoice.getValue("document_id"))
                                                        .put("iva_withheld", invoice.getValue("iva_withheld"))
                                                        .put("insurance_amount", invoice.getValue("insurance_amount"))
                                                        .put("discount", invoice.getValue("discount"))
                                                        .put("invoice_status", status)
                                                        .put("invoice_id", invoice.getValue("id"))
                                                        .put("media_document_pdf_name", invoice.getString("media_document_pdf_name"))
                                                        .put("media_document_xml_name", invoice.getString("media_document_xml_name"));
                                            }

                                            future.complete(service);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            future.fail(e);
                                        }
                                    });
                                }
                            } catch(Exception e) {
                                e.printStackTrace();
                                future.fail(e);
                            }
                        });
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    future.fail(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            future.fail(e);
        }
        return future;
    }

    private String getIvaQueryForService(String serviceType) {
        switch(serviceType) {
            case "boarding_pass":
                return QUERY_GET_IVA_BY_BRANCHOFFICE_BOARDINGPASS;
            case "parcel":
            case "freight":
                return QUERY_GET_IVA_BY_BRANCHOFFICE_PARCEL;
            case "guia_pp":
                return QUERY_GET_IVA_BY_BRANCHOFFICE_GUIAPP;
            case "prepaid":
                return QUERY_GET_IVA_BY_BRANCHOFFICE_PREPAID_BP;
            default: return "";
        }
    }

    private JsonObject getInvoiceConditions(String serviceType, JsonObject service, JsonObject extraParams, JsonObject billingInfo) {
        JsonObject invoiceConditions = new JsonObject()
                .put("iva_withheld_percent", 0.00)
                .put("invoice_status", ServiceStatus.PENDING.getValue());

        switch(serviceType) {
            case "guia_pp":
                // RANGOS RS a R3 no se consideran carga
                String rangePriceName = extraParams.getString("package_price_name");
                service.put("is_freight", false);
                if (rangePriceName != null && !COURIER_RANGES.contains(rangePriceName)) {
                    service.put("is_freight", true);
                }

                // REVISAR TIPO DE PERSONA, CARGA Y PERSONA M. RETIENEN el 4% de IVA
                if(billingInfo != null && !billingInfo.isEmpty()) {
                    LegalPerson legalP = LegalPerson.fromLegalPerson(billingInfo.getString("legal_person"));
                    if(legalP != null && legalP.isMoral() && service.getBoolean("is_freight")) {
                        invoiceConditions.put("iva_withheld_percent", legalP.getIvaWithheldPercent());
                    }
                }

                Double subtotal = UtilsMoney.roundP(service.getDouble("total_amount") / (1 + (service.getDouble("iva_percent") -
                        invoiceConditions.getDouble("iva_withheld_percent"))), 6);
                Double iva = UtilsMoney.roundP(subtotal * service.getDouble("iva_percent"), 6);
                Double ivaWithHeld = UtilsMoney.roundP(subtotal * invoiceConditions.getDouble("iva_withheld_percent"), 6);

                invoiceConditions
                    .put("amount", subtotal)
                    .put("iva", iva)
                    .put("iva_withheld", ivaWithHeld)
                    .put("total_amount", service.getDouble("total_amount"));
                break;
            case "parcel":
                // no requirió factura el cliente o es anterior al cambio
                if(service.getInteger("customer_billing_information_id") == null) {
                    invoiceConditions.put("invoice_status", ServiceStatus.NOT_REQUIRED.getValue());
                }

                // REVISAR TIPO DE PERSONA, PARA ALMACENAR EL % DE RETENCION CUANDO APLIQUE
                if(billingInfo != null && !billingInfo.isEmpty()) {
                    LegalPerson legalP = LegalPerson.fromLegalPerson(billingInfo.getString("legal_person"));
                    if(legalP != null && legalP.isMoral()) {
                        invoiceConditions.put("iva_withheld_percent", legalP.getIvaWithheldPercent());
                    }
                }

                // REVISAR CADA PAQUETE Y VER SI SON PyM o CARGA
                JsonArray packages = extraParams.getJsonArray("packages");
                if(!packages.isEmpty()) {
                    for (Object pkgO : packages) {
                        JsonObject pkg = (JsonObject) pkgO;
                        Double wObj = pkg.getDouble("weight");
                        double weight = (wObj != null) ? wObj : 0.0;
                        boolean isFreight = weight > 31.5;
                        pkg.put("is_freight", isFreight);
                    }
                }

                invoiceConditions
                        .put("amount", service.getDouble("t_amount"))
                        .put("iva", service.getDouble("iva"))
                        .put("iva_withheld", service.getDouble("iva_withheld"))
                        .put("total_amount", service.getDouble("total_amount"));
                break;
        }
        return invoiceConditions;
    }

    private Future<JsonObject> getServiceExtraParams(String serviceType, JsonObject data) {
        Future<JsonObject> future = Future.future();
        List<Future> futures = new ArrayList<>();
        JsonObject extraParams = new JsonObject();

        switch (serviceType) {
            case "guia_pp":
                Future<JsonArray> fPriceRange = queryAsyncWithFuture(QUERY_GET_PP_PRICE_NAME_BY_PREPAID_ID, new JsonArray().add(data.getInteger("id")));
                futures.add(fPriceRange);

                CompositeFuture.all(futures).setHandler(ar -> {
                    if (ar.succeeded()) {
                        JsonArray resultPriceRange = fPriceRange.result();
                        if (!resultPriceRange.isEmpty()) {
                            JsonObject resultObject = resultPriceRange.getJsonObject(0);
                            extraParams.put("package_price_name", resultObject.getString("package_price_name"));
                        }
                        future.complete(extraParams);
                    } else {
                        future.fail(ar.cause());
                    }
                });
                break;
            case "parcel":
                Integer parcelCBI = data.getInteger("customer_billing_information_id");
                String customerNickName = data.getString("customer_company_nick_name");

                Future<JsonArray> fPackages = queryAsyncWithFuture(WHERY_GET_PACKAGES_BY_PARCEL_ID, new JsonArray().add(data.getInteger("id")));
                futures.add(fPackages);

                AtomicInteger cbInfoIndex = new AtomicInteger(-1);
                AtomicInteger tlUUIDinfoIndex = new AtomicInteger(-1);

                Future<JsonArray> fCBInfo = null;
                Future<JsonArray> tlUUIDinfo = null;

                if(parcelCBI != null) {
                    fCBInfo = queryAsyncWithFuture(WHERY_GET_CUSTOMER_BILLING_INFO_SIMPLE, new JsonArray().add(parcelCBI));
                    cbInfoIndex.set(futures.size());
                    futures.add(fCBInfo);
                }

                if("HUICHOL".equals(customerNickName)) {
                    tlUUIDinfo = queryAsyncWithFuture(WHERY_GET_TRAVEL_LOG_INVOICE_UUID_FROM_PARCEL_ID, new JsonArray().add(data.getInteger("id")));
                    tlUUIDinfoIndex.set(futures.size());
                    futures.add(tlUUIDinfo);
                }

                if(data.getBoolean("relate_ccp_with_invoice") != null && data.getBoolean("relate_ccp_with_invoice")) {
                    tlUUIDinfo = queryAsyncWithFuture(WHERY_GET_CCP_UUID_FROM_PARCEL_ID, new JsonArray().add(data.getInteger("id")));
                    tlUUIDinfoIndex.set(futures.size());
                    futures.add(tlUUIDinfo);
                }

                CompositeFuture.all(futures).setHandler(ar -> {
                    if (ar.succeeded()) {
                        // Validar y obtener paquetes
                        JsonArray packages = ar.result().resultAt(0);
                        if (!packages.isEmpty()) {
                            extraParams.put("packages", packages);
                        }

                        // Validar y obtener información de facturación del cliente si existe
                        if (cbInfoIndex.get() != -1) {
                            JsonArray resultCBI = ar.result().resultAt(cbInfoIndex.get());
                            if (!resultCBI.isEmpty()) {
                                JsonObject cbi = resultCBI.getJsonObject(0);
                                extraParams.put("customer_billing_information", cbi);
                            }
                        }

                        // obtener el UUID de su CCP correspondiente si se añadió
                        if (tlUUIDinfoIndex.get() != -1) {
                            JsonArray resultUUID = ar.result().resultAt(tlUUIDinfoIndex.get());
                            if (!resultUUID.isEmpty()) {
                                String ccpUUID = resultUUID.getJsonObject(0).getString("uuid");
                                extraParams.put("ccp_uuid", ccpUUID);
                            }
                        }
                        future.complete(extraParams);
                    } else {
                        future.fail(ar.cause());
                    }
                });
                break;
            default:
                future.complete(new JsonObject());
                break;
        }
        return future;
    }

    private void register(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String code = body.getString("code").toUpperCase();
            String serviceType = body.getString("type");
            Integer billingID = body.getInteger("billing_id");
            String email = body.getString("email");
            String cfdiUse = body.getString("cfdi_use", "P01");
            Integer userID = body.getInteger("created_by");

            ServiceTypes type = ServiceTypes.getTypeByServiceTag(code.charAt(0), serviceType);
            if (type == null) {
                throw new Exception("Service: invalid code");
            }

            doServiceDetail(code, serviceType, null).setHandler(replyFindInvoice -> {
                try {
                    if (replyFindInvoice.failed()) {
                        throw new Exception(replyFindInvoice.cause());
                    }

                    // Validate status
                    JsonObject invoice = replyFindInvoice.result();
                    String status = invoice.getString("invoice_status");
                    Integer id = invoice.getInteger("id");
                    if (!status.equalsIgnoreCase(ServiceStatus.PENDING.getValue())) {
                        // Invoice already created
                        message.reply(invoice);
                    } else {
                        // New invoice for this type
                        this.startTransaction(message, conn ->
                                conn.queryWithParams(QUERY_CUSTOMER_BILLING_INFORMATION, new JsonArray().add(billingID), replyFindBilling -> {
                                    try {
                                        if (replyFindBilling.failed()) {
                                            throw new Exception(replyFindBilling.cause());
                                        }

                                        List<JsonObject> billings = replyFindBilling.result().getRows();
                                        if (billings.isEmpty()) {
                                            throw new Exception("Customer: not found");
                                        }

                                        JsonObject billing = billings.get(0);
                                        billing.put("clientCode", "D".concat(billing.getInteger("id").toString()));
                                        String billingStatus = billing.getString(type.getCustomerContpaqStatus());
                                        // Create invoice
                                        JsonObject newInvoice = new JsonObject()
                                                .put("description", invoice.getValue("service"))
                                                .put("service_type", invoice.getValue("service_type"))
                                                .put("amount", invoice.getValue("amount"))
                                                .put("discount", invoice.getValue("discount"))
                                                .put("total_amount", invoice.getValue("total_amount"))
                                                .put("insurance_amount", invoice.getValue("insurance_amount"))
                                                .put("payment_condition", invoice.getValue("payment_condition"))
                                                .put("iva", invoice.getValue("iva"))
                                                .put("customer_billing_information_id", billingID)
                                                .put("reference", code)
                                                .put("payment_method", invoice.getString("payment_method"))
                                                .put("zip_code", invoice.getValue("zip_code"))
                                                .put("cfdi_use", cfdiUse)
                                                .put("iva_withheld", invoice.getValue("iva_withheld"))
                                                .put("invoice_status", ServiceStatus.PROGRESS.getValue())
                                                .put("email", email)
                                                .put("created_by", userID);
                                        String insert = this.generateGenericCreate("invoice", newInvoice);

                                        conn.update(insert, replyInsertInvoice -> {
                                            try {
                                                if (replyInsertInvoice.failed()) {
                                                    throw new Exception(replyInsertInvoice.cause());
                                                }

                                                String tableService = type.getTable();
                                                Integer invoiceId = replyInsertInvoice.result().getKeys().getInteger(0);

                                                String updateServiceQuery = "UPDATE ".concat(tableService).concat(" SET invoice_id = ? WHERE id = ? ");
                                                JsonArray updateServiceParams = new JsonArray().add(invoiceId)
                                                        .add(invoice.getValue("id"));

                                                conn.queryWithParams(updateServiceQuery, updateServiceParams, replyUpdateService -> {
                                                    try {
                                                        if (replyUpdateService.failed()) {
                                                            throw new Exception(replyUpdateService.cause());
                                                        }

                                                        // TODO: Get tickets for details
                                                        invoice.put("code", code);
                                                        invoice.put("total", invoice.getValue("total_amount"));
                                                        invoice.put("cfdi_use", cfdiUse);
                                                        JsonArray details = new JsonArray().add(invoice);
                                                        String updateInvoiceTickets = " UPDATE tickets SET invoice_id = ? WHERE ".concat(type.getReference()).concat(" = ?");
                                                        JsonArray updateTicketsParams = new JsonArray().add(invoiceId).add(invoice.getValue("id"));
                                                        conn.queryWithParams(updateInvoiceTickets, updateTicketsParams, replyUpdateTickets ->{
                                                            try{
                                                                if (replyUpdateTickets.failed()) {
                                                                    throw new Exception(replyUpdateTickets.cause());
                                                                }

                                                                insertInvoiceContPAQ(conn, code, type, invoice, billing, details).whenComplete((resultInvoice, errorInvoice) -> {
                                                                    try {
                                                                        if (errorInvoice != null) {
                                                                            throw new Exception(errorInvoice);
                                                                        }

                                                                        if (!REGISTER_INVOICES) {
                                                                            this.commit(conn, message, newInvoice);
                                                                            return;
                                                                        }

                                                                        InvoiceInstance instance = InvoiceInstance.getInstanceByService(type);
                                                                        String updateInvoiceQuery = "UPDATE invoice SET ".concat(instance.getFieldId())
                                                                                .concat(" = ? WHERE id = ?;");

                                                                        JsonArray updateInvoiceParams = new JsonArray()
                                                                                .add(resultInvoice.getInteger("id"))
                                                                                .add(invoiceId);

                                                                        conn.updateWithParams(updateInvoiceQuery, updateInvoiceParams, replyUpdateInvoice -> {
                                                                            try {
                                                                                if (replyUpdateInvoice.failed()) {
                                                                                    throw new Exception(replyUpdateInvoice.cause());
                                                                                }

                                                                                this.commit(conn, message, newInvoice);

                                                                            } catch (Exception ex) {
                                                                                ex.printStackTrace();
                                                                                this.rollback(conn, ex, message);
                                                                            }
                                                                        });

                                                                    } catch (Exception e) {
                                                                        e.printStackTrace();
                                                                        this.rollback(conn, e, message);
                                                                    }
                                                                });


                                                                // If customer is not registered in ContPAQ then we set invoice as pending to send
                                                                // later when customer be successfully saved
//                                                                String updateInvoiceQuery = "UPDATE invoice SET invoice_status = 'pending' WHERE id = ?;";
//
//                                                                JsonArray updateInvoiceParams = new JsonArray()
//                                                                        .add(invoiceId);
//
//                                                                conn.updateWithParams(updateInvoiceQuery, updateInvoiceParams, replyUpdateInvoice -> {
//                                                                    try {
//                                                                        if (replyUpdateInvoice.failed()) {
//                                                                            throw new Exception(replyUpdateInvoice.cause());
//                                                                        }
//
//                                                                        this.commit(conn, message, newInvoice);
//
//                                                                    } catch (Exception ex) {
//                                                                        ex.printStackTrace();
//                                                                        this.rollback(conn, ex, message);
//                                                                    }
//                                                                });


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
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                this.rollback(conn, e, message);
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
                                    }
                                }));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    reportQueryError(message, e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void registerTimboxInvoice(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer parcel_id = (Integer) body.remove("parcel_id");
            String code = body.getString("code").toUpperCase();
            String cfdiUse = body.getString("cfdi_use", body.getString("cfdi_use"));
            JsonObject cfdi = body.getJsonObject("cfdi");
            JsonObject invoice = body.getJsonObject("invoice");
            JsonObject billing = body.getJsonObject("billing");
            String serviceType = (String) body.remove("type");
            ServiceTypes type = ServiceTypes.getTypeByReservationCode(code, serviceType);


            this.startTransaction(message, conn -> {

                JsonObject update = new JsonObject()
                        .put("tipo_cfdi", "ingreso")
                        .put("status_cfdi", "en proceso");

                String insertComplement = "";

                if (ServiceTypes.PARCEL.equals(type)) {
                    update.put("id_parcel",parcel_id );
                    insertComplement = this.generateGenericCreate("parcel_invoice_complement", update);
                } else if(ServiceTypes.BOARDING_PASS.equals(type)) {
                    update.put("id_boardingpass",parcel_id );
                    insertComplement = this.generateGenericCreate("boardingpass_invoice_complement", update);
                } else if(ServiceTypes.GUIA_PP.equals(type)) {
                    update.put("id_prepaid",parcel_id );
                    insertComplement = this.generateGenericCreate("guiapp_invoice_complement", update);
                } else if(ServiceTypes.PREPAID_BOARDING_PASS.equals(type)) {
                    update.put("id_prepaid",parcel_id );
                    insertComplement = this.generateGenericCreate("prepaid_travel_invoice_complement", update);
                }


                conn.update(insertComplement, (AsyncResult<UpdateResult> resp) -> {
                    try {
                        if(resp.failed()) {
                            throw new Exception(resp.cause());
                        }

                        String status = invoice.getString("invoice_status");
                        if(!status.equalsIgnoreCase(ServiceStatus.PENDING.getValue())) {
                            //invoice already created
                            message.reply(invoice);
                        } else {
                            //new invoice for this type
                            billing.put("clientCode", "D".concat(billing.getInteger("id").toString()));
                            //String billingStatus = billing.getString(type.getCustomerContpaqStatus());
                            JsonObject newInvoice = new JsonObject()
                                    .put("description", invoice.getValue("service"))
                                    .put("service_type", invoice.getValue("service_type"))
                                    .put("amount", invoice.getValue("amount"))
                                    .put("discount", invoice.getValue("discount"))
                                    .put("total_amount", invoice.getValue("total_amount"))
                                    .put("insurance_amount", invoice.getValue("insurance_amount"))
                                    .put("payment_condition", invoice.getValue("payment_condition"))
                                    .put("iva", invoice.getValue("iva"))
                                    .put("customer_billing_information_id", billing.getInteger("id"))
                                    .put("reference", code)
                                    .put("payment_method", invoice.getString("payment_method"))
                                    .put("zip_code", invoice.getValue("zip_code"))
                                    .put("cfdi_use", cfdiUse)
                                    .put("iva_withheld", invoice.getValue("iva_withheld"))
                                    .put("invoice_status", ServiceStatus.PROGRESS.getValue())
                                    .put("email", body.getString("email"))
                                    .put("created_by", billing.getInteger("created_by"))
                                    .put("cfdi_payment_method", cfdi.getString("MetodoPago"))
                                    .put("cfdi_payment_form", cfdi.getString("FormaPago"))
                                    .put("available_subtotal_for_complement", invoice.getValue("amount"))
                                    .put("available_iva_for_complement", invoice.getValue("iva"))
                                    .put("available_iva_withheld_for_complement", invoice.getValue("iva_withheld"))
                                    .put("available_amount_for_complement", invoice.getValue("total_amount"))
                                    .put("billing_name", cfdi.getJsonObject("Receptor").getString("Nombre"))
                                    .put("rfc", cfdi.getJsonObject("Receptor").getString("Rfc"));

                            String insert = this.generateGenericCreate("invoice", newInvoice);

                            conn.update(insert, replyInsertInvoice -> {
                                try {
                                    if (replyInsertInvoice.failed()) {
                                        throw new Exception(replyInsertInvoice.cause());
                                    }

                                    Integer invoiceID = replyInsertInvoice.result().getKeys().getInteger(0);
                                    invoice.put("id", invoiceID);
                                    // Update tabla de servicio
                                    String updateParcel = "";
                                    if (ServiceTypes.BOARDING_PASS.equals(type)) {
                                        updateParcel = "UPDATE boarding_pass SET invoice_id = ? WHERE id = ? ";
                                    } else if(ServiceTypes.PARCEL.equals(type)) {
                                        updateParcel = "UPDATE parcels SET invoice_id = ? WHERE id = ? ";
                                    }  else if(ServiceTypes.GUIA_PP.equals(type)) {
                                        updateParcel = "UPDATE parcels_prepaid SET invoice_id = ? WHERE id = ? ";
                                    }  else if(ServiceTypes.PREPAID_BOARDING_PASS.equals(type)) {
                                        updateParcel = "UPDATE prepaid_package_travel SET invoice_id = ? WHERE id = ? ";
                                    }

                                    JsonArray updateServiceParams = new JsonArray().add(invoiceID).add(parcel_id);

                                    conn.queryWithParams(updateParcel, updateServiceParams, replyUpdateParcel -> {
                                        try {
                                            if(replyUpdateParcel.failed()) {
                                                throw new Exception(replyUpdateParcel.cause());
                                            }
                                            invoice.put("type", type);
                                            cfdi.put("invoice", invoice);

                                            if(!billing.isEmpty()) {
                                                String updateCustomerBI = "UPDATE customer_billing_information set zip_code = ?, " +
                                                        "c_RegimenFiscal_id = ?, c_UsoCFDI_id = ? WHERE id = ?";
                                                JsonObject receptor = cfdi.getJsonObject("Receptor");
                                                JsonArray BIparams = new JsonArray()
                                                        .add(receptor.getString("DomicilioFiscalReceptor"))
                                                        .add(billing.getInteger("c_RegimenFiscal_id"))
                                                        .add(billing.getInteger("c_UsoCFDI_id"))
                                                        .add(billing.getInteger("id"));
                                                conn.queryWithParams(updateCustomerBI, BIparams, replyUpdateCustomerBI -> {
                                                    try {
                                                        if(replyUpdateCustomerBI.failed()) {
                                                            throw new Exception(replyUpdateCustomerBI.cause());
                                                        }
                                                        this.commit(conn,message, cfdi);
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                        this.rollback(conn, e, message);
                                                    }
                                                });
                                            } else {
                                                this.commit(conn,message, cfdi);
                                            }
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
                        }

                        //this.commit(conn,message, cfdi);

                    } catch (Throwable t) {
                        this.rollback(conn,t,message);
                    }
                });
            });
            //JsonObject complemento = new JsonObject().put("CartaPorte", cartaporteJSON);


        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void registerMultipleInvoice(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String cfdiUse = body.getString("cfdi_use", body.getString("cfdi_use"));
            JsonObject cfdi = body.getJsonObject("cfdi");
            JsonObject invoice = body.getJsonObject("invoice");
            JsonObject billing = body.getJsonObject("billing");
            JsonArray services = body.getJsonArray("services");
            ServiceTypes serviceType = ServiceTypes.getTypeByServiceName((String) body.remove("type"));

            this.startTransaction(message, conn -> {

                JsonObject update = new JsonObject()
                        .put("tipo_cfdi", "ingreso")
                        .put("status_cfdi", "en proceso");

                List<String> complementInserts = new ArrayList<>();

                services.forEach(s -> {
                    JsonObject service = (JsonObject) s;
                    String insertComplement = "";
                    if (ServiceTypes.PARCEL.equals(serviceType)) {
                        update.put("id_parcel", service.getJsonObject("parcel_info").getInteger("id") );
                        insertComplement = this.generateGenericCreate("parcel_invoice_complement", update);
                    } else if(ServiceTypes.GUIA_PP.equals(serviceType)) {
                        update.put("id_prepaid", service.getJsonObject("parcel_info").getInteger("id") );
                        insertComplement = this.generateGenericCreate("guiapp_invoice_complement", update);
                    }
                    complementInserts.add(insertComplement);
                });

                conn.batch(complementInserts, complementCreateReply -> {
                    try {
                        if (complementCreateReply.failed()){
                            throw complementCreateReply.cause();
                        }

                        //new invoice for this type
                        billing.put("clientCode", "D".concat(billing.getInteger("id").toString()));
                        Date invoiceDate = UtilsDate.parse_ISO8601(cfdi.getString("Fecha"), TimeZone.getTimeZone("UTC"));

                        JsonObject newInvoice = new JsonObject()
                                .put("description", invoice.getValue("service"))
                                .put("service_type", invoice.getValue("service_type"))
                                .put("amount", invoice.getValue("amount"))
                                .put("discount", invoice.getValue("discount"))
                                .put("iva", invoice.getValue("iva"))
                                .put("iva_withheld", invoice.getValue("iva_withheld"))
                                .put("total_amount", invoice.getValue("total_amount"))
                                .put("insurance_amount", invoice.getValue("insurance_amount"))
                                .put("payment_condition", invoice.getValue("payment_condition"))
                                .put("customer_billing_information_id", billing.getInteger("id"))
                                .put("reference", body.getJsonObject("reference_service").getString("code"))
                                .put("payment_method", invoice.getString("payment_method"))
                                .put("zip_code", invoice.getValue("zip_code"))
                                .put("cfdi_use", cfdiUse)
                                .put("invoice_status", ServiceStatus.PROGRESS.getValue())
                                .put("email", body.getString("email"))
                                .put("created_by", billing.getInteger("created_by"))
                                .put("is_multiple", true)
                                .put("cfdi_payment_method", cfdi.getString("MetodoPago"))
                                .put("cfdi_payment_form", cfdi.getString("FormaPago"))
                                .put("available_subtotal_for_complement", invoice.getValue("amount"))
                                .put("available_iva_for_complement", invoice.getValue("iva"))
                                .put("available_iva_withheld_for_complement", invoice.getValue("iva_withheld"))
                                .put("available_amount_for_complement", invoice.getValue("total_amount"))
                                .put("billing_name", cfdi.getJsonObject("Receptor").getString("Nombre"))
                                .put("rfc", cfdi.getJsonObject("Receptor").getString("Rfc"))
                                .put("invoice_date", UtilsDate.sdfDataBase(invoiceDate));

                        String insert = this.generateGenericCreate("invoice", newInvoice);

                        conn.update(insert, replyInsertInvoice -> {
                            try {
                                if (replyInsertInvoice.failed()) {
                                    throw new Exception(replyInsertInvoice.cause());
                                }

                                Integer invoiceID = replyInsertInvoice.result().getKeys().getInteger(0);
                                invoice.put("id", invoiceID);

                                // Update tabla de servicio
                                String updateParcel = "";
                                List<Integer> serviceIds = new ArrayList<>();
                                if(ServiceTypes.PARCEL.equals(serviceType)) {
                                    updateParcel = "UPDATE parcels SET invoice_id = ? WHERE id IN (%s) ";
                                }  else if(ServiceTypes.GUIA_PP.equals(serviceType)) {
                                    updateParcel = "UPDATE parcels_prepaid SET invoice_id = ? WHERE id IN (%s) ";
                                }

                                services.forEach(service -> {
                                    if (service instanceof JsonObject) {
                                        JsonObject jsonService = (JsonObject) service;
                                        Integer serviceId = jsonService.getJsonObject("parcel_info").getInteger("id");
                                        if (serviceId != null) {
                                            serviceIds.add(serviceId);
                                        }
                                    }
                                });
                                String placeholders = serviceIds.stream()
                                        .map(id -> "?")
                                        .collect(Collectors.joining(","));
                                String finalUpdateWithInvoiceId = String.format(updateParcel, placeholders);

                                JsonArray updateServiceParams = new JsonArray().add(invoiceID);
                                serviceIds.forEach(updateServiceParams::add);

                                conn.queryWithParams(finalUpdateWithInvoiceId, updateServiceParams, replyUpdateParcel -> {
                                    try {
                                        if(replyUpdateParcel.failed()) {
                                            throw new Exception(replyUpdateParcel.cause());
                                        }
                                        invoice.put("type", serviceType);
                                        invoice.put("services", services);
                                        cfdi.put("invoice", invoice);

                                        if(!billing.isEmpty()) {
                                            String updateCustomerBI = "UPDATE customer_billing_information set zip_code = ?, " +
                                                    "c_RegimenFiscal_id = ?, c_UsoCFDI_id = ? WHERE id = ?";
                                            JsonObject receptor = cfdi.getJsonObject("Receptor");
                                            JsonArray BIparams = new JsonArray()
                                                    .add(receptor.getString("DomicilioFiscalReceptor"))
                                                    .add(billing.getInteger("c_RegimenFiscal_id"))
                                                    .add(billing.getInteger("c_UsoCFDI_id"))
                                                    .add(billing.getInteger("id"));
                                            conn.queryWithParams(updateCustomerBI, BIparams, replyUpdateCustomerBI -> {
                                                try {
                                                    if(replyUpdateCustomerBI.failed()) {
                                                        throw new Exception(replyUpdateCustomerBI.cause());
                                                    }

                                                    if(invoice.getInteger("customer_id") != null) {
                                                        String updateCustomerEmail = "UPDATE customer set invoice_email = ? WHERE id = ?";
                                                        JsonArray invoiceEmailParams = new JsonArray()
                                                                .add(body.getString("email"))
                                                                .add(invoice.getInteger("customer_id"));
                                                        conn.queryWithParams(updateCustomerEmail, invoiceEmailParams, replyUpdateCustomerInvEmail -> {
                                                            try {
                                                                if(replyUpdateCustomerInvEmail.failed()) {
                                                                    throw new Exception(replyUpdateCustomerInvEmail.cause());
                                                                }
                                                                this.commit(conn,message, cfdi);
                                                            } catch(Exception e) {
                                                                e.printStackTrace();
                                                                this.commit(conn,message, cfdi);
                                                            }
                                                        });
                                                    } else {
                                                        this.commit(conn,message, cfdi);
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    this.rollback(conn, e, message);
                                                }
                                            });
                                        } else {
                                            this.commit(conn,message, cfdi);
                                        }
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
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void updateInvoiceStatus(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonObject cfdi = body.getJsonObject("cfdiResult");
            String error = cfdi.getString("error");
            Integer parcel_id = body.getInteger("parcel_id");
            String xml = body.getString("xml");
            String xmlNoQuotes = xml.substring(1, xml.length() - 1);
            String serviceType = body.getString("type");
            String invoiceUUID = Objects.toString(body.getString("uuid"), "");
            String serie = Objects.toString(body.getString("serie"), "");
            String folioStr = Objects.toString(body.getString("folio"), "");
            Integer folio = null;
            if (folioStr != null && !folioStr.trim().isEmpty()) {
                try {
                    folio = Integer.valueOf(folioStr.trim());
                } catch (NumberFormatException e) {
                    folio = null;
                }
            }
            String status = (error != null) ? "error" : "done";

            String query = "update invoice set invoice_status = ?, uuid = ?, cfdi_serie = ?, cfdi_folio = ? where id = ? ";
            JsonArray params = new JsonArray()
                    .add(status)
                    .add(invoiceUUID)
                    .add(serie);

            if (folio != null) {
                params.add(folio);
            } else {
                params.addNull();
            }
            params.add(body.getInteger("id"));

            this.startTransaction(message, conn -> {

                conn.queryWithParams(query, params, replyUpdate -> {
                    try {
                        if(replyUpdate.failed()) {
                            throw new Exception(replyUpdate.cause());
                        }

                        String queryGetComplement = "";

                        if (serviceType.equals("BOARDING_PASS")) {
                            queryGetComplement = QUERY_GET_INVOICE_COMPLEMENT_SITE_BOARDINGPASS;
                        } else if (serviceType.equals("PARCEL")) {
                            queryGetComplement = QUERY_GET_INVOICE_COMPLEMENT_SITE_PARCEL;
                        } else if (serviceType.equals("GUIA_PP")) {
                            queryGetComplement = QUERY_GET_INVOICE_COMPLEMENT_SITE_PREPAID;
                        } else if (serviceType.equals("PREPAID_BOARDING_PASS")) {
                            queryGetComplement = QUERY_GET_INVOICE_COMPLEMENT_SITE_PREPAID_BOARDINGPASS;
                        }

                        JsonArray paramsCp = new JsonArray().add(parcel_id);
                        conn.queryWithParams(queryGetComplement, paramsCp , replyGetComplement -> {
                            try {
                                if (replyGetComplement.failed()){
                                    throw new Exception(replyGetComplement.cause());
                                }

                                if(replyGetComplement.succeeded()) {
                                    List<JsonObject> lista = replyGetComplement.result().getRows();
                                    String query_update_complement = "";
                                    String concatStringUpdate = " status = '4' , system_origin = 'site' where id = ? ";
                                    if(lista.size() == 0) {
                                        if (serviceType.equals("BOARDING_PASS")) {
                                            query_update_complement = "update boardingpass_invoice_complement set xml = ? , status_cfdi = 'Error timbrado'' , ".concat(concatStringUpdate);
                                        } else if (serviceType.equals("PARCEL")) {
                                            query_update_complement = "update parcel_invoice_complement set xml = ? , status_cfdi = 'Error timbrado'' , ".concat(concatStringUpdate);
                                        } else if (serviceType.equals("GUIA_PP")) {
                                            query_update_complement = "update guiapp_invoice_complement set xml = ? , status_cfdi = 'Error timbrado'' , ".concat(concatStringUpdate);
                                        } else if (serviceType.equals("PREPAID_BOARDING_PASS")) {
                                            query_update_complement = "update prepaid_travel_invoice_complement set xml = ? , status_cfdi = 'Error timbrado'' , ".concat(concatStringUpdate);
                                        }
                                    } else {
                                        if (serviceType.equals("BOARDING_PASS")) {
                                            query_update_complement = "update boardingpass_invoice_complement set xml = ? , status_cfdi = 'timbrado', ".concat(concatStringUpdate);
                                        } else if (serviceType.equals("PARCEL")) {
                                            query_update_complement = "update parcel_invoice_complement set xml = ? , status_cfdi = 'timbrado',".concat(concatStringUpdate);
                                        } else if (serviceType.equals("GUIA_PP")) {
                                            query_update_complement = "update guiapp_invoice_complement set xml = ? , status_cfdi = 'timbrado', ".concat(concatStringUpdate);
                                        } else if (serviceType.equals("PREPAID_BOARDING_PASS")) {
                                            query_update_complement = "update prepaid_travel_invoice_complement set xml = ? , status_cfdi = 'timbrado', ".concat(concatStringUpdate);
                                        }
                                    }
                                    Integer idComplement = replyGetComplement.result().getRows().get(0).getInteger("id");

                                    JsonArray paramsUpdateComplement = new JsonArray().add(xmlNoQuotes).add(idComplement);

                                    conn.queryWithParams(query_update_complement, paramsUpdateComplement, replyUpdateComplement -> {
                                        try {
                                            if (replyUpdateComplement.failed()) {
                                                throw new Exception(replyUpdateComplement.cause());
                                            }

                                            this.commit(conn,message,cfdi);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            this.rollback(conn, e, message);
                                        }
                                    });
                                }else {
                                    this.commit(conn,message,cfdi);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                        //this.commit(conn,message, cfdi);
                    }catch (Exception e) {
                        e.printStackTrace();
                        this.rollback(conn, e, message);
                    }
                });

            });
        }catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void updateMultipleInvoiceStatus(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonArray services = body.getJsonArray("services");
            JsonObject cfdi = body.getJsonObject("cfdiResult");
            String error = cfdi.getString("error");
            String xml = body.getString("xml");
            String xmlNoQuotes = xml.substring(1, xml.length() - 1);
            String serviceType = body.getString("type");
            String invoiceUUID = Objects.toString(body.getString("uuid"), "");
            String serie = Objects.toString(body.getString("serie"), "");
            String folioStr = Objects.toString(body.getString("folio"), "");
            Integer folio = null;
            if (folioStr != null && !folioStr.trim().isEmpty()) {
                try {
                    folio = Integer.valueOf(folioStr.trim());
                } catch (NumberFormatException e) {
                    folio = null;
                }
            }
            String status = (error != null) ? "error" : "done";

            String queryUpdateInvoice = "update invoice set invoice_status = ?, uuid = ?, cfdi_serie = ?, cfdi_folio = ? where id = ? ";
            JsonArray paramsUpdateInvoice = new JsonArray()
                    .add(status)
                    .add(invoiceUUID)
                    .add(serie);

            if (folio != null) {
                paramsUpdateInvoice.add(folio);
            } else {
                paramsUpdateInvoice.addNull();
            }

            paramsUpdateInvoice.add(body.getInteger("id"));

            this.startTransaction(message, conn -> {
                conn.queryWithParams(queryUpdateInvoice, paramsUpdateInvoice, replyUpdateInvoice -> {
                    try {
                        if(replyUpdateInvoice.failed()) {
                            throw new Exception(replyUpdateInvoice.cause());
                        }

                        List<Integer> serviceIds = new ArrayList<>();
                        services.forEach(service -> {
                            if (service instanceof JsonObject) {
                                JsonObject jsonService = (JsonObject) service;
                                Integer serviceId = jsonService.getJsonObject("parcel_info").getInteger("id");
                                if (serviceId != null) {
                                    serviceIds.add(serviceId);
                                }
                            }
                        });

                        String tableName = "";
                        String columnName = "";
                        switch (serviceType) {
                            case "PARCEL":
                                tableName = "parcel_invoice_complement";
                                columnName = "id_parcel";
                                break;
                            case "GUIA_PP":
                                tableName = "guiapp_invoice_complement";
                                columnName = "id_prepaid";
                                break;
                            default:
                                this.rollback(conn, new Exception("Tipo de servicio no reconocido: " + serviceType), message);
                        }

                        String placeholders = serviceIds.stream().map(id -> "?").collect(Collectors.joining(","));
                        String queryUpdateComplement = String.format("UPDATE %s SET status = '4', xml = ?, status_cfdi = ?, system_origin = 'site' WHERE %s IN (%s)", tableName, columnName, placeholders);

                        JsonArray paramsUpdateComplement = new JsonArray();
                        paramsUpdateComplement.add(xmlNoQuotes).add(status.equals("error") ? "Error timbrado" : "timbrado");
                        serviceIds.forEach(paramsUpdateComplement::add);

                        conn.queryWithParams(queryUpdateComplement, paramsUpdateComplement, replyUpdateComplement -> {
                            try {
                                if (replyUpdateComplement.failed()) {
                                    throw new Exception(replyUpdateComplement.cause());
                                }

                                this.commit(conn,message,cfdi);
                            } catch (Exception e) {
                                e.printStackTrace();
                                this.rollback(conn, e, message);
                            }
                        });
                    }catch (Exception e) {
                        e.printStackTrace();
                        this.rollback(conn, e, message);
                    }
                });

            });
        }catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private CompletableFuture<JsonObject> getZatDirection(SQLConnection conn, JsonObject ubicacion ) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            Integer terminal_id =  ubicacion.getInteger("terminal_id");
           conn.queryWithParams(QUERY_GET_SAT_DIRECTION_V2, new JsonArray().add(terminal_id), replySat -> {
              try{
                  if(replySat.failed()) {
                      throw new Exception(replySat.cause());
                  }
                  List<JsonObject> resultSat = replySat.result().getRows();
                  JsonObject result = resultSat.get(0);
              } catch (Exception ex) {
                  ex.printStackTrace();
                  future.completeExceptionally(ex);
              }
           });
        } catch (Exception e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    private void getTimboxInfo(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String code = body.getString("code").toUpperCase();
            String serviceType = body.getString("type");
            Integer billingID = body.getInteger("billing_id");
            String email = body.getString("email");
            Integer userID = body.getInteger("created_by");
            String paymentCondition = body.getString("payment_condition");
            String customerParcelType = body.getString("customer_parcel_type");

            ServiceTypes type = ServiceTypes.getTypeByReservationCode(code, serviceType);
            if (type == null) {
                throw new Exception("Service: invalid code");
            }

            try {
                this.dbClient.queryWithParams(QUERY_CUSTOMER_BILLING_INFORMATION, new JsonArray().add(billingID), replyFindBilling -> {
                    try {
                        if (replyFindBilling.failed()) {
                            throw new Exception(replyFindBilling.cause());
                        }

                        List<JsonObject> billings = replyFindBilling.result().getRows();
                        if (billings.isEmpty()) {
                            throw new Exception("Customer: not found");
                        }

                        JsonObject billing = billings.get(0);
                        billing.put("clientCode", "D".concat(billing.getInteger("id").toString()));
                        String billingStatus = billing.getString(type.getCustomerContpaqStatus());
                        body.put("billing", billing);

                        doServiceDetail(code, serviceType, billing).setHandler(replyFindInvoice -> {
                            try {
                                if (replyFindInvoice.failed()) {
                                    throw new Exception(replyFindInvoice.cause());
                                }

                                // Validate status
                                JsonObject invoice = replyFindInvoice.result();
                                body.put("invoice", invoice);
                                body.put("serie", getSerieForInvoice(type, paymentCondition, customerParcelType));

                                this.getInfoService(type, invoice.getInteger("id")).whenComplete( (resultInfoService, errorInfoService) -> {
                                    try {
                                        if(errorInfoService != null) {
                                            throw errorInfoService;
                                        }

                                        body.put("parcel_info", resultInfoService);
                                        message.reply(body);

                                    } catch (Throwable e) {
                                        e.printStackTrace();
                                        reportQueryError(message, e);
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                reportQueryError(message, e);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        reportQueryError(message,e);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                reportQueryError(message, e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void processPendingInvoicesByBilling(Message<JsonObject> message) {
        try {
            startTransaction(message, conn -> {
                JsonObject billing = message.body();
                Integer billingID = billing.getInteger("id");
                String query = "SELECT * FROM invoice WHERE customer_billing_information_id = ? AND invoice_status = 'pending'";
                JsonArray params = new JsonArray().add(billingID);

                conn.queryWithParams(query, params, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        List<JsonObject> invoices = reply.result().getRows();
                        iterateProcessPendingInvoiceByBilling(conn, billing, invoices, replyProcess -> {
                            try {
                                System.out.println(String.valueOf(invoices.size())
                                        .concat(" invoices processed for ")
                                        .concat(billing.encodePrettily()));

                                this.commit(conn, message, new JsonObject().put("status", "OK"));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                this.rollback(conn, ex, message);
                            }
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        this.rollback(conn, ex, message);
                    }
                });
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private Future<JsonObject> iterateProcessPendingInvoiceByBilling(SQLConnection conn, JsonObject billing, List<JsonObject> invoices, Handler<JsonObject> handler) {
        return iterateProcessPendingInvoiceByBilling(conn, billing, invoices, 0, handler);
    }

    private Future<JsonObject> iterateProcessPendingInvoiceByBilling(SQLConnection conn, JsonObject billing, List<JsonObject> invoices, int index, Handler<JsonObject> handler) {
        Future<JsonObject> future = Future.future();
        int next = index + 1;
        try {
            if (index == invoices.size()) {
                System.out.println("All invoices processed");
                future.complete();
                handler.handle(billing);
                return future;
            }

            processOnePendingInvoiceByBilling(conn, billing, invoices.get(index)).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                      throw new Exception(reply.cause());
                    }

                    iterateProcessPendingInvoiceByBilling(conn, billing, invoices, next, handler);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.fail(ex);
                    iterateProcessPendingInvoiceByBilling(conn, billing, invoices, next, handler);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            future.fail(ex);
            iterateProcessPendingInvoiceByBilling(conn, billing, invoices, next, handler);
        }

        return future;
    }

    private Future<JsonObject> processOnePendingInvoiceByBilling(SQLConnection conn, JsonObject billing, JsonObject invoice) {
        Future<JsonObject> future = Future.future();

        try {
            String code = invoice.getString("reference");
            String serviceType = invoice.getString("service_type");
            Integer invoiceId = invoice.getInteger("id");
            String contpaqStatus = billing.getString("contpaq_status");
            String contpaqParcelStatus = billing.getString("contpaq_parcel_status");
            ServiceTypes type = ServiceTypes.getTypeByServiceTag(code.charAt(0), serviceType);
            if (type == null) {
                throw new Exception("Invalid type: ".concat(serviceType));
            }

            if (ServiceTypes.PARCEL.equals(type) &&
                    !ServiceStatus.DONE.getValue().equalsIgnoreCase(contpaqParcelStatus)) {
                throw new Exception("Customer not ready for invoice parcels");
            }

            if ((ServiceTypes.RENTAL.equals(type) || ServiceTypes.BOARDING_PASS.equals(type)) &&
                    !ServiceStatus.DONE.getValue().equalsIgnoreCase(contpaqStatus)) {
                throw new Exception("Customer not ready for invoice boarding pass or rentals");
            }

            // TODO: Get tickets for details
            invoice.put("code", invoice.getString("reference"));
            invoice.put("total", invoice.getValue("total_amount"));
            JsonArray details = new JsonArray().add(invoice);

            insertInvoiceContPAQ(conn, code, type, invoice, billing, details).whenComplete((resultInvoice, errorInvoice) -> {
                try {
                    if (errorInvoice != null) {
                        throw new Exception(errorInvoice);
                    }

                    if (!REGISTER_INVOICES) {
                        future.complete(invoice);
                        return;
                    }

                    JsonObject contPAQData = resultInvoice;
                    InvoiceInstance instance = InvoiceInstance.getInstanceByService(type);
                    String updateInvoiceQuery = "UPDATE invoice SET ".concat(instance.getFieldId())
                            .concat(" = ? WHERE id = ?;");

                    JsonArray updateInvoiceParams = new JsonArray()
                            .add(contPAQData.getValue("id"))
                            .add(invoiceId);

                    this.dbClient.updateWithParams(updateInvoiceQuery, updateInvoiceParams, replyUpdateInvoice -> {
                        try {
                            if (replyUpdateInvoice.failed()) {
                                throw new Exception(replyUpdateInvoice.cause());
                            }

                            future.complete(invoice);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            future.fail(ex);
                        }
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.fail(ex);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            future.fail(ex);
        }

        return future;
    }

    private CompletableFuture<JsonObject> insertInvoiceContPAQ(SQLConnection conn, String reference, ServiceTypes type, JsonObject invoice, JsonObject billing, JsonArray details) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            JsonObject document = new JsonObject();
            if (!REGISTER_INVOICES) {

                 System.out.println("Not sending invoice to ContPAQ");
                future.complete(document);
                return future;
            }

            ServicePaymentMethod paymentMethod = invoice.containsKey("payment_method") ? ServicePaymentMethod.getCodeByValue(invoice.getString("payment_method")) : null;
            String invoicePrefix = InvoiceInstance.getInstanceByService(type).getPrefix();

            Date dateInitValidAt;
            Date today = UtilsDate.getLocalDate();
            String initValidAt = invoice.getString("init_valid_at");
            try{
                dateInitValidAt = UtilsDate.toLocalDate(UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(initValidAt, "UTC"));
            } catch (Exception e){
                dateInitValidAt = UtilsDate.toLocalDate(UtilsDate.parse_yyyy_MM_dd_HH_mm_ss(initValidAt, ""));
            }

            Date dateEndOfMonth = UtilsDate.lastDayOfTheMonth(dateInitValidAt);
            Date dateEndValidAt = new Date(dateEndOfMonth.getTime() + (1000 * 60 * 60 * 24 * 3));
            String aDate = UtilsDate.format_YYYY_M_DD_HH_MM_SS(UtilsDate.parse_yyyy_MM_dd(UtilsDate.format_yyyy_MM_dd(UtilsDate.getLocalDate())));
            if(UtilsDate.isGreaterThanEqual(today, dateEndOfMonth) && UtilsDate.isLowerThanEqual(today, dateEndValidAt))
                aDate = UtilsDate.format_YYYY_M_DD_HH_MM_SS(UtilsDate.parse_yyyy_MM_dd(UtilsDate.format_yyyy_MM_dd(dateEndOfMonth)));

            InvoiceInstance invoiceInstance = InvoiceInstance.getInstanceByService(type);
            document.put("aFolio", 0)
                    .put("aNumMoneda", 1)
                    .put("aTipoCambio", 1)
                    .put("aImporte", invoice.getDouble("amount"))
                    .put("aDescuentoDoc1", 0)
                    .put("aDescuentoDoc2", 0)
                    .put("aSistemaOrigen", 209)
                    .put("aCodConcepto", type.getConceptCode())
                    .put("aSerie", invoicePrefix)
                    .put("aFecha", aDate)
                    .put("aCodigoCteProv", billing.getValue("clientCode"))
                    .put("aCodigoAgente", "")
                    .put("aReferencia", reference.length() > 21 ? reference.substring(0, 21) : reference)
                    .put("aAfecta", 0)
                    .put("aGasto1", 0)
                    .put("aGasto2", 0)
                    .put("aGasto3", 0)
                    .put("estatusServicio", 0)
                    .put("empresa", invoiceInstance.id)
                    .put("tipoPeticion", "POST");

            if (paymentMethod != null) {
                document.put("CMETODOPAG", paymentMethod.getCode());
            }

            String cfdiUse = invoice.getString("cfdi_use");
            if (cfdiUse != null) {
                document.put("CUSOCFDI", cfdiUse);
            }

            Integer zipCode = invoice.getInteger("zip_code");
            if (zipCode != null) {
                document.put("cLugarExpedicion", zipCode.toString());
            }

            JsonArray documentDetails = new JsonArray();

            for (int i = 0; i < details.size(); i++) {
                JsonObject detail = details.getJsonObject(i);
                JsonObject documentDetail = new JsonObject();

                Double totalAmount = detail.getDouble("total");
                double ivaWithheldPercent = type.getIvaWithHeldPercent();
                double ivaPercent = 0.16;

                double amount = UtilsMoney.round(totalAmount / (1 + (ivaPercent - ivaWithheldPercent)), 2);

                documentDetail.put("aConsecutivo", 0)
                        .put("aCodProdSer", type.getServiceCode())
                        .put("aCodAlmacen", "1")
                        .put("aReferencia", detail.getString("code"))
                        .put("aCodClasificacion", "")
                        .put("aCosto", amount)
                        .put("aUnidades", 1)
                        .put("aPrecio", amount)
                        .put("aImporte", amount)
                        .put("estatusServicio", 0);
                documentDetails.add(documentDetail);
            }

            document.put("aMovimientos", documentDetails);

            System.out.println(document.encodePrettily());

            this.sendDocumentContPAQ(conn, document, type).whenComplete((JsonObject resultDocument, Throwable documentError) -> {
                try{
                    if (documentError != null) {
                        throw new Exception(documentError);
                    }

                    future.complete(resultDocument);

                } catch(Exception e){
                    future.completeExceptionally(e);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    private void confirmRegisterInvoice(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String documentId = body.getString("folio");
            String pdfFileName = body.getString("pdf");
            String xmlFileName = body.getString("xml");
            Integer instanceId = body.getInteger("instance_id");
            InvoiceInstance instance = InvoiceInstance.getInstanceById(instanceId);
            if (instance == null) {
                throw new Exception("Invalid instance id for invoicing".concat(String.valueOf(instanceId)));
            }

            String query = QUERY_UPDATE_INVOICE_BY_CONTPAQ_ID_WITHOUT_DOCUMENTS
                    .replace("{FIELD_CONTPAQ_ID}", instance.getFieldId());
            JsonArray params = new JsonArray()
                .add(documentId)
                .add(body.getInteger("id"));
            if (pdfFileName != null && xmlFileName != null) {
                query = QUERY_UPDATE_INVOICE_BY_CONTPAQ_ID_WITH_DOCUMENTS
                        .replace("{FIELD_CONTPAQ_ID}", instance.getFieldId());
                params = new JsonArray()
                    .add(pdfFileName)
                    .add(xmlFileName)
                    .add(documentId)
                    .add(body.getInteger("id"));
            }

            System.out.println("Confirm invoice : ".concat(query).concat(params.encode()));

            String finalQuery = query;
            JsonArray finalParams = params;
            this.startTransaction(message, conn -> conn.updateWithParams(finalQuery, finalParams, result -> {
                try {
                    if (result.failed()) {
                        throw new Exception(result.cause());
                    }

                    int updated = result.result().getUpdated();
                    if (updated == 0) {
                        throw new Exception("Invoice: not found");
                    }

                    System.out.println("Updated invoice :".concat(finalParams.encode()));
                    this.commit(conn, message, new JsonObject());
                    // Send notification email
                    this.sendEmailInvoice(documentId);
                } catch (Exception e) {
                    e.printStackTrace();
                    this.rollback(conn, e, message);
                }
            }));

        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void updateInvoiceGlobal(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonObject cfdi = body.getJsonObject("cfdiResult");
            JsonObject cfdi_body = body.getJsonObject("cfdi_body");
            String error = cfdi.getString("error");
            JsonArray arrayID = cfdi_body.getJsonArray("array_of_ids");
            String xml = body.getString("xml");
            String xmlNoQuotes = xml.substring(1, xml.length() -1);
            String serviceType = cfdi_body.getJsonObject("invoice").getString("service_type");
            String invoiceUUID = Objects.toString(body.getString("uuid"), "");
            String serie = Objects.toString(body.getString("serie"), "");
            String folioStr = Objects.toString(body.getString("folio"), "");
            Integer folio = null;
            if (folioStr != null && !folioStr.trim().isEmpty()) {
                try {
                    folio = Integer.valueOf(folioStr.trim());
                } catch (NumberFormatException e) {
                    folio = null;
                }
            }
            String statusInvoice = (error != null) ? "error" : "done";

            String query = "update invoice set invoice_status = ?, uuid = ?, cfdi_serie = ?, cfdi_folio = ? where id = ? ";
            JsonArray params = new JsonArray()
                    .add(statusInvoice)
                    .add(invoiceUUID)
                    .add(serie);

            if (folio != null) {
                params.add(folio);
            } else {
                params.addNull();
            }
            params.add(cfdi_body.getJsonObject("invoice").getInteger("id"));

            String finalStatusInvoice = statusInvoice;
            this.startTransaction(message, conn -> {

                conn.queryWithParams(query, params, replyUpdate -> {
                    try {
                        if(replyUpdate.failed()) {
                            throw new Exception(replyUpdate.cause());
                        }

                        String estatusComplement = "";
                        if(finalStatusInvoice.equals("error")) {
                            estatusComplement = "Error timbrado";
                        } else if (finalStatusInvoice.equals("done")) {
                            estatusComplement = "timbrado";
                        }
                        JsonArray arrayUpdate = new JsonArray();

                        for (int i = 0 ; i < arrayID.size()  ; i++) {
                            JsonObject objads = arrayID.getJsonObject(i);
                            Integer objID = objads.getInteger("id");
                            String queryGetComplement = "";
                            if (serviceType.equals("parcel")) {
                                 queryGetComplement = "update parcel_invoice_complement set xml = '".concat(xmlNoQuotes).concat("' , status_cfdi = '").concat(estatusComplement).concat("' , updated_by = ").concat(cfdi_body.getJsonObject("invoice").getInteger("created_by").toString())
                                      .concat(" WHERE id_parcel = ").concat(objID.toString()).concat(" AND tipo_cfdi = 'factura global' ");
                            } else if (serviceType.equals("guia_pp")) {
                                queryGetComplement = "update guiapp_invoice_complement set xml = '".concat(xmlNoQuotes).concat("' , status_cfdi = '").concat(estatusComplement).concat("' , updated_by = ").concat(cfdi_body.getJsonObject("invoice").getInteger("created_by").toString())
                                        .concat(" WHERE id_prepaid = ").concat(objID.toString()).concat(" AND tipo_cfdi = 'factura global' ");
                            }
                            arrayUpdate.add(queryGetComplement);
                        }

                        List<String> insertUpdates = arrayUpdate.getList();

                        conn.batch(insertUpdates, (AsyncResult<List<Integer>> replyBatch) -> {
                            try {
                                if(replyBatch.failed()) {
                                    throw new Exception(replyBatch.cause());
                                }

                                this.commit(conn,message,cfdi);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                this.rollback(conn, ex, message);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.rollback(conn, e, message);
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void sendEmailInvoice(String documentId) {
        try {
            String query = "SELECT inv.email, inv.reference, inv.is_global, inv.description, " +
                    "cbi.name AS full_name, " +
                    "inv.media_document_pdf_name pdf_file, inv.media_document_xml_name xml_file " +
                    "FROM invoice AS inv " +
                    "LEFT JOIN customer_billing_information AS cbi " +
                    "ON cbi.id = inv.customer_billing_information_id " +
                    "WHERE inv.document_id = ?;";
            JsonArray params = new JsonArray().add(documentId);

            this.dbClient.queryWithParams(query, params, replyInvoice -> {
                try {
                    if (replyInvoice.failed()) {
                        throw new Exception(replyInvoice.cause());
                    }

                    List<JsonObject> invoices = replyInvoice.result().getRows();
                    if (invoices.isEmpty()) {
                        throw new Exception("Invoice not found");
                    }

                    JsonObject invoice = invoices.get(0);

                    if(!invoice.getBoolean("is_global")){

                        String email = invoice.getString("email");
                        String fullName = invoice.getString("full_name");
                        String code = invoice.getString("reference");
                        String pdfFile = invoice.getString("pdf_file");
                        String xmlFile = invoice.getString("xml_file");

                        if (code == null) {
                            code = invoice.getString("description");
                        }
                        String invoiceFileRoute = config().getString("invoice_file_host")
                                .concat("/");

                        JsonObject body = new JsonObject()
                                .put("template", "invoicemail.html")
                                .put("subject", "AllAbordo | Descargar factura")
                                .put("attachments" , new JsonArray()
                                        .add(new JsonObject()
                                                .put("filename" , code.concat(".xml"))
                                                .put("path" , invoiceFileRoute.concat(xmlFile)))

                                        .add(new JsonObject()
                                                .put("filename" , code.concat(".pdf"))
                                                .put("path" , invoiceFileRoute.concat(pdfFile))
                                        ))
                                .put("to", email)
                                .put("bcc", "facturacion@allabordo.com,abordo@indqtech.com")
                                .put("body", new JsonObject()
                                        .put("code", code)
                                        .put("name", fullName));

                        DeliveryOptions options = new DeliveryOptions()
                                .addHeader(ACTION, MailVerticle.ACTION_SEND_HTML_TEMPLATE_MAIL_MONGODB);

                        this.vertx.eventBus().send(MailVerticle.class.getSimpleName(), body, options, replySend -> {
                            if (replySend.failed()) {
                                replySend.cause().printStackTrace();
                            }
                        });
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private JsonObject getUnbilledQueries(Integer branchofficeID, Integer offset,
                                          Integer limit, InvoiceDBV.ServiceTypes type,
                                          Date initOfMonth, Date endOfMonth) throws ParseException {

        String query = JOINS_UNBILLED_BRANCHOFFICE;

        String queryItems = QUERY_UNBILLED.replace("{JOINS}", query)
                .replace("{SERVICE}", type.getTable())
                .replace("{SERVICE_CODE}", type.getCode())
                .replace("{SERVICE_ID}", type.getReference());
        String paymentMethods = Arrays.stream(ServicePaymentMethod.values())
                .map(p -> "SUM(IF(pt.payment_method = '".concat(p.getValue()).concat("', 1, 0)) AS ")
                        .concat(p.getValue()).concat("_method,\n").concat("SUM(IF(pt.payment_method = '")
                        .concat(p.getValue()).concat("', pt.amount, 0)) AS ").concat(p.getValue()).concat("_amount_method,\n"))
                .collect(Collectors.joining());

        String querySummary = SUMMARY_UNBILLED.replace("{JOINS}", query)
                .replace("{PAYMENT_METHODS}", paymentMethods)
                .replace("{GROUP}", GROUP_UNBILLED_BRANCHOFFICE)
                .replace("{SERVICE}", type.getTable())
                .replace("{SERVICE_CODE}", type.getCode())
                .replace("{SERVICE_ID}", type.getReference());

        String queryCount = COUNT_UNBILLED.replace("{JOINS}", query)
                .replace("{SERVICE}", type.getTable())
                .replace("{SERVICE_CODE}", type.getCode())
                .replace("{SERVICE_ID}", type.getReference());

        JsonArray paramsItems = new JsonArray();
        JsonArray paramsCount = new JsonArray();

        String queryWhere = WHERE_BY_BRANCHOFFICE.replace("{SERVICE_DATE}", type.getInitValidAt())
                .concat(" AND ").concat(type.getPaymentValidation());
        queryItems = queryItems.replace("{WHERE}", queryWhere);
        querySummary = querySummary.replace("{WHERE}", queryWhere);
        queryCount = queryCount.replace("{WHERE}", queryWhere);

        paramsItems.add(branchofficeID)
                .add(UtilsDate.sdfDataBase(initOfMonth))
                .add(UtilsDate.sdfDataBase(endOfMonth))
                .add(limit).add(offset);
        paramsCount.add(branchofficeID)
                .add(UtilsDate.sdfDataBase(initOfMonth))
                .add(UtilsDate.sdfDataBase(endOfMonth));

        JsonArray paramsSumary = paramsCount.copy();

        queryItems = queryItems.replaceAll("\\n\\n", "\n");
        queryCount = queryCount.replaceAll("\\n\\n", "\n");
        querySummary = querySummary.replaceAll("\\n\\n", "\n");

        String queryInvoice = QUERY_GET_INVOICE_DATA.concat(" AND i.purchase_origin = 'sucursal' AND i.branchoffice_id = ?");

        JsonArray paramsInvoice = new JsonArray()
                .add(UtilsDate.format_yyyy_MM_dd(initOfMonth))
                .add(type.getType())
                .add(branchofficeID);

        return new JsonObject().put("queryItems", queryItems)
                .put("paramsItems", paramsItems)
                .put("queryInvoice", queryInvoice)
                .put("paramsInvoice", paramsInvoice)
                .put("queryCount", queryCount)
                .put("paramsCount", paramsCount)
                .put("querySummary", querySummary)
                .put("paramsSummary", paramsSumary);
    }

    private JsonObject getUnbilledQueries(String purchaseOrigin, Integer offset,
                                          Integer limit, InvoiceDBV.ServiceTypes type, String serviceSubtype,
                                          Date initOfMonth, Date endOfMonth) throws ParseException {
        String queryItems = QUERY_UNBILLED.replace("{JOINS}", JOINS_UNBILLED_ORIGIN)
                .replace("{SERVICE}", type.getTable())
                .replace("{SERVICE_CODE}", type.getCode())
                .replace("{SERVICE_ID}", type.getReference());

        String paymentMethods = Arrays.stream(ServicePaymentMethod.values())
                .map(p -> "SUM(IF(pt.payment_method = '".concat(p.getValue()).concat("', 1, 0)) AS ")
                        .concat(p.getValue()).concat("_method,\n").concat("SUM(IF(pt.payment_method = '")
                        .concat(p.getValue()).concat("', pt.amount, 0)) AS ").concat(p.getValue()).concat("_amount_method,\n"))
                .collect(Collectors.joining());

        String querySummary = SUMMARY_UNBILLED.replace("{JOINS}", JOINS_UNBILLED_ORIGIN)
                .replace("{PAYMENT_METHODS}", paymentMethods)
                .replace("{GROUP}", GROUP_UNBILLED_ORIGIN)
                .replace("{SERVICE}", type.getTable())
                .replace("{SERVICE_CODE}", type.getCode())
                .replace("{SERVICE_ID}", type.getReference());

        String queryCount = COUNT_UNBILLED.replace("{JOINS}", JOINS_UNBILLED_ORIGIN)
                .replace("{SERVICE}", type.getTable())
                .replace("{SERVICE_CODE}", type.getCode())
                .replace("{SERVICE_ID}", type.getReference());

        JsonArray paramsItems = new JsonArray();
        JsonArray paramsCount = new JsonArray();

        String queryWhere = WHERE_BY_ORIGIN
                .replace("{SERVICE_DATE}", type.getInitValidAt())
                .concat(" AND ").concat(type.getPaymentValidation());

        if(!serviceSubtype.isEmpty()) {
            queryWhere = queryWhere.concat(" AND cust.parcel_type IS NOT NULL AND sv.payment_condition = 'cash'\n");
        }

        queryItems = queryItems.replace("{WHERE}", queryWhere);
        querySummary = querySummary.replace("{WHERE}", queryWhere);
        queryCount = queryCount.replace("{WHERE}", queryWhere);

        paramsItems.add(purchaseOrigin)
                .add(UtilsDate.sdfDataBase(initOfMonth))
                .add(UtilsDate.sdfDataBase(endOfMonth))
                .add(limit).add(offset);
        paramsCount.add(purchaseOrigin)
                .add(UtilsDate.sdfDataBase(initOfMonth))
                .add(UtilsDate.sdfDataBase(endOfMonth));

        JsonArray paramsSumary = paramsCount.copy();

        queryItems = queryItems.replaceAll("\\n\\n", "\n");
        queryCount = queryCount.replaceAll("\\n\\n", "\n");
        querySummary = querySummary.replaceAll("\\n\\n", "\n");

        String queryInvoice = QUERY_GET_INVOICE_DATA.concat(" AND purchase_origin = ?");

        JsonArray paramsInvoice = new JsonArray()
                .add(UtilsDate.format_yyyy_MM_dd(initOfMonth))
                .add(type.getType())
                .add(purchaseOrigin);

        if(!serviceSubtype.isEmpty()) {
            queryInvoice = queryInvoice.concat(" AND service_subtype = ?");
            paramsInvoice.add(serviceSubtype);
        }

        return new JsonObject().put("queryItems", queryItems)
                .put("paramsItems", paramsItems)
                .put("queryInvoice", queryInvoice)
                .put("paramsInvoice", paramsInvoice)
                .put("queryCount", queryCount)
                .put("paramsCount", paramsCount)
                .put("querySummary", querySummary)
                .put("paramsSummary", paramsSumary);
    }

    private String getSummaryTotalSubquery(String service, String serviceCode, String serviceId, Boolean byBranchOfficeId){
        return "(SELECT SUM(total) from ({QUERY}) as totals)".replace("{QUERY}",
                SUMMARY_UNBILLED.replace("{JOINS}", byBranchOfficeId ? JOINS_UNBILLED_BRANCHOFFICE : JOINS_UNBILLED_ORIGIN)
                        .replace("{PAYMENT_METHODS}", "DISTINCT sv.{SERVICE_CODE},")
                        .replace("{PAYMENT_TOTAL}", "sv.total_amount")
                        .replace("{GROUP}", "")
                        .replace("{SERVICE}", service)
                        .replace("{SERVICE_CODE}", serviceCode)
                        .replace("{SERVICE_ID}", serviceId)
                        .replace(";", "")
        );
    }

    private Future<JsonObject> replyUnbilled(String queryItems, JsonArray paramsItems,
                                             String queryCount, JsonArray paramsCount,
                                             String querySummary, JsonArray paramsSummary,
                                             String queryInvoice, JsonArray paramsInvoice, Integer draw) {
        Future<JsonObject> future = Future.future();

        this.dbClient.queryWithParams(queryCount, paramsCount, replyCount -> {
            try {
                if (replyCount.failed()) {
                    throw new Exception(replyCount.cause());
                }

                List<JsonObject> counts = replyCount.result().getRows();
                if (counts.isEmpty()) {
                    System.out.println("Count not found");
                    future.complete(emptyResponseUnbilled(draw));
                    return;
                }

                this.dbClient.queryWithParams(querySummary, paramsSummary, replySummary -> {
                    try {
                        if (replySummary.failed()) {
                            throw new Exception(replySummary.cause());
                        }

                        List<JsonObject> summaries = replySummary.result().getRows();
                        if (summaries.isEmpty()) {
                            future.complete(emptyResponseUnbilled(draw));
                            return;
                        }

                        Integer count = counts.get(0).getInteger("items", 0);
                        JsonObject summary = summaries.get(0)
                                .put("invoice_status", ServiceStatus.PENDING.getValue());
                        this.dbClient.queryWithParams(queryInvoice, paramsInvoice, replyInvoice -> {
                           try {
                                if (replyInvoice.failed()) {
                                    throw new Exception(replyInvoice.cause());
                                }

                                List<JsonObject> invoices = replyInvoice.result().getRows();
                                if (!invoices.isEmpty()) {
                                    summary.mergeIn(invoices.get(0));
                                }

                               this.dbClient.queryWithParams(queryItems, paramsItems, reply -> {
                                   try {
                                       if (reply.failed()) {
                                           throw new Exception(reply.cause());
                                       }

                                       List<JsonObject> items = reply.result().getRows();
                                       future.complete(responseUnbilled(draw, count, items, summary));

                                   } catch (Exception ex) {
                                       ex.printStackTrace();
                                       future.fail(ex);
                                   }
                               });

                           } catch(Exception ex) {
                               ex.printStackTrace();
                           }
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        future.fail(ex);
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                future.fail(ex);
            }
        });

        return future;
    }

    private JsonObject responseUnbilled(Integer draw, Integer count, List<JsonObject> items, JsonObject summary) {
        return new JsonObject()
                .put("draw", draw)
                .put("recordsTotal", count)
                .put("recordsFiltered", count)
                .put("data", items)
                .put("summary", summary);
    }

    private JsonObject emptyResponseUnbilled(Integer draw) {
        return responseUnbilled(draw, 0, new ArrayList<>(), new JsonObject());
    }

    private void getUnbilled(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            doGetUnbilled(body, true).setHandler(reply -> {
                if (reply.failed()) {
                    reportQueryError(message, reply.cause());
                } else {
                    message.reply(reply.result());
                }
            });

        } catch (Exception ex) {
            reportQueryError(message, ex);
        }
    }

    private Future<JsonObject> doGetUnbilled(JsonObject body, Boolean withSearch) {
        Future<JsonObject> future = Future.future();

        try {
            Integer offset = body.getInteger("start", 0);
            Integer limit = body.getInteger("length", MAX_LIMIT);
            Integer draw = body.getInteger("draw", 1);
            Date initOfMonth = UtilsDate.parse_ISO8601(body.getString("init_date"), UtilsDate.serverTimezone);
            Date endOfMonth = UtilsDate.parse_ISO8601(body.getString("end_date"), UtilsDate.serverTimezone);

            if (limit > MAX_LIMIT) {
                limit = MAX_LIMIT;
            }

            String purchaseOrigin = body.getString("purchase_origin", "sucursal");
            String service = body.getString("service", "not_set");
            String serviceSubtype = body.getString("service_subtype", "");
            ServiceTypes type = ServiceTypes.getTypeByServiceName(service);

            if (type == null) {
                throw new Exception("Invalid type");
            }

            JsonObject queries;
            if (purchaseOrigin.equals("sucursal") && serviceSubtype.isEmpty()) {
                Integer branchofficeID = body.getInteger("branchoffice_id", 0);
                queries = getUnbilledQueries(branchofficeID, offset, limit, type, initOfMonth, endOfMonth);
            } else {
                queries = getUnbilledQueries(purchaseOrigin, offset, limit, type, serviceSubtype, initOfMonth, endOfMonth);
            }

            return replyUnbilled(queries.getString("queryItems"), queries.getJsonArray("paramsItems"),
                    queries.getString("queryCount"), queries.getJsonArray("paramsCount"),
                    queries.getString("querySummary"), queries.getJsonArray("paramsSummary"),
                    queries.getString("queryInvoice"), queries.getJsonArray("paramsInvoice"), draw);

        } catch (Exception ex) {
            ex.printStackTrace();
            future.fail(ex);
        }
        return future;
    }

    private Future<JsonObject> paginateUnbilled(JsonObject request) {
        Future<JsonObject> future = Future.future();
        try {
            doGetUnbilled(request, false).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    JsonObject response = reply.result();
                    Integer reqCount = request.getInteger("count", 1);
                    Integer resCount = response.getInteger("recordsTotal", 0);
                    Integer reqTotal = request.getInteger("total", 1);
                    Integer resTotal = response.getJsonObject("summary").getInteger("total", 0);

                    if (!reqCount.equals(resCount) || !reqTotal.equals(resTotal)) {
                        throw new Exception("Summary don't match");
                    }


                    JsonArray resData = response.getJsonArray("data");
                    int resItems = resData.size();

                    JsonArray data = request.getJsonArray("data");
                    if (data != null) {
                        data.addAll(resData);
                    } else {
                        data = resData;
                    }

                    request.put("data", data);

                    if (resItems > 0) {
                        // Get next items
                        Integer offset = request.getInteger("start", 0);
                        request.put("start", offset + MAX_LIMIT)
                            .put("length", MAX_LIMIT);

                        paginateUnbilled(request).setHandler(pageReply -> {
                            try {
                                if (pageReply.failed()) {
                                    throw new Exception(pageReply.cause());
                                }

                                future.complete(pageReply.result());

                            } catch (Exception ex) {
                                ex.printStackTrace();
                                future.fail(ex);
                            }
                        });

                    } else {
                        response.put("data", data);
                        future.complete(response);

                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.fail(ex);
                }
            });


        } catch (Exception ex) {
            ex.printStackTrace();
            future.fail(ex);
        }

        return future;
    }

    private void registerGlobal(Message<JsonObject> message) {
        System.out.println("\nregister global\n");
        try {
            JsonObject request = message.body();
            Date initPeriod = UtilsDate.parse_ISO8601(request.getString("init_date"), UtilsDate.serverTimezone);
            Date endPeriod = UtilsDate.parse_ISO8601(request.getString("end_date"), UtilsDate.serverTimezone);
            Date invoiceDate = UtilsDate.parse_ISO8601_invoice_date(request.getString("invoice_date"), UtilsDate.timezone);
            String period = UtilsDate.format_yyyy_MM(initPeriod);
            String service = request.getString("service");
            ServiceTypes type = ServiceTypes.getTypeByServiceName(service);
            String serviceSubtype = request.getString("service_subtype", "");
            String periodicity = request.getString("periodicity");
            if (type == null) {
                throw new Exception("Invalid type");
            }

            paginateUnbilled(request).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    JsonObject response = reply.result();
                    JsonArray items = response.getJsonArray("data");
                    JsonObject summary = response.getJsonObject("summary");
                    String invoiceStatus = summary.getString("invoice_status");

                    if (invoiceStatus != null && !invoiceStatus.equals(ServiceStatus.PENDING.getValue())) {
                        message.reply(summary);
                        return;
                    }

                    //test
                    double totalAmount = 0.0;
                    for (int i = 0; i < items.size(); i++) {
                        double total = items.getJsonObject(i).getDouble("total");
                        totalAmount = totalAmount + total;
                    }

                    double totalInvoice = UtilsMoney.round(totalAmount, 2);
                    totalAmount = UtilsMoney.round(totalAmount, 2);

                    double ivaWithheldPercent = type.getIvaWithHeldPercent();
                    Double ivaPercent = 0.16;

                    double amount = UtilsMoney.round(totalAmount / (1 + (ivaPercent - ivaWithheldPercent)), 2);
                    double iva = UtilsMoney.round(amount * ivaPercent, 2);
                    double ivaWithheld = UtilsMoney.round(amount * ivaWithheldPercent, 2);

                    String purchaseOrigin = request.getString("purchase_origin", "sucursal");
                    String reference = period;

                    JsonObject invoice = new JsonObject()
                            .put("description", type.getName())
                            .put("service_type", type.getType())
                            .put("amount", amount)
                            .put("discount", 0)
                            .put("insurance_amount", 0)
                            .put("total_amount", totalAmount)
                            .put("iva_withheld", ivaWithheld)
                            .put("iva", iva)
                            .put("is_global", true)
                            .put("reference", reference)
                            .put("payment_condition", ServicePaymentMethod.getCodeByValue("cash"))
                            .put("purchase_origin", purchaseOrigin)
                            .put("global_period", UtilsDate.format_yyyy_MM_dd(initPeriod))
                            .put("init_valid_at", UtilsDate.sdfDataBase(initPeriod))
                            .put("end_valid_at", UtilsDate.sdfDataBase(endPeriod))
                            .put("created_by", request.getValue("created_by"))
                            .put("email", "")
                            .put("cfdi_payment_method", "PUE")
                            .put("cfdi_use", "S01")
                            .put("invoice_status", ServiceStatus.PROGRESS.getValue());

                    JsonObject globalInformation = new JsonObject()
                            .put("periodicity", periodicity)
                            .put("gi_month", UtilsDate.format_MM(initPeriod))
                            .put("gi_year", UtilsDate.format_yyyy(initPeriod))
                            .put("invoice_date", UtilsDate.format_YYYY_MM_DD_T_HH_MM_SS(invoiceDate, UtilsDate.timezone));

                    String code = purchaseOrigin;
                    JsonObject queries;
                    Integer branchofficeID = request.getInteger("branchoffice_id");
                    if (branchofficeID != null) {
                        invoice.put("branchoffice_id", request.getValue("branchoffice_id"));
                        code = code.concat("-").concat(branchofficeID.toString());
                        queries = getUnbilledQueries(branchofficeID, 0, 30, type, initPeriod, endPeriod);
                    } else {
                        if(!serviceSubtype.isEmpty()) {
                            invoice.put("service_subtype", serviceSubtype);
                            globalInformation.put("service_subtype", serviceSubtype);
                        }
                        queries = getUnbilledQueries(purchaseOrigin, 0, 0, type, serviceSubtype, initPeriod, endPeriod);
                    }

                    if(invoice.containsKey("branchoffice_id")){
                        String query = "SELECT zip_code FROM branchoffice WHERE id = ?;";
                        JsonArray param = new JsonArray();
                        param.add(invoice.getInteger("branchoffice_id"));
                        String finalCode = code;
                        Double finalIvaPercent = ivaPercent;

                        this.dbClient.queryWithParams(query, param, replyZip -> {
                            try {
                                if (replyZip.failed()) {
                                    throw new Exception(replyZip.cause());
                                }

                                if(!replyZip.result().getRows().isEmpty()){
                                    Integer zip_code = replyZip.result().getRows().get(0).getInteger("zip_code");
                                    invoice.put("zip_code", zip_code);
                                }

                                endRegisterGlobalTimbox(message, invoice, summary, items, queries, finalCode, type, totalInvoice, finalIvaPercent, globalInformation);

                            }catch (Throwable ex){
                                // failed queryWithParams ZipCode
                                ex.printStackTrace();
                            }
                        });
                    }else{
                        invoice.put("zip_code", 81200);
                        endRegisterGlobalTimbox(message, invoice, summary, items, queries, code, type, totalInvoice, ivaPercent, globalInformation);
                    }

                } catch (Exception ex) {
                    // failed paginateUnbilled
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        } catch (Exception ex) {
            // fail
            System.out.println("\n-- catch register global (invoiceDBV) --\n");
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void endRegisterGlobal(Message<JsonObject> message, JsonObject invoice, JsonObject summary, JsonArray items, JsonObject queries, String code, ServiceTypes type){
        String query = queries.getString("queryCount");
        JsonArray params = queries.getJsonArray("paramsCount");

        // Get mayor payment method for all services
        ServicePaymentMethod mayorPaymentMethod = ServicePaymentMethod.CASH;
        Double mayorPaymentMethodCount = 0.0;
        for (ServicePaymentMethod method: ServicePaymentMethod.values()) {
            Double methodCount = summary.getDouble(method.getValue().concat("_amount_method"), 0.0);
            if (methodCount > mayorPaymentMethodCount) {
                mayorPaymentMethod = method;
                mayorPaymentMethodCount = methodCount;
            }
        }

        invoice.put("payment_method", mayorPaymentMethod.getValue());

        this.startTransaction(message, conn -> conn.update(this.generateGenericCreate("invoice", invoice), insertReply -> {
            try {
                if (insertReply.failed()) {
                    throw new Exception(insertReply.cause());
                }

                Integer invoiceId = insertReply.result().getKeys().getInteger(0);
                invoice.put("id", invoiceId);

                // Update services
                String queryUpdate = query.replaceFirst("SELECT\\s.*\\sFROM", "UPDATE")
                        .replaceFirst("WHERE", " SET sv.invoice_id = ?, sv.invoice_is_global = 1 WHERE")
                        .replace(") as itemcount", "");
                JsonArray paramsUpdate = new JsonArray().add(invoiceId).addAll(params);

                conn.updateWithParams(queryUpdate, paramsUpdate, replyUpdate -> {
                    try {
                        if (replyUpdate.failed()) {
                            throw new Exception(replyUpdate.cause());
                        }

                        JsonObject billing = new JsonObject().put("clientCode", "999999");

                        insertInvoiceContPAQ(conn, code, type, invoice, billing, items).whenComplete((resultInvoice, errorInvoice) -> {
                            try {
                                if (errorInvoice != null) {
                                    throw new Exception(errorInvoice);
                                }

                                if (!REGISTER_INVOICES) {
                                    this.commit(conn, message, invoice);
                                    return;
                                }

                                InvoiceInstance instance = InvoiceInstance.getInstanceByService(type);
                                String updateInvoiceQuery = "UPDATE invoice SET ".concat(instance.getFieldId())
                                        .concat(" = ? WHERE id = ?;");

                                JsonArray updateInvoiceParams = new JsonArray()
                                        .add(resultInvoice.getValue("id"))
                                        .add(invoiceId);

                                conn.updateWithParams(updateInvoiceQuery, updateInvoiceParams, replyUpdateInvoice -> {
                                    try {
                                        if (replyUpdateInvoice.failed()) {
                                            throw new Exception(replyUpdateInvoice.cause());
                                        }

                                        this.commit(conn, message, invoice);

                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        this.rollback(conn, ex, message);
                                    }
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                                this.rollback(conn, e, message);
                            }
                        });

                    } catch(Exception ex) {
                        ex.printStackTrace();
                        this.rollback(conn, ex, message);
                    }

                });

            } catch(Exception ex) {
                ex.printStackTrace();
                this.rollback(conn, ex, message);
            }
        }));
    }

    private void exportGlobal(Message<JsonObject> message){
        try {
            JsonObject request = message.body();
            String service = request.getString("service");
            ServiceTypes type = ServiceTypes.getTypeByServiceName(service);
            if (type == null) {
                throw new Exception("Invalid type");
            }

            paginateUnbilled(request).setHandler(reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    JsonObject response = reply.result();
                    JsonArray items = response.getJsonArray("data");
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    String finalFileName = String.valueOf(Arrays.toString(digest.digest(items.encode().getBytes(StandardCharsets.UTF_8))).hashCode());
                    message.reply(new JsonObject().put("status", "Pending").put("fileName", finalFileName.concat(".csv")));
                    finalFileName = finalFileName.concat(".csv");
                    List<ExportGlobal> totalItems = new ArrayList<>();
                    for(int i = 0; i < items.size(); i++){
                        JsonObject item = items.getJsonObject(i);
                        Double total = item.getDouble("total");
                        String code = item.getString("code");
                        String createdAt = item.getString("created_at");
                        ExportGlobal result = new ExportGlobal(total, code, createdAt);
                        totalItems.add(result);
                    }
                    JSONArray array = new JSONArray(totalItems);
                    String csv = CDL.toString(array);
                    this.saveFileInvoice(csv, finalFileName).whenComplete((res, error)->{
                        try{
                            if(error != null){
                                throw new Exception(error);
                            }
                            System.out.println("Generate Invoice CSV - OK");
                        }catch (Exception e){
                            reportQueryError(message, e);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void getInvoiceCsv(Message<JsonObject> message){
        try{
            String fileName = message.body().getString("fileName");
            FileSystem fileSystem = vertx.fileSystem();
            String fileFinalPath = new File("")
                    .getAbsolutePath()
                    .concat("/files/invoices/")
                    .concat(fileName);
                this.getFileFromDisc(fileSystem, fileFinalPath).whenComplete((byte[] buffer, Throwable error) -> {
                    try{
                        if(error != null){
                            throw error;
                        }
                        message.reply(buffer);
                    }catch (Throwable e){
                        e.printStackTrace();
                        reportQueryError(message,e);
                    }
                });
        }catch (Exception ex){
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private CompletableFuture<byte[]> getFileFromDisc(FileSystem fileSystem, String fileFinalPath) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        fileSystem.readFile(fileFinalPath, reply -> {
            try{
                if (reply.failed()){
                    throw new Exception(reply.cause());
                } else {
                    future.complete(reply.result().getBytes());
                }
            }catch (Exception ex){
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    private CompletableFuture<JsonObject> sendDocumentContPAQ(SQLConnection conn, JsonObject document, ServiceTypes type) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        WebClient client = WebClient.create(this.vertx);
        JsonArray movements = (JsonArray) document.remove("aMovimientos");
        try {
            String host = InvoiceInstance.getHostByService(type);
            String insertDocument = this.generateGenericCreate("CONTPAQ_Documentos", document);
            conn.update(insertDocument, (AsyncResult<UpdateResult> replyDocument) -> {
                try {
                    if (replyDocument.failed()) {
                        future.completeExceptionally(replyDocument.cause());
                    }
                    final int id = replyDocument.result().getKeys().getInteger(0);
                    List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
                    IntStream.range(0, movements.size()).mapToObj(movements::getJsonObject).forEach(movement -> {
                        movement.put("idDocumento", id);
                        tasks.add(insertMovement(conn, movement));
                    });

                    CompletableFuture<Void> alltasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));
                    alltasks.whenComplete((resultAllTasks, errorAllTasks) -> {
                        try{
                            if (errorAllTasks != null) {
                                throw errorAllTasks;
                            }

                            future.complete(new JsonObject().put("id", id));

                        } catch (Throwable t){
                            t.printStackTrace();
                            future.completeExceptionally(t);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            client.close();
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    private CompletableFuture<String> saveFileInvoice( String file, String finalFileName) {
        CompletableFuture<String> future = new CompletableFuture<>();
        String fileFinalPath = new File("")
                .getAbsolutePath()
                .concat("/files/invoices/")
                .concat(finalFileName);
        Path pathToFile = Paths.get(fileFinalPath);
        try {
            byte[] buffer = file.getBytes();
            saveFile(buffer, pathToFile, fileFinalPath);
            future.complete("ok");
        } catch (IOException ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }
    public static void saveFile(byte[] buffer, Path actualPath, String finalPath) throws IOException {
        Files.createDirectories(actualPath.getParent());
        FileOutputStream stream = new FileOutputStream(finalPath);
        stream.write(buffer);
        stream.close();
    }

    private void associateDocument(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String code = body.getString("code").toUpperCase();
            String documentId = body.getString("document_id").toUpperCase();
            Integer billingID = body.getInteger("billing_id");
            String email = body.getString("email");
            String pdfDocumentName = body.getString("pdf_document_name");
            String xmlDocumentName = body.getString("xml_document_name");
            Integer userID = body.getInteger(CREATED_BY);

            ServiceTypes type = ServiceTypes.getTypeByReservationCode(code);
            if (type == null) {
                throw new Exception("Service: invalid code");
            }

            this.dbClient.queryWithParams(type.getQuery(), new JsonArray().add(code), replyService -> {
                try {
                    if (replyService.failed()) {
                        throw new Exception(replyService.cause());
                    }
                    List<JsonObject> services = replyService.result().getRows();
                    if (services.isEmpty()) {
                        throw new Exception("Service: not found");
                    }

                    JsonObject service = services.get(0);
                    if (service.getInteger("invoice_id") != null) {
                        throw new Exception("Service with related invoice");
                    }

                    JsonArray paramsPayments = new JsonArray().add(service.getInteger(ID));
                    String queryPayments = QUERY_SERVICE_GET_PAYMENTS.replace("{SERVICE_ID}", type.getReference());
                    this.dbClient.queryWithParams(queryPayments, paramsPayments, replyPayments -> {
                        try {
                            if (replyPayments.failed()) {
                                throw new Exception(replyPayments.cause());
                            }
                            List<JsonObject> payments = replyPayments.result().getRows();
                            Optional<JsonObject> greaterPayment = payments.stream().reduce((item, last) ->
                                    item.getInteger("total") > last.getDouble("total") ?
                                            item : last);
                            JsonObject paymentObject = greaterPayment.get();

                            Double insuranceAmount = service.getDouble("insurance_amount", 0.0);
                            Double total = UtilsMoney.round(service.getDouble("total_amount") - insuranceAmount, 2);

                            Double ivaWithheldPercent = type.getIvaWithHeldPercent();
                            Double ivaPercent = 0.16;

                            Double subtotal = UtilsMoney.round(total / (1 + (ivaPercent - ivaWithheldPercent)), 2);

                            startTransaction(message, conn -> {

                                JsonObject invoice = new JsonObject()
                                        .put("description", type.getName())
                                        .put(AMOUNT, subtotal)
                                        .put("service_type", type.getType())
                                        .put(DISCOUNT, service.getValue(DISCOUNT))
                                        .put("payment_condition", service.getValue("payment_condition"))
                                        .put("insurance_amount", insuranceAmount)
                                        .put(TOTAL_AMOUNT, total)
                                        .put("reference", code)
                                        .put(IVA, UtilsMoney.round(subtotal * ivaPercent, 2))
                                        .put("iva_withheld", UtilsMoney.round(subtotal * ivaWithheldPercent, 2))
                                        .put("customer_billing_information_id", billingID)
                                        .put("invoice_status", ServiceStatus.DONE.getValue())
                                        .put("zip_code", paymentObject.getValue("zip_code"))
                                        .put("payment_method", paymentObject.getString("payment_method"))
                                        .put("email", email)
                                        .put("document_id", documentId)
                                        .put("media_document_pdf_name", pdfDocumentName)
                                        .put("media_document_xml_name", xmlDocumentName)
                                        .put(CREATED_BY, userID);

                                String insert = this.generateGenericCreate("invoice", invoice);
                                conn.update(insert, replyInsertInvoice -> {
                                    try {
                                        if (replyInsertInvoice.failed()) {
                                            throw new Exception(replyInsertInvoice.cause());
                                        }
                                        String tableService = type.getTable();
                                        Integer invoiceId = replyInsertInvoice.result().getKeys().getInteger(0);
                                        invoice.put(ID, invoiceId);
                                        String updateServiceQuery = "UPDATE ".concat(tableService).concat(" SET invoice_id = ? WHERE id = ? ");
                                        JsonArray updateServiceParams = new JsonArray().add(invoiceId)
                                                .add(service.getValue(ID));

                                        conn.queryWithParams(updateServiceQuery, updateServiceParams, replyUpdateService -> {
                                            try {
                                                if (replyUpdateService.failed()) {
                                                    throw new Exception(replyUpdateService.cause());
                                                }

                                                String updateInvoiceTickets = " UPDATE tickets SET invoice_id = ? WHERE ".concat(type.getReference()).concat(" = ?");
                                                JsonArray updateTicketsParams = new JsonArray().add(invoiceId).add(service.getValue(ID));
                                                conn.queryWithParams(updateInvoiceTickets, updateTicketsParams, replyUpdateTickets ->{
                                                    try{
                                                        if (replyUpdateTickets.failed()) {
                                                            throw new Exception(replyUpdateTickets.cause());
                                                        }
                                                        this.commit(conn, message, invoice);
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

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
                                    }
                                });
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            reportQueryError(message, e);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    reportQueryError(message, e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void disassociateDocument(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String code = body.getString("code").toUpperCase();
            Integer userID = body.getInteger(UPDATED_BY);

            ServiceTypes type = ServiceTypes.getTypeByReservationCode(code);
            if (type == null) {
                throw new Exception("Service: invalid code");
            }

            this.dbClient.queryWithParams(type.getQuery(), new JsonArray().add(code), replyService -> {
                try {
                    if (replyService.failed()) {
                        throw new Exception(replyService.cause());
                    }
                    List<JsonObject> services = replyService.result().getRows();
                    if (services.isEmpty()) {
                        throw new Exception("Service: not found");
                    }

                    JsonObject service = services.get(0);
                    Integer invoiceId = service.getInteger("invoice_id");
                    Boolean serviceIsGlobal = service.getBoolean("invoice_is_global");

                    if (invoiceId == null)
                        throw new Exception("Service without invoices related");

                    if(serviceIsGlobal)
                        throw new Exception("Service invoice is global");

                    startTransaction(message, conn -> {
                        conn.queryWithParams(QUERY_INVOICE_BY_ID, new JsonArray().add(invoiceId), replyFindInvoice -> {
                            try {
                                if (replyFindInvoice.failed()) {
                                    throw new Exception(replyFindInvoice.cause());
                                }

                                List<JsonObject> invoices = replyFindInvoice.result().getRows();
                                if (invoices.isEmpty()) {
                                    throw new Exception("Invoice: not found or deleted");
                                }
                                JsonObject invoice = invoices.get(0);
                                Boolean invoiceIsGlobal = invoice.getBoolean("is_global");
                                if (invoiceIsGlobal)
                                    throw new Exception("Invoice is global");

                                invoice.clear();
                                invoice.put(ID, invoiceId).put(STATUS, 3).put(UPDATED_BY, userID)
                                        .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

                                GenericQuery updateInvoice = this.generateGenericUpdate("invoice", invoice);
                                conn.updateWithParams(updateInvoice.getQuery(), updateInvoice.getParams(), replyInsertInvoice -> {
                                    try {
                                        if (replyInsertInvoice.failed()) {
                                            throw new Exception(replyInsertInvoice.cause());
                                        }
                                        String tableService = type.getTable();
                                        String updateServiceQuery = "UPDATE ".concat(tableService).concat(" SET invoice_id = NULL WHERE id = ? ");
                                        JsonArray updateServiceParams = new JsonArray().add(service.getValue(ID));

                                        conn.queryWithParams(updateServiceQuery, updateServiceParams, replyUpdateService -> {
                                            try {
                                                if (replyUpdateService.failed()) {
                                                    throw new Exception(replyUpdateService.cause());
                                                }

                                                String updateInvoiceTickets = " UPDATE tickets SET invoice_id = NULL WHERE ".concat(type.getReference()).concat(" = ? AND invoice_id = ?");
                                                JsonArray updateTicketsParams = new JsonArray().add(invoice.getValue("id")).add(invoiceId);
                                                conn.queryWithParams(updateInvoiceTickets, updateTicketsParams, replyUpdateTickets ->{
                                                    try{
                                                        if (replyUpdateTickets.failed()) {
                                                            throw new Exception(replyUpdateTickets.cause());
                                                        }
                                                        this.commit(conn, message, invoice.put("code", code)
                                                                .put("service", type.getType()));
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
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    reportQueryError(message, e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void deleteInvoiceWithError(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            ServiceTypes type = ServiceTypes.getTypeByServiceName(body.getString("service_type"));
            String code = body.getString("code");
            if (type == null) {
                throw new Exception("Service: invalid code");
            }

            this.dbClient.queryWithParams(type.getQuery(), new JsonArray().add(code), replyService -> {
                try {
                    if (replyService.failed()) {
                        throw new Exception(replyService.cause());
                    }
                    List<JsonObject> services = replyService.result().getRows();
                    if (services.isEmpty()) {
                        throw new Exception("Service: not found");
                    }

                    JsonObject service = services.get(0);
                    Integer invoiceId = service.getInteger("invoice_id");
                    Boolean serviceIsGlobal = service.getBoolean("invoice_is_global");

                    if (invoiceId == null)
                        throw new Exception("Service without invoices related");

                    if(serviceIsGlobal != null && serviceIsGlobal)
                        throw new Exception("Service invoice is global");

                    startTransaction(message, conn -> {
                        conn.queryWithParams(QUERY_INVOICE_BY_ID, new JsonArray().add(invoiceId), replyFindInvoice -> {
                            try {
                                if (replyFindInvoice.failed()) {
                                    throw new Exception(replyFindInvoice.cause());
                                }

                                List<JsonObject> invoices = replyFindInvoice.result().getRows();
                                if (invoices.isEmpty()) {
                                    throw new Exception("Invoice: not found or deleted");
                                }
                                JsonObject invoice = invoices.get(0);
                                Boolean invoiceIsGlobal = invoice.getBoolean("is_global");
                                if (invoiceIsGlobal)
                                    throw new Exception("Invoice is global");

                                invoice.clear();
                                invoice.put(ID, invoiceId).put(STATUS, 3).put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

                                GenericQuery updateInvoice = this.generateGenericUpdate("invoice", invoice);
                                conn.updateWithParams(updateInvoice.getQuery(), updateInvoice.getParams(), replyInsertInvoice -> {
                                    try {
                                        if (replyInsertInvoice.failed()) {
                                            throw new Exception(replyInsertInvoice.cause());
                                        }
                                        String tableService = type.getTable();
                                        String updateServiceQuery = "UPDATE ".concat(tableService).concat(" SET invoice_id = NULL WHERE id = ? ");
                                        JsonArray updateServiceParams = new JsonArray().add(service.getValue(ID));

                                        conn.queryWithParams(updateServiceQuery, updateServiceParams, replyUpdateService -> {
                                            try {
                                                if (replyUpdateService.failed()) {
                                                    throw new Exception(replyUpdateService.cause());
                                                }

                                                String updateInvoiceTickets = " UPDATE tickets SET invoice_id = NULL WHERE ".concat(type.getReference()).concat(" = ? AND invoice_id = ?");
                                                JsonArray updateTicketsParams = new JsonArray().add(invoice.getValue("id")).add(invoiceId);
                                                conn.queryWithParams(updateInvoiceTickets, updateTicketsParams, replyUpdateTickets ->{
                                                    try{
                                                        if (replyUpdateTickets.failed()) {
                                                            throw new Exception(replyUpdateTickets.cause());
                                                        }
                                                        this.commit(conn, message, invoice.put("code", type.code)
                                                                .put("service", type.getType()));
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
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    reportQueryError(message, e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void deleteInvoiceMultipleWithError(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            ServiceTypes type = ServiceTypes.getTypeByServiceName(body.getString("service_type"));
            Integer invoiceId = body.getInteger("id");
            JsonArray services = body.getJsonArray("services");

            if (type == null) {
                throw new Exception("Service: invalid code");
            }

            startTransaction(message, conn -> {
                conn.queryWithParams(QUERY_INVOICE_BY_ID, new JsonArray().add(invoiceId), replyFindInvoice -> {
                    try {
                        if (replyFindInvoice.failed()) {
                            throw new Exception(replyFindInvoice.cause());
                        }

                        List<JsonObject> invoices = replyFindInvoice.result().getRows();
                        if (invoices.isEmpty()) {
                            throw new Exception("Invoice: not found or deleted");
                        }
                        JsonObject invoice = invoices.get(0);
                        Boolean invoiceIsGlobal = invoice.getBoolean("is_global");
                        if (invoiceIsGlobal)
                            throw new Exception("Invoice is global");

                        invoice.clear();
                        invoice.put(ID, invoiceId).put(STATUS, 3).put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

                        GenericQuery updateInvoice = this.generateGenericUpdate("invoice", invoice);
                        conn.updateWithParams(updateInvoice.getQuery(), updateInvoice.getParams(), replyInsertInvoice -> {
                            try {
                                if (replyInsertInvoice.failed()) {
                                    throw new Exception(replyInsertInvoice.cause());
                                }

                                List<Integer> serviceIds = new ArrayList<>();
                                services.forEach(service -> {
                                    if (service instanceof JsonObject) {
                                        JsonObject jsonService = (JsonObject) service;
                                        Integer serviceId = jsonService.getJsonObject("parcel_info").getInteger("id");
                                        if (serviceId != null) {
                                            serviceIds.add(serviceId);
                                        }
                                    }
                                });

                                String placeholders = serviceIds.stream()
                                        .map(id -> "?")
                                        .collect(Collectors.joining(","));
                                String tableService = type.getTable();
                                String updateServiceQuery = "UPDATE " + tableService + " SET invoice_id = NULL WHERE id IN (" + placeholders + ")";
                                JsonArray updateServiceParams = new JsonArray();
                                serviceIds.forEach(updateServiceParams::add);

                                conn.queryWithParams(updateServiceQuery, updateServiceParams, replyUpdateService -> {
                                    try {
                                        if (replyUpdateService.failed()) {
                                            throw new Exception(replyUpdateService.cause());
                                        }

                                        String updateInvoiceTickets = "UPDATE tickets SET invoice_id = NULL WHERE " + type.getReference() + " IN (" + placeholders + ") AND invoice_id = ?";

                                        JsonArray updateTicketsParams = new JsonArray();
                                        serviceIds.forEach(updateTicketsParams::add);
                                        updateTicketsParams.add(invoiceId);

                                        conn.queryWithParams(updateInvoiceTickets, updateTicketsParams, replyUpdateTickets ->{
                                            try{
                                                if (replyUpdateTickets.failed()) {
                                                    throw new Exception(replyUpdateTickets.cause());
                                                }
                                                this.commit(conn, message, invoice.put("code", type.code)
                                                        .put("service", type.getType()));
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
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void deleteCCPWithError(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer CCPid = null;
            String id = "";
            String CCPTableName = "";
            String tableName = "";
            String referenceField = "";
            String referenceValue = "";

            switch(body.getString("type")) {
                case "travel_log":
                    CCPid = body.getInteger("travel_logs_ccp_id");
                    id = "travel_log_id";
                    CCPTableName = "travel_logs_ccp";
                    tableName = "travel_logs";
                    referenceField = "is_stamped";
                    referenceValue = "0";
                    break;
                case "EAD/RAD":
                    CCPid = body.getInteger("parcels_manifest_ccp_id");
                    id = "parcels_manifest_id";
                    CCPTableName = "parcels_manifest_ccp";
                    tableName = "parcels_manifest";
                    referenceField = "parcels_manifest_ccp_id";
                    referenceValue = "NULL";
                    break;
            }
            JsonObject ccp = new JsonObject()
                    .put(ID, CCPid)
                    .put(STATUS, 3)
                    .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

            String finalCCPTableName = CCPTableName;
            String finalTableName = tableName;
            String finalReferenceField = referenceField;
            String finalreferenceValue = referenceValue;
            String finalId = id;
            startTransaction(message, conn -> {
                GenericQuery updateInvoice = this.generateGenericUpdate(finalCCPTableName, ccp);
                conn.updateWithParams(updateInvoice.getQuery(), updateInvoice.getParams(), replyUpdateCCP -> {
                    try {
                        if (replyUpdateCCP.failed()) {
                            throw new Exception(replyUpdateCCP.cause());
                        }

                        String updateTLQuery = "UPDATE " + finalTableName + " SET " + finalReferenceField + " = "+ finalreferenceValue + " WHERE id = ? ";
                        JsonArray updateTLParams = new JsonArray().add(body.getInteger(finalId));

                        conn.queryWithParams(updateTLQuery, updateTLParams, replyUpdateTL -> {
                            try {
                                if (replyUpdateTL.failed()) {
                                    throw new Exception(replyUpdateTL.cause());
                                }
                                this.commit(conn, message, new JsonObject().put("result", replyUpdateTL.succeeded()));
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
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void getGlobalInvoiceXML(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer id = Integer.parseInt(body.getString("id"));
            String serviceType = body.getString("type");
            String QUERY = "";

            switch(serviceType) {
                case "parcel":
                    QUERY = QUERY_GET_GLOBAL_INVOICE_XML_BY_ID_PARCEL_SERVICE;
                    break;
                case "guia_pp":
                    QUERY = QUERY_GET_GLOBAL_INVOICE_XML_BY_ID_PARCEL_PREPAID_SERVICE;
                    break;
            }

            this.dbClient.queryWithParams(QUERY, new JsonArray().add(id), reply -> {
                try {
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    message.reply(result.isEmpty() ? new JsonObject() : result.get(0));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void getCustomerServices(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String serviceType = body.getString("service_type");
            String dateType = body.getString("filter_by_date");
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer customerId = body.getInteger("customer_id");
            Boolean includeInvoiced = body.getBoolean("include_invoiced");
            Integer limit = body.getInteger(LIMIT);
            Integer page = body.getInteger(PAGE);

            String QUERY = "";
            String QUERY_COUNT = "";
            switch (serviceType) {
                case "parcel":
                    QUERY = QUERY_GET_PARCELS_FOR_INVOICING_BY_CRITERIA;
                    QUERY_COUNT = QUERY_COUNT_GET_PARCELS_FOR_INVOICING_BY_CRITERIA.replace("{TABLE_NAME}", "parcels");

                    break;
                case "guia_pp":
                    QUERY = QUERY_GET_PARCELS_PREPAID_FOR_INVOICING_BY_CRITERIA;
                    QUERY_COUNT = QUERY_COUNT_GET_PARCELS_FOR_INVOICING_BY_CRITERIA.replace("{TABLE_NAME}", "parcels_prepaid");
                    break;
            }

            JsonArray params = new JsonArray();
            params.add(initDate).add(endDate);

            if(customerId != null){
                QUERY += " AND p.customer_id = ?";
                QUERY_COUNT += " AND p.customer_id = ?";
                params.add(customerId);
            }

            if(!includeInvoiced) {
                QUERY += " AND p.invoice_id IS NULL";
                QUERY_COUNT += " AND p.invoice_id IS NULL";
            }

            switch(dateType) {
                case "created_at":
                    QUERY = QUERY.concat(" ORDER BY p.created_at DESC");
                    break;
                case "invoice_date":
                    QUERY = QUERY.concat(" ORDER BY inv.created_at DESC");
                    break;
            }

            List<Future> taskList = new ArrayList<>();
            Future f1 = Future.future();
            Future f2 = Future.future();

            this.dbClient.queryWithParams(QUERY_COUNT, params.copy(), f1.completer());
            taskList.add(f1);

            QUERY += " LIMIT ? OFFSET ?";
            params.add(limit).add((page - 1) * limit);
            this.dbClient.queryWithParams(QUERY, params, f2.completer());
            taskList.add(f2);

            CompositeFuture.all(taskList).setHandler(reply -> {
                if (reply.failed()) {
                    reportQueryError(message, reply.cause());
                } else {
                    ResultSet resultSet1 = reply.result().resultAt(0);
                    Integer count = resultSet1.getRows().get(0).getInteger("count");
                    Double pendingAmount = resultSet1.getRows().get(0).getDouble("pending_amount");
                    Integer pendingCount = resultSet1.getRows().get(0).getInteger("pending_count");
                    Integer invoicedCount = resultSet1.getRows().get(0).getInteger("invoiced_count");
                    Double invoicedAmount = resultSet1.getRows().get(0).getDouble("invoiced_amount");
                    Double totalAmount = resultSet1.getRows().get(0).getDouble("amount");

                    List<JsonObject> parcelsList = reply.result().<ResultSet>resultAt(1).getRows();
                    JsonObject result = new JsonObject()
                            .put("count", count)
                            .put("pending_count", pendingCount)
                            .put("invoiced_count", invoicedCount)
                            .put("pending_amount", pendingAmount)
                            .put("invoiced_amount", invoicedAmount)
                            .put("amount", totalAmount)
                            .put("items", parcelsList.size())
                            .put("results", parcelsList);
                    message.reply(result);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void getInvoiceXML(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer id = Integer.parseInt(body.getString("id"));
            String serviceType = body.getString("type");
            String QUERY = "";

            switch(serviceType) {
                case "parcel":
                    QUERY = QUERY_GET_GLOBAL_INVOICE_XML_BY_ID_PARCEL_SERVICE;
                    break;
                case "guia_pp":
                    QUERY = QUERY_GET_GLOBAL_INVOICE_XML_BY_ID_PARCEL_SERVICE;
                    break;
            }

            this.dbClient.queryWithParams(QUERY, new JsonArray().add(id), reply -> {
                try {
                    if(reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    message.reply(result.isEmpty() ? new JsonObject() : result.get(0));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private CompletableFuture<JsonObject> insertMovement(SQLConnection conn, JsonObject movement) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String insert = this.generateGenericCreate("CONTPAQ_Movimientos", movement);
        conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
            try {
                if (reply.succeeded()) {
                    final int id = reply.result().getKeys().getInteger(0);
                    movement.put("id", id);
                    future.complete(movement);
                } else {
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }
    private void disassociateGlobalBillingDocument(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer invoiceId = body.getInteger("invoice_id");
            Integer userID = body.getInteger(UPDATED_BY);

            this.dbClient.queryWithParams("SELECT service_type from invoice where id = ?;", new JsonArray().add(invoiceId), replyService -> {
                try {
                    if (replyService.failed()) {
                        throw new Exception(replyService.cause());
                    }
                    List<JsonObject> services = replyService.result().getRows();
                    if (services.isEmpty()) {
                        throw new Exception("Invoice: Invoice not found");
                    }

                    JsonObject service = services.get(0);
                    ServiceTypes type = ServiceTypes.getTypeByServiceName(service.getString("service_type"));

                    startTransaction(message, conn -> {
                        conn.queryWithParams(QUERY_INVOICE_BY_ID, new JsonArray().add(invoiceId), replyFindInvoice -> {
                            try {
                                if (replyFindInvoice.failed()) {
                                    throw new Exception(replyFindInvoice.cause());
                                }

                                List<JsonObject> invoices = replyFindInvoice.result().getRows();
                                if (invoices.isEmpty()) {
                                    throw new Exception("Invoice: not found or deleted");
                                }
                                JsonObject invoice = invoices.get(0);

                                invoice.clear();
                                invoice.put(ID, invoiceId).put(STATUS, 3).put(UPDATED_BY, userID)
                                        .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

                                GenericQuery updateInvoice = this.generateGenericUpdate("invoice", invoice);
                                conn.updateWithParams(updateInvoice.getQuery(), updateInvoice.getParams(), replyInsertInvoice -> {
                                    try {
                                        if (replyInsertInvoice.failed()) {
                                            throw new Exception(replyInsertInvoice.cause());
                                        }
                                        String tableService = type.getTable();
                                        String updateServiceQuery = "UPDATE ".concat(tableService).concat(" SET invoice_id = NULL, invoice_is_global = 0 WHERE invoice_id = ? ");
                                        JsonArray updateServiceParams = new JsonArray().add(invoiceId);

                                        conn.queryWithParams(updateServiceQuery, updateServiceParams, replyUpdateService -> {
                                            try {
                                                if (replyUpdateService.failed()) {
                                                    throw new Exception(replyUpdateService.cause());
                                                }

                                                this.commit(conn, message, invoice.put("invoice_id", invoiceId)
                                                        .put("service", type.getType()));

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
                            } catch (Exception e) {
                                e.printStackTrace();
                                this.rollback(conn, e, message);
                            }
                        });
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    reportQueryError(message, e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private CompletableFuture<JsonObject> getInfoService( ServiceTypes type, Integer id) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String query = "";

            if(ServiceTypes.PARCEL.equals(type)) {
                query = QUERY_GET_PARCEL_INFO_BY_ID;
            } else if(ServiceTypes.BOARDING_PASS.equals(type)) {
                query = QUERY_GET_BOARDINGPASS_INFO_BY_ID;
            } else if(ServiceTypes.GUIA_PP.equals(type)) {
                query = QUERY_GET_GUIAPP_INFO_BY_ID;
            } else if(ServiceTypes.PREPAID_BOARDING_PASS.equals(type)) {
                query = QUERY_GET_PREPAID_BPP_INFO_BY_ID;
            }

            this.dbClient.queryWithParams(query, new JsonArray().add(id), replyInfoService -> {
                try {
                    if(replyInfoService.failed()) {
                        throw new Exception(replyInfoService.cause());
                    }
                    List<JsonObject> resultInfo = replyInfoService.result().getRows();
                    JsonObject result = resultInfo.get(0);

                    if(ServiceTypes.GUIA_PP.equals(type) || ServiceTypes.PREPAID_BOARDING_PASS.equals(type)) {
                        // obtener detalles
                        String queryDetails = "";
                        if(ServiceTypes.GUIA_PP.equals(type)) {
                            queryDetails = QUERY_GET_GUIAPP_GUIAS_BY_ID;
                        } else if(ServiceTypes.PREPAID_BOARDING_PASS.equals(type)) {
                            queryDetails = QUERY_GET_PREPAID_BPP_RESERVATIONS_BY_ID;
                        }
                        this.dbClient.queryWithParams(queryDetails, new JsonArray().add(id), replyDetailService -> {
                            try {
                                if(replyDetailService.failed()) {
                                    throw new Exception(replyDetailService.cause());
                                }
                                List<JsonObject> resultDetailsInfo = replyDetailService.result().getRows();
                                result.put("details", resultDetailsInfo);
                                future.complete(result);
                            } catch (Exception e) {
                                e.printStackTrace();
                                future.completeExceptionally(e);
                            }
                        });

                    } else {
                        future.complete(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable ex) {
            ex.printStackTrace();
            future.completeExceptionally(ex);
        }
        return future;
    }

    private void endRegisterGlobalTimbox(Message<JsonObject> message, JsonObject invoice, JsonObject summary, JsonArray items, JsonObject queries, String code, ServiceTypes type, double totalInvoice, double ivaPercent, JsonObject globalInformation){
        System.out.println("end register global timbox");
        String query = queries.getString("queryCount");
        JsonArray params = queries.getJsonArray("paramsCount");

        // Get mayor payment method for all services
        ServicePaymentMethod mayorPaymentMethod = ServicePaymentMethod.CASH;
        Double mayorPaymentMethodCount = 0.0;
        for (ServicePaymentMethod method: ServicePaymentMethod.values()) {
            Double methodCount = summary.getDouble(method.getValue().concat("_amount_method"), 0.0);
            if (methodCount > mayorPaymentMethodCount) {
                mayorPaymentMethod = method;
                mayorPaymentMethodCount = methodCount;
            }
        }

        invoice.put("payment_method", mayorPaymentMethod.getValue());
        invoice.put("cfdi_payment_form", mayorPaymentMethod.getCode());

        this.startTransaction(message, conn -> conn.update(this.generateGenericCreate("invoice", invoice), insertReply -> {
            try {
                if (insertReply.failed()) {
                    throw new Exception(insertReply.cause());
                }

                Integer invoiceId = insertReply.result().getKeys().getInteger(0);
                invoice.put("id", invoiceId);
                System.out.println("\n-- inserted invoice --\n");
                System.out.println(invoiceId);

                // Update services
                String queryUpdate = query.replaceFirst("SELECT\\s.*\\sFROM", "UPDATE")
                        .replaceFirst("WHERE", " SET sv.invoice_id = ?, sv.invoice_is_global = 1 WHERE")
                        .replace(") as itemcount", "");
                JsonArray paramsUpdate = new JsonArray().add(invoiceId).addAll(params);

                conn.updateWithParams(queryUpdate, paramsUpdate, replyUpdate -> {
                    try {
                        if (replyUpdate.failed()) {
                            throw new Exception(replyUpdate.cause());
                        }

                        JsonObject billing = new JsonObject().put("clientCode", "999999");

                        insertGlobalTimbox(conn, code, type, invoice, billing, items, queries, totalInvoice, ivaPercent, globalInformation).whenComplete((resultInvoice, errorInvoice) -> {
                            try {
                                if (errorInvoice != null) {
                                    throw new Exception(errorInvoice);
                                }
                                this.commit(conn, message, resultInvoice);
                            } catch (Exception e) {
                                e.printStackTrace();
                                this.rollback(conn, e, message);
                            }
                        });

                    } catch(Exception ex) {
                        ex.printStackTrace();
                        this.rollback(conn, ex, message);
                    }

                });

            } catch(Exception ex) {
                // failed transaction
                ex.printStackTrace();
                this.rollback(conn, ex, message);
            }
        }));
    }

    private CompletableFuture<JsonObject> insertGlobalTimbox(SQLConnection conn, String reference, ServiceTypes type, JsonObject invoice, JsonObject billing, JsonArray details, JsonObject queries, double totalInvoice, Double ivaPercent, JsonObject globalInformation) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        System.out.println("insert global timbox");
        try {
            String query = queries.getString("queryCount");
            JsonArray params = queries.getJsonArray("paramsCount");
            String zipcode = invoice.getInteger("zip_code").toString();
            String serie = getSerieForInvoiceGlobal(invoice.getInteger("branchoffice_id"), type);
            //Creamos los datos del cfdi
            JsonObject bodyCfdi = new JsonObject()
                    .put("Folio", invoice.getInteger("id"))
                    .put("TipoDeComprobante", "I")
                    .put("Serie", serie)
                    .put("MetodoPago", invoice.getString("cfdi_payment_method"))
                    .put("FormaPago", invoice.getString("cfdi_payment_form"))
                    .put("LugarExpedicion", zipcode)
                    .put("Fecha", globalInformation.getString("invoice_date"))
                    .put("Moneda", "MXN")
                    .put("Descuento", invoice.getDouble("discount").toString())
                    .put("Periodicidad", globalInformation.getString("periodicity"))
                    .put("Meses", globalInformation.getString("gi_month"))
                    .put("Año", globalInformation.getString("gi_year"));

            //Dato emisor
            JsonObject emisor = new JsonObject()
                    .put("RegimenFiscal", "601")
                    .put("Rfc", config().getString("rfc_parcel"))
                    .put("Nombre", config().getString("rfc_name_parcel"));

            //Dato receptor
            JsonObject receptor = new JsonObject()
                    .put("Rfc" , "XAXX010101000")
                    .put("Nombre", "PUBLICO EN GENERAL")
                    .put("UsoCFDI", invoice.getString("cfdi_use"))
                    .put("RegimenFiscalReceptor", "616")
                    .put("DomicilioFiscalReceptor", zipcode);
            //Dato concepto
            JsonArray arrayConcepto = new JsonArray();

            Double subtotalSub = 0.0;
            Double subiva = 0.0;
            double totalSub = 0.0;

            for(int i = 0; i < details.size(); i++) {
                JsonObject concepto = new JsonObject();

                String code = details.getJsonObject(i).getString("code");
                Double total = details.getJsonObject(i).getDouble("total");
                Double sub = UtilsMoney.round(total / ( 1 + ivaPercent), 4);
                Double ivaConcepto = UtilsMoney.round(sub * ivaPercent, 4);

                subtotalSub = subtotalSub + sub;
                subiva = subiva + ivaConcepto;
                totalSub = totalSub + total;

                //Impuestos de los conceptos
                JsonObject impuestos = new JsonObject();
                JsonObject traslados = new JsonObject();
                traslados.put("Importe", ivaConcepto.toString())
                        .put("TipoFactor", "Tasa")
                        .put("TasaOCuota", ivaPercent.toString().concat("0000"))
                .put("Impuesto", "002")
                .put("Base", sub.toString());

                impuestos.put("Traslados", traslados);

                concepto.put("ValorUnitario", sub.toString())
                        .put("Importe", sub.toString())
                        .put("Descuento", "0")
                        .put("NoIdentificacion", code)
                        .put("Unidad", "E48")
                        .put("ClaveUnidad", "E48")
                        .put("Cantidad", "1")
                        .put("Impuestos", impuestos)
                        .put("ClaveProdServ", "01010101")
                        .put("Descripcion", "PAQUETERIA Y MENSAJERIA ".concat(code));

                if (invoice.getInteger("branchoffice_id") != null) {
                    // incluir el numero de sucursal (viene en el attr reference desde una funcion anterior)
                    concepto.put("Descripcion", concepto.getString("Descripcion").concat(" ").concat(reference));
                }
                arrayConcepto.add(concepto);
            }
            subtotalSub = UtilsMoney.round(subtotalSub, 2);
            subiva = UtilsMoney.round(subiva, 2);

            double totalFinal = UtilsMoney.round((subtotalSub + subiva), 2);

            bodyCfdi.put("Subtotal", String.valueOf(subtotalSub))
            .put("Total", String.valueOf(totalFinal));

            //Dato Impuestos
            JsonObject impuestoCFDI = new JsonObject()
                    .put("TotalImpuestosTrasladados", String.valueOf(subiva))
                    .put("Traslado", new JsonObject()
                            .put("Importe", String.valueOf(subiva))
                            .put("TipoFactor", "Tasa")
                            .put("TasaOCuota", ivaPercent.toString().concat("0000"))
                            .put("Impuesto", "002")
                            .put("Base", subtotalSub.toString()));

            bodyCfdi.put("Emisor", emisor)
                    .put("Receptor", receptor)
                    .put("Conceptos", arrayConcepto)
                    .put("Impuestos", impuestoCFDI)
            .put("invoice", invoice);

            String querySelect = query.replaceFirst("SELECT\\s.*\\sFROM", "SELECT sv.id FROM")
                    .replace(") as itemcount", "");
            JsonArray paramsSelect = new JsonArray().addAll(params);
            conn.queryWithParams(querySelect, paramsSelect, replySelect -> {
                try {
                    if (replySelect.failed()) {
                        throw new Exception(replySelect.cause());
                    }

                    JsonArray insertArray = new JsonArray();
                    List<JsonObject> responseSelect = replySelect.result().getRows();
                    responseSelect.forEach( v ->  {
                        String insert = "";
                        Integer id = v.getInteger("id");
                        if (ServiceTypes.PARCEL.equals(type)) {
                            insert = "INSERT INTO parcel_invoice_complement ( status_cfdi, tipo_cfdi, id_parcel, created_by) values('en proceso' , 'factura global',".concat(id.toString()).concat(", ").concat(invoice.getInteger("created_by").toString()).concat(")");
                        } else if(ServiceTypes.GUIA_PP.equals(type)) {
                            insert = "INSERT INTO guiapp_invoice_complement ( status_cfdi, tipo_cfdi, id_prepaid, created_by) values('en proceso' , 'factura global',".concat(id.toString()).concat(", ").concat(invoice.getInteger("created_by").toString()).concat(")");
                        }
                        insertArray.add(insert);
                    });

                    if(insertArray.size() > 0) {
                        List<String> insertsStream = insertArray.getList();
                        conn.batch(insertsStream, (AsyncResult<List<Integer>> replyBatch ) -> {
                           try {
                               if(replyBatch.failed()) {
                                   throw new Exception(replyBatch.cause());
                               }

                               bodyCfdi.put("array_of_ids", responseSelect);
                               future.complete(bodyCfdi);
                           } catch (Exception ex) {
                               ex.printStackTrace();
                               future.completeExceptionally(replyBatch.cause());
                           }
                        });
                    } else {
                        future.completeExceptionally(replySelect.cause());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    private void updateCCPStatus(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String xml = body.getString("xml");
            String xmlNoQuotes = xml.substring(1, xml.length() -1);
            String status = "";
            String invoiceUUID = body.getString("uuid");
            String uuid = "";
            String QUERY = "";

            if(invoiceUUID != null) {
                uuid = invoiceUUID;
            }

            switch(body.getString("type")) {
                case "travel_log":
                    QUERY = "update travel_logs_ccp set invoice_status = ?";
                    break;
                case "EAD/RAD":
                    QUERY = "update parcels_manifest_ccp set invoice_status = ?";
                    break;
            }

            JsonArray params = new JsonArray();
            if(body.getJsonObject("cfdiResult").getString("error") != null) {
                status = "Error timbrado";
                params.add(status);
            } else {
                status = "timbrado";
                QUERY += ", xml = ?";
                QUERY += ", uuid = ?";
                params.add(status).add(xmlNoQuotes).add(uuid);
            }
            QUERY += " WHERE id = ?";

            switch(body.getString("type")) {
                case "travel_log":
                    params.add(body.getInteger("travel_logs_ccp_id"));
                    break;
                case "EAD/RAD":
                    params.add(body.getInteger("parcels_manifest_ccp_id"));
                    break;
            }

            String finalQUERY = QUERY;
            this.startTransaction(message, conn -> {
                conn.queryWithParams(finalQUERY, params, replyUpdate -> {
                    try {
                        if(replyUpdate.succeeded()) {
                            this.commit(conn, message, new JsonObject());
                        }
                         else {
                            throw new Exception(replyUpdate.cause());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.rollback(conn, e, message);
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void getInvoicesForPaymentComplement(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonArray uuids = body.getJsonArray("uuids");
            JsonArray params = new JsonArray();

            String QUERY = QUERY_GET_INVOICES_FOR_PAYMENT_COMPLEMENT;

            if(uuids != null) {
                StringBuilder placeholders = new StringBuilder();
                for(int i = 0 ; i < uuids.size() ; i++ ){
                    String uuid = uuids.getString(i);
                    params.add(uuid);
                    placeholders.append("?");
                    if (i < uuids.size() - 1) {
                        placeholders.append(",");
                    }
                }
                QUERY +=" AND i.uuid IN (" + placeholders +")";
            }

            this.dbClient.queryWithParams(QUERY, params, reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> invoices = reply.result().getRows();
                    JsonObject result = new JsonObject()
                            .put("invoices", invoices);
                    message.reply(result);
                }catch (Exception e){
                    e.printStackTrace();
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private Future<JsonArray> queryAsyncWithFuture(String query, JsonArray params) {
        Future<JsonArray> future = Future.future();
        this.dbClient.queryWithParams(query, params, res -> {
            if (res.succeeded()) {
                List<JsonObject> rows = res.result().getRows();
                if (rows == null || rows.isEmpty() || rows.stream().allMatch(JsonObject::isEmpty)) {
                    future.complete(new JsonArray());
                } else {
                    JsonArray jsonArray = new JsonArray(rows);
                    future.complete(jsonArray);
                }
            } else {
                future.fail(res.cause());
            }
        });
        return future;
    }

    private String getSerieForInvoice(ServiceTypes type, String paymentCondition, String customerParcelType) {
        if (customerParcelType == null || customerParcelType.isEmpty()) {
            return PG_SERIE;
        }
        if (type == null) {
            throw new IllegalArgumentException("tipo de servicio no encontrado");
        }
        return ServiceSeries.resolve(type, paymentCondition);
    }

    private String getSerieForInvoiceGlobal(Integer branchofficeId, ServiceTypes type) {
        if (type == null) {
            throw new IllegalArgumentException("ServiceTypes no puede ser null");
        }

        if (branchofficeId != null) {
            return PG_SERIE;
        }

        try {
            return ServiceSeries.resolve(type, "global");
        } catch (IllegalArgumentException iae) {
            throw iae;
        }
    }

    // START: CODE BLOCK FOR ADMIN CFDI MODULE
    private void searchAdvancedCFDI(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            // ----------- INPUTS -----------
            String cfdiType = normalize(body.getString("cfdi_type"), "all"); // invoice | payment_complement | credit_note | all
            String serviceType = normalize(body.getString("service_type"), "all"); // all | parcel | guia_pp | global (global solo con invoice)
            String dateType = body.getString("filter_by_date");
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");

            String cfdiSerie = body.getString("cfdi_serie"); // solo invoice
            String cfdiFolio = body.getString("cfdi_folio"); // por tabla
            String cfdiUuid  = body.getString("cfdi_uuid");  // por tabla

            String cfdiPaymentForm   = body.getString("cfdi_payment_form");   // cash, card, transfer, etc.
            String cfdiPaymentMethod = body.getString("cfdi_payment_method"); // PUE | PPD (solo invoice)

            Integer customerId = body.getInteger("customer_id");
            if (customerId == null) customerId = body.getInteger("customer");
            String cbiRfc = body.getString("cbi_rfc");

            boolean ignoreDates = notBlank(cfdiUuid);

            Integer limit = body.getInteger(LIMIT) != null ? body.getInteger(LIMIT) : 50;
            Integer page  = body.getInteger(PAGE)  != null ? body.getInteger(PAGE)  : 1;
            int offset = (page - 1) * limit;

            // invoice: created_at | invoice_date
            // payment_complement: created_at | payment_date
            // credit_note: created_at | issue_date
            // all: solo created_at en cada tabla
            String invDateCol = "created_at";
            String pcDateCol  = "created_at";
            String cnDateCol  = "created_at";

            if ("invoice".equals(cfdiType)) {
                invDateCol = "invoice_date".equals(dateType) ? "invoice_date" : "created_at";
            } else if ("payment_complement".equals(cfdiType)) {
                pcDateCol  = "payment_date".equals(dateType) ? "payment_date" : "created_at";
            } else if ("credit_note".equals(cfdiType)) {
                cnDateCol  = "issue_date".equals(dateType) ? "issue_date" : "created_at";
            } else {
                dateType = "created_at";
            }

            // INVOICE
            final String BASE_SELECT_INV =
                    "SELECT " +
                            "  'invoice' AS cfdi_type, " +
                            "  inv.id AS id, " +
                            "  inv.uuid AS uuid, " +
                            "  inv.cfdi_serie AS serie, " +
                            "  inv.cfdi_folio AS folio, " +
                            "  ? AS date_type, " +
                            "  inv." + invDateCol + " AS date_value, " +
                            "  inv.created_at, " +
                            "  CASE " +
                            "    WHEN inv.service_type = 'parcel'  THEN ip.customer_id " +
                            "    WHEN inv.service_type = 'guia_pp' THEN ig.customer_id " +
                            "    ELSE NULL " +
                            "  END AS customer_id, " +
                            "  CONCAT(c.first_name, ' ', c.last_name) AS customer_name, " +
                            "  cbi.name AS cbi_name, " +
                            "  cbi.rfc AS cbi_rfc, " +
                            "  inv.email AS email, " +
                            "  cbi.zip_code AS cbi_zip_code, " +
                            "  cbi.c_UsoCFDI_id AS c_UsoCFDI_id, " +
                            "  cbi.c_RegimenFiscal_id AS c_RegimenFiscal_id, " +
                            "  uso.c_UsoCFDI AS c_UsoCFDI, " +
                            "  reg.c_RegimenFiscal AS c_RegimenFiscal, " +
                            "  inv.customer_billing_information_id AS customer_billing_information_id, " +
                            "  inv.total_amount AS total_amount, " +
                            "  inv.available_subtotal_for_complement AS available_subtotal_for_complement, " +
                            "  inv.available_iva_for_complement AS available_iva_for_complement, " +
                            "  inv.available_iva_withheld_for_complement AS available_iva_withheld_for_complement, " +
                            "  inv.available_amount_for_complement AS available_amount_for_complement, " +
                            "  inv.iva AS iva_amount, " +
                            "  inv.iva_withheld AS iva_withheld_amount, " +
                            "  inv.status AS status, " +
                            "  inv.service_type AS service_type, " +
                            "  inv.is_global AS is_global, " +
                            "  inv.reference AS invoice_reference, " +
                            "  inv.payment_method AS cfdi_payment_form, " +
                            "  inv.cfdi_payment_method AS cfdi_payment_method " +
                            "FROM invoice inv " +
                            "LEFT JOIN customer_billing_information cbi ON cbi.id = inv.customer_billing_information_id " +
                            "LEFT JOIN c_UsoCFDI uso ON uso.id = cbi.c_UsoCFDI_id " +
                            "LEFT JOIN c_RegimenFiscal reg ON reg.id = cbi.c_RegimenFiscal_id " +
                            "LEFT JOIN (SELECT invoice_id, MIN(customer_id) AS customer_id FROM parcels GROUP BY invoice_id) ip ON ip.invoice_id = inv.id " +
                            "LEFT JOIN (SELECT invoice_id, MIN(customer_id) AS customer_id FROM parcels_prepaid GROUP BY invoice_id) ig ON ig.invoice_id = inv.id " +
                            "LEFT JOIN customer c ON c.id = COALESCE(ip.customer_id, ig.customer_id) ";

            final String BASE_COUNT_INV =
                            "SELECT " +
                            " COUNT(inv.id) AS count, " +
                            " COALESCE(SUM(inv.total_amount), 0) AS total, " +
                            " COALESCE(SUM(inv.available_amount_for_complement), 0) AS available_amount_for_complement_total " +
                            "FROM invoice inv " +
                            "LEFT JOIN customer_billing_information cbi ON cbi.id = inv.customer_billing_information_id " +
                            "LEFT JOIN (SELECT invoice_id, MIN(customer_id) AS customer_id FROM parcels GROUP BY invoice_id) ip ON ip.invoice_id = inv.id " +
                            "LEFT JOIN (SELECT invoice_id, MIN(customer_id) AS customer_id FROM parcels_prepaid GROUP BY invoice_id) ig ON ig.invoice_id = inv.id ";

            // PAYMENT COMPLEMENT
            final String BASE_SELECT_PC =
                    "SELECT " +
                            "  'payment_complement' AS cfdi_type, " +
                            "  pc.id AS id, " +
                            "  pc.uuid AS uuid, " +
                            "  pc.serie AS serie, " +
                            "  pc.folio AS folio, " +
                            "  ? AS date_type, " +
                            "  pc." + pcDateCol + " AS date_value, " +
                            "  pc.created_at, " +
                            "  pc.customer_id AS customer_id, " +
                            "  CONCAT(c.first_name, ' ', c.last_name) AS customer_name, " +
                            "  cbi.name AS cbi_name, " +
                            "  cbi.rfc AS cbi_rfc, " +
                            "  pc.email AS email, " +
                            "  cbi.zip_code AS cbi_zip_code, " +
                            "  cbi.c_UsoCFDI_id AS c_UsoCFDI_id, " +
                            "  cbi.c_RegimenFiscal_id AS c_RegimenFiscal_id, " +
                            "  uso.c_UsoCFDI AS c_UsoCFDI, " +
                            "  reg.c_RegimenFiscal AS c_RegimenFiscal, " +
                            "  pc.customer_billing_information_id AS customer_billing_information_id, " +
                            "  COALESCE(pcs.total_paid, 0) AS total_amount, " +
                            "  NULL AS available_subtotal_for_complement, " +
                            "  NULL AS available_iva_for_complement, " +
                            "  NULL AS available_iva_withheld_for_complement, " +
                            "  NULL AS available_amount_for_complement, " +
                            "  pc.iva_amount AS iva_amount, " +
                            "  pc.iva_withheld_amount AS iva_withheld_amount, " +
                            "  pc.status AS status, " +
                            "  NULL AS service_type, " +
                            "  NULL AS is_global, " +
                            "  NULL AS invoice_reference, " +
                            "  paymin.payment_method AS cfdi_payment_form, " +
                            "  NULL AS cfdi_payment_method " +
                            "FROM payment_complement pc " +
                            "LEFT JOIN customer_billing_information cbi ON cbi.id = pc.customer_billing_information_id " +
                            "LEFT JOIN c_UsoCFDI uso ON uso.id = cbi.c_UsoCFDI_id " +
                            "LEFT JOIN c_RegimenFiscal reg ON reg.id = cbi.c_RegimenFiscal_id " +
                            "LEFT JOIN customer c ON c.id = pc.customer_id " +
                            "LEFT JOIN ( " +
                            "   SELECT payment_complement_id, SUM(paid_amount) AS total_paid " +
                            "   FROM payment_complement_detail WHERE status = 1 GROUP BY payment_complement_id " +
                            ") pcs ON pcs.payment_complement_id = pc.id " +
                            "LEFT JOIN ( " +
                            "   SELECT pcp.payment_complement_id, MIN(pay.payment_method) AS payment_method " +
                            "   FROM payment_complement_payment pcp " +
                            "   JOIN payment pay ON pay.id = pcp.payment_id " +
                            "   WHERE pay.status = 1 " +
                            "   GROUP BY pcp.payment_complement_id " +
                            ") paymin ON paymin.payment_complement_id = pc.id " +
                            "LEFT JOIN payment_complement_detail pcd ON pcd.payment_complement_id = pc.id AND pcd.status = 1 " +
                            "LEFT JOIN invoice inv ON inv.id = pcd.invoice_id ";

            final String BASE_COUNT_PC =
                    "SELECT COUNT(DISTINCT pc.id) AS count, COALESCE(SUM(pcs.total_paid), 0) AS total " +
                            "FROM payment_complement pc " +
                            "LEFT JOIN customer_billing_information cbi ON cbi.id = pc.customer_billing_information_id " +
                            "LEFT JOIN ( " +
                            "   SELECT payment_complement_id, SUM(paid_amount) AS total_paid " +
                            "   FROM payment_complement_detail WHERE status = 1 GROUP BY payment_complement_id " +
                            ") pcs ON pcs.payment_complement_id = pc.id " +
                            "LEFT JOIN payment_complement_detail pcd ON pcd.payment_complement_id = pc.id AND pcd.status = 1 " +
                            "LEFT JOIN invoice inv ON inv.id = pcd.invoice_id ";

            // CREDIT NOTE
            final String BASE_SELECT_CN =
                    "SELECT " +
                            "  'credit_note' AS cfdi_type, " +
                            "  cn.id AS id, " +
                            "  cn.uuid AS uuid, " +
                            "  cn.serie AS serie, " +
                            "  cn.folio AS folio, " +
                            "  ? AS date_type, " +
                            "  cn." + cnDateCol + " AS date_value, " +
                            "  cn.created_at, " +
                            "  cn.customer_id AS customer_id, " +
                            "  CONCAT(c.first_name, ' ', c.last_name) AS customer_name, " +
                            "  cbi.name AS cbi_name, " +
                            "  cbi.rfc AS cbi_rfc, " +
                            "  cn.email AS email, " +
                            "  cbi.zip_code AS cbi_zip_code, " +
                            "  cbi.c_UsoCFDI_id AS c_UsoCFDI_id, " +
                            "  cbi.c_RegimenFiscal_id AS c_RegimenFiscal_id, " +
                            "  uso.c_UsoCFDI AS c_UsoCFDI, " +
                            "  reg.c_RegimenFiscal AS c_RegimenFiscal, " +
                            "  cn.customer_billing_information_id AS customer_billing_information_id, " +
                            "  COALESCE(cns.total_amount, 0) AS total_amount, " +
                            "  NULL AS available_subtotal_for_complement, " +
                            "  NULL AS available_iva_for_complement, " +
                            "  NULL AS available_iva_withheld_for_complement, " +
                            "  NULL AS available_amount_for_complement, " +
                            "  cn.iva_amount AS iva_amount, " +
                            "  cn.iva_withheld_amount AS iva_withheld_amount, " +
                            "  cn.status AS status, " +
                            "  NULL AS service_type, " +
                            "  NULL AS is_global, " +
                            "  NULL AS invoice_reference, " +
                            "  invpm.payment_method AS cfdi_payment_form, " +
                            "  NULL AS cfdi_payment_method " +
                            "FROM credit_note cn " +
                            "LEFT JOIN customer_billing_information cbi ON cbi.id = cn.customer_billing_information_id " +
                            "LEFT JOIN c_UsoCFDI uso ON uso.id = cbi.c_UsoCFDI_id " +
                            "LEFT JOIN c_RegimenFiscal reg ON reg.id = cbi.c_RegimenFiscal_id " +
                            "LEFT JOIN customer c ON c.id = cn.customer_id " +
                            "LEFT JOIN ( " +
                            "   SELECT credit_note_id, SUM(total_amount) AS total_amount " +
                            "   FROM credit_note_detail WHERE status = 1 GROUP BY credit_note_id " +
                            ") cns ON cns.credit_note_id = cn.id " +
                            "LEFT JOIN credit_note_detail cnd ON cnd.credit_note_id = cn.id AND cnd.status = 1 " +
                            "LEFT JOIN invoice inv ON inv.id = cnd.invoice_id " +
                            "LEFT JOIN ( " +
                            "   SELECT cnd2.credit_note_id, MIN(inv2.payment_method) AS payment_method " +
                            "   FROM credit_note_detail cnd2 " +
                            "   JOIN invoice inv2 ON inv2.id = cnd2.invoice_id " +
                            "   WHERE cnd2.status = 1 " +
                            "   GROUP BY cnd2.credit_note_id " +
                            ") invpm ON invpm.credit_note_id = cn.id ";

            final String BASE_COUNT_CN =
                    "SELECT COUNT(*) AS count, COALESCE(SUM(cns.total_amount), 0) AS total " +
                            "FROM credit_note cn " +
                            "LEFT JOIN customer_billing_information cbi ON cbi.id = cn.customer_billing_information_id " +
                            "LEFT JOIN ( " +
                            "   SELECT credit_note_id, SUM(total_amount) AS total_amount " +
                            "   FROM credit_note_detail WHERE status = 1 GROUP BY credit_note_id " +
                            ") cns ON cns.credit_note_id = cn.id ";

            // INVOICE
            FilterBuilder fbInv = new FilterBuilder();
            if (!ignoreDates) fbInv.andDateBetween("inv." + invDateCol, initDate, endDate);
            fbInv.andEq("inv.invoice_status", "done")
                .andEq("inv.uuid", cfdiUuid)
                .andEq("inv.cfdi_folio", cfdiFolio)
                .andUpperEq("cbi.rfc", cbiRfc);

            // service_type para invoice (incluye global)
            if ("global".equals(serviceType) && !"all".equals(cfdiType)) {
                fbInv.andRaw("inv.is_global = 1");
            } else if ("parcel".equals(serviceType)) {
                fbInv.andRaw("inv.service_type = 'parcel' AND inv.is_global = 0");
            } else if ("guia_pp".equals(serviceType)) {
                fbInv.andRaw("inv.service_type = 'guia_pp' AND inv.is_global = 0");
            }
            // cfdi_serie (solo invoice)
            if ("invoice".equals(cfdiType) && notBlank(cfdiSerie)) {
                fbInv.andEq("inv.cfdi_serie", cfdiSerie);
            }
            // customer (mapea desde parcels / parcels_prepaid)
            if (customerId != null) {
                fbInv.andRaw("( (inv.service_type = 'parcel' AND ip.customer_id = ?) OR " +
                                "  (inv.service_type = 'guia_pp' AND ig.customer_id = ?) )",
                        customerId, customerId);
            }
            // forma / método de pago
            if (notBlank(cfdiPaymentForm))   fbInv.andEq("inv.payment_method", cfdiPaymentForm);
            if (notBlank(cfdiPaymentMethod)) fbInv.andEq("inv.cfdi_payment_method", cfdiPaymentMethod);

            // PAYMENT COMPLEMENT
            FilterBuilder fbPc = new FilterBuilder();
            if (!ignoreDates) fbPc.andDateBetween("pc." + pcDateCol, initDate, endDate);
            fbPc.andEq("pc.invoice_status", "done")
                .andEq("pc.uuid", cfdiUuid)
                .andEq("pc.folio", cfdiFolio)
                .andUpperEq("cbi.rfc", cbiRfc);

            // service_type para PC (por invoices relacionados). 'global' no aplica aquí por reglas.
            if ("parcel".equals(serviceType))  fbPc.andRaw("inv.service_type = 'parcel'");
            if ("guia_pp".equals(serviceType)) fbPc.andRaw("inv.service_type = 'guia_pp'");
            // customer directo en PC
            if (customerId != null) fbPc.andEq("pc.customer_id", customerId);
            // forma de pago via EXISTS en payments
            if (notBlank(cfdiPaymentForm)) {
                fbPc.andExists(
                        "SELECT 1 FROM payment_complement_payment pcp " +
                                "JOIN payment pay ON pay.id = pcp.payment_id " +
                                "WHERE pcp.payment_complement_id = pc.id " +
                                "  AND pay.status = 1 " +
                                "  AND pay.payment_method = ?",
                        cfdiPaymentForm
                );
            }

            // CREDIT NOTE
            FilterBuilder fbCn = new FilterBuilder();
            if (!ignoreDates) fbCn.andDateBetween("cn." + cnDateCol, initDate, endDate);
            fbCn.andDateBetween("cn." + cnDateCol, initDate, endDate)
                .andEq("cn.invoice_status", "done")
                .andEq("cn.uuid", cfdiUuid)
                .andEq("cn.folio", cfdiFolio)
                .andUpperEq("cbi.rfc", cbiRfc);

            // service_type para CN (por invoices relacionados). 'global' no aplica aquí.
            if ("parcel".equals(serviceType)) {
                fbCn.andExists(
                        "SELECT 1 " +
                                "FROM credit_note_detail d " +
                                "JOIN invoice i ON i.id = d.invoice_id " +
                                "WHERE d.credit_note_id = cn.id " +
                                "  AND d.status = 1 " +
                                "  AND i.service_type = 'parcel'"
                );
            }
            if ("guia_pp".equals(serviceType)) {
                fbCn.andExists(
                        "SELECT 1 " +
                                "FROM credit_note_detail d " +
                                "JOIN invoice i ON i.id = d.invoice_id " +
                                "WHERE d.credit_note_id = cn.id " +
                                "  AND d.status = 1 " +
                                "  AND i.service_type = 'guia_pp'"
                );
            }
            // customer directo en CN
            if (customerId != null) fbCn.andEq("cn.customer_id", customerId);
            // forma de pago via EXISTS contra invoices afectados
            if (notBlank(cfdiPaymentForm)) {
                fbCn.andExists(
                        "SELECT 1 FROM credit_note_detail cnd2 " +
                                "JOIN invoice inv2 ON inv2.id = cnd2.invoice_id " +
                                "WHERE cnd2.credit_note_id = cn.id " +
                                "  AND cnd2.status = 1 " +
                                "  AND inv2.payment_method = ?",
                        cfdiPaymentForm
                );
            }

            // SELECT por tipo (añadimos ORDER/GROUP y paginación)
            final String ORDER_INV = " ORDER BY inv." + invDateCol + " DESC, inv.id DESC ";
            final String ORDER_PC  = " GROUP BY pc.id ORDER BY pc." + pcDateCol + " DESC, pc.id DESC ";
            final String ORDER_CN  = " GROUP BY cn.id ORDER BY cn." + cnDateCol + " DESC, cn.id DESC ";

            String sqlInvSelect = BASE_SELECT_INV + fbInv.whereSql() + ORDER_INV;
            JsonArray paramsInvSelect = new JsonArray()
                    .add(dateTypeLabel("invoice", invDateCol))
                    .addAll(fbInv.whereParams());

            String sqlPcSelect = BASE_SELECT_PC + fbPc.whereSql() + ORDER_PC;
            JsonArray paramsPcSelect = new JsonArray()
                    .add(dateTypeLabel("payment_complement", pcDateCol))
                    .addAll(fbPc.whereParams());

            String sqlCnSelect = BASE_SELECT_CN + fbCn.whereSql() + ORDER_CN;
            JsonArray paramsCnSelect = new JsonArray()
                    .add(dateTypeLabel("credit_note", cnDateCol))
                    .addAll(fbCn.whereParams());

            String sqlInvCount = BASE_COUNT_INV + fbInv.whereSql();
            JsonArray paramsInvCount = fbInv.whereParams();

            String sqlPcCount  = BASE_COUNT_PC  + fbPc.whereSql();
            JsonArray paramsPcCount  = fbPc.whereParams();

            String sqlCnCount  = BASE_COUNT_CN  + fbCn.whereSql();
            JsonArray paramsCnCount  = fbCn.whereParams();

            List<Future> tasks = new ArrayList<>();
            Future fResults = Future.future();

            if ("all".equals(cfdiType)) {
                String union =
                        "SELECT * FROM ( " + sqlInvSelect + " ) inv_u " +
                                "UNION ALL " +
                                "SELECT * FROM ( " + sqlPcSelect + " ) pc_u " +
                                "UNION ALL " +
                                "SELECT * FROM ( " + sqlCnSelect + " ) cn_u " +
                                "ORDER BY date_value DESC, id DESC " +
                                "LIMIT ? OFFSET ?";

                JsonArray unionParams = new JsonArray();
                paramsInvSelect.forEach(unionParams::add);
                paramsPcSelect.forEach(unionParams::add);
                paramsCnSelect.forEach(unionParams::add);
                unionParams.add(limit).add(offset);

                Future fCInv = Future.future();
                Future fCPc  = Future.future();
                Future fCCn  = Future.future();
                this.dbClient.queryWithParams(sqlInvCount, paramsInvCount, fCInv.completer());
                this.dbClient.queryWithParams(sqlPcCount,  paramsPcCount,  fCPc.completer());
                this.dbClient.queryWithParams(sqlCnCount,  paramsCnCount,  fCCn.completer());

                tasks.add(fCInv); tasks.add(fCPc); tasks.add(fCCn);

                this.dbClient.queryWithParams(union, unionParams, fResults.completer());
                tasks.add(fResults);

                CompositeFuture.all(tasks).setHandler(reply -> {
                    if (reply.failed()) { reportQueryError(message, reply.cause()); return; }

                    ResultSet rsInv = reply.result().resultAt(0);
                    ResultSet rsPc  = reply.result().resultAt(1);
                    ResultSet rsCn  = reply.result().resultAt(2);

                    long countInv   = rsInv.getRows().get(0).getLong("count");
                    double totalInv = rsInv.getRows().get(0).getDouble("total");
                    Double invAvailObj = rsInv.getRows().get(0).getDouble("available_amount_for_complement_total");
                    double availableInvTotal = invAvailObj != null ? invAvailObj : 0.0;

                    long countPc    = rsPc.getRows().get(0).getLong("count");
                    double totalPc  = rsPc.getRows().get(0).getDouble("total");

                    long countCn    = rsCn.getRows().get(0).getLong("count");
                    double totalCn  = rsCn.getRows().get(0).getDouble("total");

                    List<JsonObject> rows = reply.result().<ResultSet>resultAt(3).getRows();

                    JsonObject summary = new JsonObject()
                            .put("invoice", new JsonObject()
                                    .put("count", countInv)
                                    .put("total", totalInv)
                                    .put("available_amount_for_complement_total", availableInvTotal))
                            .put("payment_complement", new JsonObject().put("count", countPc).put("total", totalPc))
                            .put("credit_note", new JsonObject().put("count", countCn).put("total", totalCn));

                    JsonObject out = new JsonObject()
                            .put("count", countInv + countPc + countCn)
                            .put("summary", summary)
                            .put("items", rows.size())
                            .put("results", rows);

                    message.reply(out);
                });

            } else {
                // Un solo tipo
                String dataQuery;
                JsonArray dataParams;
                String orderAndLimit;
                String countQuery;
                JsonArray countParams;

                switch (cfdiType) {
                    case "invoice":
                        dataQuery = sqlInvSelect;
                        dataParams = paramsInvSelect.copy().add(limit).add(offset);
                        orderAndLimit = ""; // ya incluido ORDER en sqlInvSelect
                        countQuery = sqlInvCount;
                        countParams = paramsInvCount;
                        break;
                    case "payment_complement":
                        dataQuery = sqlPcSelect;
                        dataParams = paramsPcSelect.copy().add(limit).add(offset);
                        orderAndLimit = "";
                        countQuery = sqlPcCount;
                        countParams = paramsPcCount;
                        break;
                    case "credit_note":
                        dataQuery = sqlCnSelect;
                        dataParams = paramsCnSelect.copy().add(limit).add(offset);
                        orderAndLimit = "";
                        countQuery = sqlCnCount;
                        countParams = paramsCnCount;
                        break;
                    default:
                        message.fail(400, "cfdi_type inválido");
                        return;
                }

                Future fCount = Future.future();
                this.dbClient.queryWithParams(countQuery, countParams, fCount.completer());
                tasks.add(fCount);

                String finalQuery = dataQuery + " LIMIT ? OFFSET ? ";
                this.dbClient.queryWithParams(finalQuery, dataParams, fResults.completer());
                tasks.add(fResults);

                CompositeFuture.all(tasks).setHandler(reply -> {
                    if (reply.failed()) { reportQueryError(message, reply.cause()); return; }

                    ResultSet rsc = reply.result().resultAt(0);
                    long count = rsc.getRows().get(0).getLong("count");
                    double total = rsc.getRows().get(0).getDouble("total");
                    Double invAvailObj = rsc.getRows().get(0).getDouble("available_amount_for_complement_total");
                    double availableInvTotal = invAvailObj != null ? invAvailObj : 0.0;

                    JsonObject summary = new JsonObject()
                            .put(cfdiType, new JsonObject()
                                    .put("count", count)
                                    .put("total", total)
                                    .put("available_amount_for_complement_total", availableInvTotal));

                    List<JsonObject> rows = reply.result().<ResultSet>resultAt(1).getRows();
                    JsonObject out = new JsonObject()
                            .put("count", count)
                            .put("summary", summary)
                            .put("items", rows.size())
                            .put("results", rows);

                    message.reply(out);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private static final class FilterBuilder {
        private final StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        private final List<Object> params = new ArrayList<>();

        public FilterBuilder andDateBetween(String qualifiedCol, String init, String end) {
            if (notBlank(init) && notBlank(end)) {
                where.append(" AND ").append(qualifiedCol).append(" BETWEEN ? AND ? ");
                params.add(init);
                params.add(end);
            }
            return this;
        }

        public FilterBuilder andEq(String qualifiedCol, Object value) {
            if (value != null && !(value instanceof String && ((String) value).trim().isEmpty())) {
                where.append(" AND ").append(qualifiedCol).append(" = ? ");
                params.add(value);
            }
            return this;
        }

        public FilterBuilder andUpperEq(String qualifiedCol, String value) {
            if (notBlank(value)) {
                where.append(" AND UPPER(").append(qualifiedCol).append(") = UPPER(?) ");
                params.add(value);
            }
            return this;
        }

        public FilterBuilder andExists(String existsSql, Object... values) {
            if (notBlank(existsSql)) {
                where.append(" AND EXISTS ( ").append(existsSql).append(" ) ");
                if (values != null) Collections.addAll(params, values);
            }
            return this;
        }

        public FilterBuilder andRaw(String raw, Object... values) {
            if (notBlank(raw)) {
                where.append(" AND ").append(raw).append(" ");
                if (values != null) Collections.addAll(params, values);
            }
            return this;
        }

        public String whereSql() {
            return where.toString();
        }

        public JsonArray whereParams() {
            JsonArray a = new JsonArray();
            params.forEach(a::add);
            return a;
        }
    }

    private static String normalize(String v, String def) {
        return (v == null || v.trim().isEmpty()) ? def : v;
    }
    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
    private static String dateTypeLabel(String type, String chosenCol) {
        if ("invoice".equals(type)) return "invoice_date".equals(chosenCol) ? "invoice_date" : "created_at";
        if ("payment_complement".equals(type)) return "payment_date".equals(chosenCol) ? "payment_date" : "created_at";
        if ("credit_note".equals(type)) return "issue_date".equals(chosenCol) ? "issue_date" : "created_at";
        return "created_at";
    }
    // END: CODE BLOCK FOR ADMIN CFDI MODULE

    private void getServicesByInvoice(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonArray ids = body.getJsonArray("ids");
            JsonArray params = new JsonArray();

            String QUERY = QUERY_GET_SERVICES_BY_INVOICE;

            if (ids != null && !ids.isEmpty()) {
                StringBuilder placeholders = new StringBuilder();
                for (int i = 0; i < ids.size(); i++) {
                    params.add(ids.getInteger(i));
                    placeholders.append("?");
                    if (i < ids.size() - 1) placeholders.append(",");
                }
                QUERY += " AND invoice_id IN (" + placeholders + ")";
            }
            QUERY += " ORDER BY invoice_id, tracking_code";

            this.dbClient.queryWithParams(QUERY, params, reply ->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> services = reply.result().getRows();
                    JsonObject result = new JsonObject()
                            .put("services", services);
                    message.reply(result);
                }catch (Exception e){
                    e.printStackTrace();
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void assignCustomerBillingInfo(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String typeStr = body.getString("type");
            Integer cbiId = body.getInteger("customer_billing_information_id");
            Integer serviceId = body.getInteger("service_id");

            ServiceTypes serviceType = ServiceTypes.getTypeByServiceName(typeStr);
            if (serviceType == null) {
                throw new IllegalArgumentException("Tipo de servicio no soportado: " + typeStr);
            }

            JsonObject history = new JsonObject()
                    .put("parcel_id", serviceType.getType().equals(ServiceTypes.PARCEL.getType()) ? serviceId : null)
                    .put("parcel_prepaid_id", serviceType.getType().equals(ServiceTypes.GUIA_PP.getType()) ? serviceId : null)
                    .put("user_id", body.getInteger(USER_ID, null))
                    .put("prev_customer_billing_information_id", body.getInteger("prev_customer_billing_information_id", null))
                    .put("new_customer_billing_information_id", cbiId)
                    .put("prev_rfc", body.getString("prev_rfc", null))
                    .put("new_rfc", body.getString("new_rfc", null))
                    .put("move_type", body.getString("move_type", null));

            String insertHistory = this.generateGenericCreate("cbi_assignment_history", history);


            // Query de actualización
            final String sql = "UPDATE " + serviceType.getTable() + " SET customer_billing_information_id = ?, updated_by = ?, updated_at = ? WHERE id = ?";

            startTransaction(message, conn -> {
                try {
                    JsonArray params = new JsonArray()
                            .add(cbiId)
                            .add(body.getInteger(USER_ID))
                            .add(UtilsDate.sdfDataBase(new Date()))
                            .add(serviceId);

                            conn.update(insertHistory, replyInsertHistory -> {
                                try {
                                    if (replyInsertHistory.failed()) {
                                        throw replyInsertHistory.cause();
                                    }

                                    conn.updateWithParams(sql, params, ar -> {
                                        try {
                                            if (ar.failed()) {
                                                throw ar.cause();
                                            }
                                            this.commit(conn, message, new JsonObject());
                                        } catch (Throwable t) {
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
                    this.rollback(conn, t, message);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private static final String PATH_INVOICING = "/api/facturacontroller/post";

    private static final String QUERY_INVOICE_BY_ID = "SELECT * FROM invoice WHERE id = ? ";

    private static final String QUERY_UPDATE_INVOICE_BY_CONTPAQ_ID_WITH_DOCUMENTS = "UPDATE invoice SET invoice_status = 'done', \n" +
        "media_document_pdf_name = ?, media_document_xml_name = ?, document_id = ? WHERE {FIELD_CONTPAQ_ID} = ? AND invoice_status != 'done' AND document_id IS NULL ";

    private static final String QUERY_UPDATE_INVOICE_BY_CONTPAQ_ID_WITHOUT_DOCUMENTS = "UPDATE invoice SET invoice_status = 'done', \n" +
        "document_id = ? WHERE {FIELD_CONTPAQ_ID} = ? AND invoice_status != 'done' AND document_id IS NULL ";

    private static final String QUERY_CUSTOMER_BILLING_INFORMATION = "SELECT cbi.*, \n" +
            "str.name as street_name\n" +
            "FROM customer_billing_information cbi\n" +
            "LEFT JOIN street str ON cbi.street_id = str.id\n" +
            "WHERE cbi.id = ? AND cbi.status != 3";

    private static final String QUERY_SERVICE_GET_PAYMENTS = "SELECT COALESCE(SUM(py.amount), 0) AS total, \n" +
            "bo.zip_code, py.payment_method\n" +
            "FROM payment AS py\n" +
            "LEFT JOIN tickets AS tk ON tk.id = py.ticket_id\n" +
            "LEFT JOIN cash_out AS co ON co.id = tk.cash_out_id\n" +
            "LEFT JOIN branchoffice AS bo ON bo.id = co.branchoffice_id\n" +
            "WHERE  py.status = 1 AND py.{SERVICE_ID} = ?\n" +
            "GROUP BY py.payment_method, bo.zip_code;";

    private static final String QUERY_UNBILLED = "SELECT DISTINCT sv.{SERVICE_CODE} AS code, sv.total_amount as total, sv.created_at \n" +
            "FROM {SERVICE} AS sv\n" +
            "{JOINS}\n" +
            "{WHERE}\n" +
            "LIMIT ?\n" +
            "OFFSET ?;";
    private static final String SUMMARY_UNBILLED = "SELECT {PAYMENT_METHODS} " +
            "SUM(pt.amount) AS total\n" +
            "FROM {SERVICE} AS sv\n" +
            "{JOINS}\n" +
            "{WHERE}\n" +
            "{GROUP};";
    private static final String COUNT_UNBILLED = "SELECT COUNT(items) as items FROM (SELECT DISTINCT sv.id AS items\n" +
            "FROM {SERVICE} AS sv\n" +
            "{JOINS}\n" +
            "{WHERE}) as itemcount;";

    private static final String JOINS_UNBILLED_BRANCHOFFICE = "INNER JOIN tickets AS t ON t.{SERVICE_ID} = sv.id  AND (t.action = 'purchase' OR t.action = 'change')\n"+
    " LEFT JOIN cash_out co ON t.cash_out_id = co.id\n" +
    " LEFT JOIN branchoffice b ON co.branchoffice_id = b.id \n" +
    " LEFT JOIN payment AS pt ON pt.ticket_id = t.id\n " +
    " LEFT JOIN branchoffice bo ON sv.terminal_origin_id = bo.id\n" +
    " LEFT JOIN branchoffice bd ON sv.terminal_destiny_id = bd.id\n" +
    " LEFT JOIN customer AS cc ON cc.id = sv.customer_id\n";
    private static final String JOINS_UNBILLED_ORIGIN = "INNER JOIN tickets AS t ON t.{SERVICE_ID} = sv.id  AND (t.action = 'purchase' OR t.action = 'change')\n"+
        " LEFT JOIN payment AS pt ON pt.ticket_id = t.id\n " +
        " LEFT JOIN customer AS cust ON cust.id = sv.customer_id\n ";
    private static final String GROUP_UNBILLED_BRANCHOFFICE = "";
    private static final String GROUP_UNBILLED_ORIGIN = "";
    private static final String WHERE_BY_BRANCHOFFICE = "WHERE co.branchoffice_id = ? AND t.created_at BETWEEN ? AND ? \n" +
            " AND sv.total_amount > 0 \n" +
            " AND (sv.invoice_id IS NULL OR (sv.invoice_is_global = 1 AND sv.invoice_id IS NOT NULL))\n" +
            // valida que al menos una de las dos sucursales involucradas no cobre IVA del 8%.
            // se comenta temporalmente porque no se necesita
            //" AND (bo.id not in (select id from branchoffice where iva = 0.08) OR bd.id not in (select id from branchoffice where iva = 0.08) )\n" +
            " AND (\n" +
            "    cc.parcel_type IS NULL AND (cc.company_nick_name IS NULL OR cc.company_nick_name != 'IMSS')\n" +
            " )\n";
    private static final String WHERE_BY_ORIGIN = "WHERE sv.purchase_origin = ? " +
            "AND sv.total_amount > 0 \n" +
            "AND t.created_at BETWEEN ? AND ? \n" +
            "AND (sv.invoice_id IS NULL OR (sv.invoice_is_global = 1 AND sv.invoice_id IS NOT NULL))\n";

    private static final String QUERY_GET_SCHEDULE_ROUTE_DETAIL = "SELECT cv.*, sr.config_route_id, sr.arrival_date, v.economic_number , sr.id AS schedule_route_id, cr.terminal_origin_id, cr.terminal_destiny_id, bo.prefix AS terminal_origin_prefix, bd.prefix AS terminal_destiny_prefix, bo.name AS terminal_origin_name, bd.name AS terminal_destiny_name, sr.travel_date , sr.schedule_status, cr.name AS schedule_route_name, sr.code AS schedule_route_code, v.policy_insurance, v.sct_license, v.plate_state, v.policy, v.plate, v.vehicle_year, v.model,v.id as vehicle_id, tp.clave as tipoPermiso \n" +
            "    FROM schedule_route AS sr\n" +
            "    INNER JOIN vehicle AS v ON v.id = sr.vehicle_id\n" +
            "    INNER JOIN config_route AS cr ON cr.id = sr.config_route_id\n"+
            "    INNER JOIN config_vehicle AS cv ON cv.id = v.config_vehicle_id\n" +
            "    INNER JOIN branchoffice AS bo ON bo.id = cr.terminal_origin_id \n" +
            "    INNER JOIN branchoffice AS bd ON bd.id = cr.terminal_destiny_id \n" +
            "    LEFT JOIN c_TipoPermiso AS tp ON tp.id = v.sat_permit_id \n" +
            "    where sr.id = ?";

    private static final String QUERY_GET_STOPS_BY_SCHEDULE_ROUTE= " SELECT   \n" +
            "            cd.order_origin,\n" +
            "            cd.order_destiny,\n" +
            "            origin.id AS terminal_origin_id,  \n" +
            "            origin.name AS terminal_origin_name,  \n" +
            "            origin.prefix as terminal_origin_prefix,    \n" +
            "            destiny.id AS terminal_destiny_id,  \n" +
            "            destiny.name AS terminal_destiny_name,   \n" +
            "            destiny.prefix as terminal_destiny_prefix,\n" +
            "            srdd.time_checkpoint,\n" +
            "            srdd.id,\n" +
            "            srdd.arrival_date,\n"+
            "            srdd.travel_date,\n"+
            "            cd.distance_km,\n" +
            "            srdd.destination_status,\n"+
            "            e.id AS driver_id,\n"+
            "            e.rfc AS driver_rfc,\n"+
            "            srdd.id AS schedule_route_destination_id,\n" +
            "            e.driver_license AS driver_license,\n"+
            "            CONCAT(e.name , ' ', e.last_name) AS driver\n" +
            "            FROM schedule_route AS sr   \n" +
            "               INNER JOIN schedule_route_destination AS srdd ON srdd.schedule_route_id = sr.id\n" +
            "               INNER JOIN schedule_route_driver AS srd ON srd.schedule_route_id = sr.id AND srd.terminal_origin_id = srdd.terminal_origin_id AND srd.terminal_destiny_id = srdd.terminal_destiny_id\n" +
            "               INNER JOIN config_destination AS cd ON cd.id = srdd.config_destination_id\n" +
            "               INNER JOIN employee AS e ON e.id = srd.employee_id\n" +
            "               INNER JOIN branchoffice AS destiny ON destiny.id=cd.terminal_destiny_id   \n" +
            "               INNER JOIN branchoffice AS origin ON origin.id=cd.terminal_origin_id \n" +
            "            WHERE sr.id = ? AND cd.config_route_id = ? AND (COALESCE(cd.order_destiny, 0) - COALESCE(cd.order_origin, 0)) = 1 GROUP BY srdd.id";

    private static final String QUERY_GET_PARCEL_INFO_BY_ID = "SELECT p.*, b.zip_code as terminal_zip_code, 0.16 as branchIva FROM parcels p \n" +
            "LEFT JOIN branchoffice destino ON destino.id = p.terminal_destiny_id\n" +
            "LEFT JOIN branchoffice b ON b.id = p.terminal_origin_id\n" +
            "WHERE p.id = ? ";

    private static final String QUERY_GET_BOARDINGPASS_INFO_BY_ID = "SELECT b.* , branch.zip_code as terminal_zip_code, if( (branch.iva = 0.08 AND destino.iva = 0.08), 0.08, 0.16 ) as branchIva FROM boarding_pass b \n" +
            "LEFT JOIN branchoffice branch ON branch.id = b.terminal_origin_id\n" +
            "LEFT JOIN branchoffice destino ON destino.id = b.terminal_destiny_id \n" +
            "WHERE b.id = ? ";

    private static final String QUERY_GET_SAT_DIRECTION = "select \n" +
            "str.name as Calle,\n" +
            "c_col.c_Colonia as Colonia,\n" +
            "c_mun.c_Estado as c_Estado,\n" +
            "c_mun.c_municipio as c_Municipio\n" +
            " from branchoffice b\n" +
            " left join state sta ON b.state_id = sta.id\n" +
            " left join c_Colonia c_col ON b.zip_code = c_col.c_CodigoPostal\n" +
            " left join c_Municipio c_mun ON sta.name_sat = c_mun.c_Estado \n" +
            " left join c_Localidad c_lo ON sta.name_sat = c_lo.c_Estado\n" +
            " left join street str ON b.street_id = str.id\n" +
            " where b.id = ? ";

    private static final String QUERY_GET_SAT_DIRECTION_V2 = "select \n" +
            "str.name as Calle,\n" +
            "c_col.c_Colonia as Colonia,\n" +
            "c_mun.c_Estado as c_Estado,\n" +
            "c_mun.c_municipio as c_Municipio\n" +
            " from branchoffice b\n" +
            " left join county coun ON b.county_id = coun.id\n" +
            " left join state sta ON b.state_id = sta.id\n" +
            " left join c_Colonia c_col ON b.zip_code = c_col.c_CodigoPostal\n" +
            " left join c_Municipio c_mun ON sta.name_sat = c_mun.c_Estado AND c_mun.description like concat(\"%\",coun.name,\"%\") \n" +
            " left join c_Localidad c_lo ON sta.name_sat = c_lo.c_Estado\n" +
            " left join street str ON b.street_id = str.id\n" +
            " where b.id = ? ";

    private static final String ZIP_CODE_SEARCH_SAT_V3 = " select \n" +
            "            co.c_Colonia as  Colonia,\n" +
            "            lo.c_Localidad as c_Localidad,\n" +
            "            mu.c_Estado as c_Estado,\n" +
            "            mu.c_Municipio as c_Municipio\n" +
            "            from c_Colonia as co\n" +
            "            left join c_Municipio as  mu ON mu.c_Estado = ? AND mu.description like ? \n" +
            "            left join c_Localidad as lo ON lo.c_Estado = ? AND lo.descripcion like ?\n" +
            "            where co.c_CodigoPostal = ? ";

    private static final String QUERY_GET_INVOICE_COMPLEMENT_SITE_PARCEL = "select * from parcel_invoice_complement where id_parcel = ? and tipo_cfdi = 'ingreso' ;";

    private static final String QUERY_GET_INVOICE_COMPLEMENT_SITE_BOARDINGPASS = "select * from boardingpass_invoice_complement where id_boardingpass = ? and tipo_cfdi = 'ingreso' ;";

    private static final String QUERY_GET_IVA_BY_BRANCHOFFICE_BOARDINGPASS = "select if( (b.iva = 0.08 AND bD.iva = 0.08), 0.08, 0.16 ) as iva from boarding_pass  bp\n" +
            "left join branchoffice b ON bp.terminal_origin_id = b.id\n" +
            "left join branchoffice bD ON bp.terminal_destiny_id = bD.id\n" +
            "where bp.id = ? ";

    private static final String QUERY_GET_IVA_BY_BRANCHOFFICE_PARCEL = "select 0.16 as iva from parcels  p\n" +
            "left join branchoffice b ON p.terminal_origin_id = b.id\n" +
            "left join branchoffice bD ON p.terminal_destiny_id = bD.id\n" +
            "where p.id = ? ";

    private static final String QUERY_GET_IVA_BY_BRANCHOFFICE_GUIAPP = "select 0.16 from parcels_prepaid  pp\n" +
            "left join branchoffice b ON pp.branchoffice_id = b.id\n" +
            "where pp.id = ? ";

    private static final String QUERY_GET_IVA_BY_BRANCHOFFICE_PREPAID_BP = "select 0.16 from prepaid_package_travel  ppt\n" +
            "left join branchoffice b ON ppt.branchoffice_id = b.id\n" +
            "where ppt.id = ? ";

    private static final String QUERY_GET_GUIAPP_INFO_BY_ID = "SELECT b.* , branch.zip_code as terminal_zip_code, 0.16 as branchIva FROM parcels_prepaid b\n" +
            "LEFT JOIN branchoffice branch ON branch.id = b.branchoffice_id\n" +
            "WHERE b.id = ?";

    private static final String QUERY_GET_PREPAID_BPP_INFO_BY_ID = "SELECT ppt.* , branch.zip_code as terminal_zip_code, 0.16 as branchIva FROM prepaid_package_travel ppt\n" +
            "LEFT JOIN branchoffice branch ON branch.id = ppt.branchoffice_id\n" +
            "WHERE ppt.id = ?";
    private static final String QUERY_GET_GUIAPP_GUIAS_BY_ID = "SELECT ppd.id, ppd.guiapp_code as code FROM parcels_prepaid_detail ppd WHERE ppd.parcel_prepaid_id = ? ORDER BY ppd.id";

    private static final String QUERY_GET_PP_PRICE_NAME_BY_PREPAID_ID = "SELECT DISTINCT ppp.name_price as package_price_name \n" +
            "FROM parcels_prepaid_detail ppd \n" +
            "LEFT JOIN pp_price ppp ON ppp.id = ppd.price_id \n" +
            "WHERE ppd.parcel_prepaid_id = ?\n" +
            "LIMIT 1";

    private static final String QUERY_GET_PREPAID_BPP_RESERVATIONS_BY_ID = "SELECT bp.id, bp.reservation_code as code FROM boarding_pass bp WHERE bp.prepaid_id = ? ORDER BY bp.id";

    private static final String QUERY_GET_INVOICE_COMPLEMENT_SITE_PREPAID = "select * from guiapp_invoice_complement where id_prepaid = ? and tipo_cfdi = 'ingreso' ;";

    private static final String QUERY_GET_INVOICE_COMPLEMENT_SITE_PREPAID_BOARDINGPASS = "select * from prepaid_travel_invoice_complement where id_prepaid = ? and tipo_cfdi = 'ingreso' ;";

    private static final String WHERE_BY_BRANCHOFFICE_v2 = "WHERE ( co.branchoffice_id = ? || bo.id = ?) AND t.created_at BETWEEN ? AND ? \n" +
            " AND sv.total_amount > 0 \n" +
            " AND (sv.invoice_id IS NULL OR (sv.invoice_is_global = 1 AND sv.invoice_id IS NOT NULL))\n" +
            " AND bd.id = ? \n";

    private static final String WHERY_GET_CUSTOMER_BILLING_INFO_SIMPLE = "SELECT * FROM customer_billing_information where id = ?";

    private static final String WHERY_GET_PACKAGES_BY_PARCEL_ID = "SELECT\n" +
            "pp.*, pp.parcel_iva AS iva_withheld, ppri.name_price AS package_type_name,\n" +
            "(pp.amount + pp.excess_cost) - (pp.discount + pp.excess_discount) as t_freight\n" +
            "FROM\n" +
            "parcels_packages pp\n" +
            "LEFT JOIN package_price as ppri ON ppri.id = pp.package_price_id\n" +
            "where pp.parcel_id = ?";

    private static final String QUERY_GET_PARCEL_INFO_FOR_INVOICE = "SELECT\n" +
            "p.id,\n" +
            "(p.excess_amount - p.excess_discount) as t_excess_amount,\n" +
            "(p.amount + p.excess_amount) - (p.discount + p.excess_discount) as t_freight,\n" +
            "p.services_amount,\n"+
            "p.extra_charges,\n"+
            "p.insurance_amount,\n"+
            "(p.amount + p.excess_amount + p.services_amount + p.insurance_amount + p.extra_charges) - (p.discount + p.excess_discount) as t_amount,\n"+
            "p.amount,\n" +
            "p.discount,\n" +
            "p.iva,\n" +
            "p.parcel_iva as iva_withheld,\n" +
            "p.total_amount,\n" +
            "p.insurance_amount,\n" +
            "p.invoice_id,\n" +
            "p.created_at,\n" +
            "p.shipment_type,\n" +
            "p.payment_condition,\n" +
            "p.invoice_is_global,\n" +
            "p.customer_id,\n" +
            "p.customer_billing_information_id,\n" +
            "c.first_name as customer_first_name,\n" +
            "c.last_name as customer_last_name,\n" +
            "CONCAT(c.first_name, ' ', c.last_name) as customer_name,\n" +
            "c.invoice_email as customer_invoice_email,\n" +
            "c.company_nick_name as customer_company_nick_name,\n" +
            "c.relate_ccp_with_invoice as relate_ccp_with_invoice,\n" +
            "EXISTS (\n" +
            "        SELECT 1\n" +
            "        FROM payment_complement_detail pcd\n" +
            "        WHERE pcd.invoice_id = p.invoice_id\n" +
            "          AND pcd.status = 1\n" +
            "        LIMIT 1\n" +
            ") AS has_payment_complement,\n" +
            "p.pays_sender\n" +
            "FROM parcels p\n" +
            "LEFT JOIN customer c on c.id = p.customer_id\n" +
            "WHERE p.parcel_tracking_code = ?";

    private static final String QUERY_GET_PARCELS_PREPAID_INFO_FOR_INVOICE = "SELECT \n" +
            "    pp.id,\n" +
            "    pp.amount,\n" +
            "    pp.discount,\n" +
            "    pp.iva,\n" +
            "    pp.parcel_iva AS iva_withheld,\n" +
            "    pp.total_amount,\n" +
            "    pp.invoice_id,\n" +
            "    pp.insurance_amount,\n" +
            "    pp.created_at,\n" +
            "    pp.payment_condition,\n" +
            "    pp.invoice_is_global,\n" +
            "    pp.customer_id,\n" +
            "    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,\n" +
            "    EXISTS (\n" +
            "        SELECT 1\n" +
            "        FROM payment_complement_detail pcd\n" +
            "        WHERE pcd.invoice_id = pp.invoice_id\n" +
            "          AND pcd.status = 1\n" +
            "        LIMIT 1\n" +
            "    ) AS has_payment_complement\n" +
            "FROM\n" +
            "    parcels_prepaid pp\n" +
            "    LEFT JOIN customer c ON c.id = pp.customer_id\n" +
            "WHERE\n" +
            "    pp.tracking_code = ?";

    private static final String QUERY_GET_GLOBAL_INVOICE_XML_BY_ID_PARCEL_SERVICE = "SELECT \n" +
            "xml\n" +
            "FROM parcel_invoice_complement\n" +
            "WHERE id_parcel = (SELECT id FROM parcels WHERE invoice_id = ? LIMIT 1)\n" +
            "AND status_cfdi = 'timbrado'\n" +
            "AND tipo_cfdi = 'factura global'\n" +
            "ORDER BY id DESC LIMIT 1";

    private static final String QUERY_GET_GLOBAL_INVOICE_XML_BY_ID_PARCEL_PREPAID_SERVICE = "SELECT \n" +
            "xml\n" +
            "FROM guiapp_invoice_complement\n" +
            "WHERE id_prepaid = (SELECT id FROM parcels_prepaid WHERE invoice_id = ? LIMIT 1)\n" +
            "AND status_cfdi = 'timbrado'\n" +
            "AND tipo_cfdi = 'factura global'\n" +
            "ORDER BY id DESC LIMIT 1";

    private static final String WHERY_GET_TRAVEL_LOG_INVOICE_UUID_FROM_PARCEL_ID = "SELECT\n" +
            "tlc.uuid\n" +
            "FROM travel_logs_ccp tlc\n" +
            "WHERE tlc.invoice_status = 'timbrado'\n" +
            "AND tlc.uuid is not null\n" +
            "AND tlc.status = 1\n" +
            "AND tlc.specific_parcel_id = ?";
    private static final String QUERY_GET_INVOICE_XML_BY_ID_PARCEL_SERVICE = "SELECT \n" +
            "xml\n" +
            "FROM parcel_invoice_complement\n" +
            "WHERE id_parcel = (SELECT id FROM parcels WHERE invoice_id = ? LIMIT 1)\n" +
            "AND status_cfdi = 'timbrado'\n" +
            "AND tipo_cfdi = 'factura global'\n" +
            "ORDER BY id DESC";

    private static final String QUERY_GET_PARCELS_FOR_INVOICING_BY_CRITERIA = "WITH ParcelTypes AS (\n" +
            "    SELECT\n" +
            "        pp.parcel_id,\n" +
            "        CASE\n" +
            "            WHEN MIN(COALESCE(pp.weight, 1)) <= 31.5 AND MAX(COALESCE(pp.weight, 1)) <= 31.5 THEN 'courier'\n" +
            "            WHEN MIN(COALESCE(pp.weight, 1)) > 31.5 AND MAX(COALESCE(pp.weight, 1)) > 31.5 THEN 'freight'\n" +
            "            ELSE 'mixed'\n" +
            "        END AS parcel_type\n" +
            "    FROM parcels_packages pp\n" +
            "    GROUP BY pp.parcel_id\n" +
            "),\n" +
            "SpecificCCP AS (\n" +
            "    SELECT\n" +
            "        sp.parcel_id,\n" +
            "        MIN(tlccp.id) AS specific_tlccp_id\n" +
            "    FROM shipments_parcels sp\n" +
            "    LEFT JOIN travel_logs_ccp tlccp \n" +
            "        ON tlccp.specific_parcel_id = sp.parcel_id\n" +
            "    WHERE tlccp.invoice_status = 'timbrado'\n" +
            "    GROUP BY sp.parcel_id\n" +
            ")," +
            "ccp AS (\n" +
            "    SELECT\n" +
            "        sp.parcel_id,\n" +
            "        COALESCE(\n" +
            "            MAX(CASE WHEN tlccp.specific_parcel_id = sp.parcel_id THEN tlccp.id END),\n" +
            "            MAX(CASE WHEN tlccp.customer_id IS NULL THEN tlccp.id END)\n" +
            "        ) AS courier_tlccp_id,\n" +
            "        COALESCE(\n" +
            "            MIN(CASE WHEN tlccp.specific_parcel_id = sp.parcel_id THEN tlccp.id END),\n" +
            "            MIN(CASE WHEN tlccp.customer_id = p.customer_id THEN tlccp.id END)\n" +
            "        ) AS freight_tlccp_id\n" +
            "    FROM shipments_parcels sp\n" +
            "    LEFT JOIN travel_logs tl ON tl.load_id = sp.shipment_id\n" +
            "    LEFT JOIN travel_logs_ccp tlccp ON tlccp.travel_log_id = tl.id\n" +
            "    LEFT JOIN parcels p ON p.id = sp.parcel_id\n" +
            "    WHERE tl.has_stamp = TRUE\n" +
            "      AND tlccp.invoice_status = 'timbrado'\n" +
            "    GROUP BY sp.parcel_id\n" +
            ")\n" +
            "SELECT\n" +
            "    p.id,\n" +
            "    p.parcel_tracking_code as code,\n" +
            "    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,\n" +
            "    cbi.rfc AS rfc, \n" +
            "    (p.excess_amount - p.excess_discount) AS t_excess_amount,\n" +
            PARCEL_FREIGHT_COLUMN_DEF + " AS t_freight,\n" +
            "    p.services_amount,\n" +
            "    p.extra_charges,\n" +
            PARCEL_AMOUNT_COLUMN_DEF + " AS t_amount,\n" +
            "    p.amount,\n" +
            "    p.discount,\n" +
            "    p.iva,\n" +
            "    p.parcel_iva AS iva_withheld,\n" +
            "    p.total_amount,\n" +
            "    p.insurance_amount,\n" +
            "    p.invoice_id,\n" +
            "    p.created_at,\n" +
            "    p.shipment_type,\n" +
            "    p.payment_condition,\n" +
            "    p.invoice_is_global,\n" +
            "    p.customer_id,\n" +
            "    p.customer_billing_information_id,\n" +
            "    p.total_packages,\n" +
            "    p.pays_sender,\n" +
            "    COALESCE(pay.total_payment, 0) + COALESCE(dp.total_payment_credit, 0) AS total_payment,\n" +
            "    CASE\n" +
            "        WHEN pay.payment_method IS NOT NULL AND dp.payment_method_credit IS NOT NULL THEN \n" +
            "            CONCAT(pay.payment_method, ', ', dp.payment_method_credit)\n" +
            "        WHEN pay.payment_method IS NOT NULL THEN \n" +
            "            pay.payment_method\n" +
            "        WHEN dp.payment_method_credit IS NOT NULL THEN \n" +
            "            dp.payment_method_credit\n" +
            "        ELSE \n" +
            "        NULL\n" +
            "    END AS payment_method,\n" +
            "    c.company_nick_name as customer_company_nick_name,\n" +
            "    CASE\n" +
            "        WHEN specific_ccp.specific_tlccp_id IS NOT NULL THEN specific_ccp.specific_tlccp_id\n" +
            "        WHEN pt.parcel_type = 'courier' THEN ccp.courier_tlccp_id\n" +
            "        WHEN pt.parcel_type = 'freight' THEN ccp.freight_tlccp_id\n" +
            "        WHEN pt.parcel_type = 'mixed' THEN COALESCE(ccp.freight_tlccp_id, ccp.courier_tlccp_id)\n" +
            "    END AS travel_logs_ccp_id,\n" +
            "    pt.parcel_type,\n" +
            "    EXISTS (\n" +
            "        SELECT 1\n" +
            "        FROM payment_complement_detail pcd\n" +
            "        WHERE pcd.invoice_id = p.invoice_id\n" +
            "          AND pcd.status = 1\n" +
            "        LIMIT 1\n" +
            "    ) AS has_payment_complement\n" +
            "FROM parcels p\n" +
            "LEFT JOIN invoice inv ON inv.id = p.invoice_id\n" +
            "LEFT JOIN customer c ON c.id = p.customer_id\n" +
            "LEFT JOIN customer_billing_information cbi ON p.customer_billing_information_id = cbi.id\n" +
            "LEFT JOIN (\n" +
            "    SELECT\n" +
            "        pay.parcel_id,\n" +
            "        COALESCE(SUM(pay.amount), 0) AS total_payment,\n" +
            "        MIN(pay.payment_method) AS payment_method\n" +
            "    FROM payment pay\n" +
            "    WHERE pay.status = 1\n" +
            "    GROUP BY pay.parcel_id\n" +
            ") pay ON pay.parcel_id = p.id\n" +
            "LEFT JOIN (\n" +
            "    SELECT\n" +
            "        dp.parcel_id,\n" +
            "        COALESCE(SUM(dp.amount), 0) AS total_payment_credit,\n" +
            "        MIN(pay.payment_method) AS payment_method_credit\n" +
            "    FROM debt_payment dp\n" +
            "    LEFT JOIN payment pay ON pay.id = dp.payment_id\n" +
            "    WHERE dp.status = 1\n" +
            "    GROUP BY dp.parcel_id\n" +
            ") dp ON dp.parcel_id = p.id\n" +
            "LEFT JOIN ParcelTypes pt ON pt.parcel_id = p.id\n" +
            "LEFT JOIN SpecificCCP specific_ccp ON specific_ccp.parcel_id = p.id\n" +
            "LEFT JOIN ccp ON ccp.parcel_id = p.id\n" +
            "WHERE\n" +
            "    p.parcel_status != 4\n" +
            "    AND p.total_amount > 0\n" +
            "    AND p.created_at BETWEEN ? AND ?";

    private static final String QUERY_COUNT_GET_PARCELS_FOR_INVOICING_BY_CRITERIA = "SELECT COUNT(p.id) AS count, \n" +
            "SUM(p.total_amount) AS amount,\n" +
            "SUM(CASE WHEN p.invoice_id IS NULL THEN p.total_amount ELSE 0 END) AS pending_amount,\n" +
            "SUM(CASE WHEN p.invoice_id IS NOT NULL THEN p.total_amount ELSE 0 END) AS invoiced_amount,\n" +
            "COUNT(CASE WHEN p.invoice_id IS NULL THEN 1 END) AS pending_count,\n" +
            "COUNT(CASE WHEN p.invoice_id IS NOT NULL THEN 1 END) AS invoiced_count\n" +
            "FROM {TABLE_NAME} p\n" +
            "WHERE p.parcel_status != 4\n" +
            " AND p.total_amount > 0\n" +
            "AND p.created_at BETWEEN ? AND ?";


    private static final String QUERY_GET_PARCELS_PREPAID_FOR_INVOICING_BY_CRITERIA = "SELECT \n" +
            "    p.id,\n" +
            "    p.tracking_code as code,\n" +
            "    p.amount,\n" +
            "    p.discount,\n" +
            "    p.iva,\n" +
            "    p.parcel_iva AS iva_withheld,\n" +
            "    p.total_amount,\n" +
            "    p.invoice_id,\n" +
            "    p.insurance_amount,\n" +
            "    p.created_at,\n" +
            "    p.payment_condition,\n" +
            "    p.invoice_is_global,\n" +
            "    p.customer_id,\n" +
            "    p.total_count_guipp as total_packages,\n" +
            "    p.shipment_type,\n" +
            "    cbi.rfc AS rfc, \n" +
            "    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,\n" +
            "    COALESCE(pay.total_payment, 0) + COALESCE(dp.total_payment_credit, 0) AS total_payment,\n" +
            "    CASE\n" +
            "        WHEN pay.payment_method IS NOT NULL AND dp.payment_method_credit IS NOT NULL THEN \n" +
            "            CONCAT(pay.payment_method, ', ', dp.payment_method_credit)\n" +
            "        WHEN pay.payment_method IS NOT NULL THEN \n" +
            "            pay.payment_method\n" +
            "        WHEN dp.payment_method_credit IS NOT NULL THEN \n" +
            "            dp.payment_method_credit\n" +
            "        ELSE NULL\n" +
            "    END AS payment_method,\n" +
            "    EXISTS (\n" +
            "        SELECT 1\n" +
            "        FROM payment_complement_detail pcd\n" +
            "        WHERE pcd.invoice_id = p.invoice_id\n" +
            "          AND pcd.status = 1\n" +
            "        LIMIT 1\n" +
            "    ) AS has_payment_complement\n" +
            "FROM parcels_prepaid p\n" +
            "LEFT JOIN invoice inv ON inv.id = p.invoice_id\n" +
            "LEFT JOIN customer c ON c.id = p.customer_id\n" +
            "LEFT JOIN customer_billing_information cbi ON p.customer_billing_information_id = cbi.id\n" +
            "LEFT JOIN (\n" +
            "    SELECT\n" +
            "        pay.parcel_prepaid_id,\n" +
            "        COALESCE(SUM(pay.amount), 0) AS total_payment,\n" +
            "        MIN(pay.payment_method) AS payment_method\n" +
            "    FROM payment pay\n" +
            "    WHERE pay.status = 1\n" +
            "    GROUP BY pay.parcel_prepaid_id\n" +
            ") pay ON pay.parcel_prepaid_id = p.id\n" +
            "LEFT JOIN (\n" +
            "    SELECT\n" +
            "        dp.parcel_prepaid_id,\n" +
            "        COALESCE(SUM(dp.amount), 0) AS total_payment_credit,\n" +
            "        MIN(pay.payment_method) AS payment_method_credit\n" +
            "    FROM debt_payment dp\n" +
            "    LEFT JOIN payment pay ON pay.id = dp.payment_id\n" +
            "    WHERE dp.status = 1\n" +
            "    GROUP BY dp.parcel_prepaid_id\n" +
            ") dp ON dp.parcel_prepaid_id = p.id\n" +
            "WHERE\n" +
            "    p.parcel_status != 4  \n" +
            "    AND p.total_amount > 0\n" +
            "    AND p.created_at BETWEEN ? AND ?";

    private static final String WHERY_GET_CCP_UUID_FROM_PARCEL_ID = "WITH ParcelTypes AS (\n" +
            "    SELECT\n" +
            "        pp.parcel_id,\n" +
            "        CASE\n" +
            "            WHEN MIN(COALESCE(pp.weight, 1)) <= 31.5 AND MAX(COALESCE(pp.weight, 1)) <= 31.5 THEN 'courier'\n" +
            "            WHEN MIN(COALESCE(pp.weight, 1)) > 31.5 AND MAX(COALESCE(pp.weight, 1)) > 31.5 THEN 'freight'\n" +
            "            ELSE 'mixed'\n" +
            "        END AS parcel_type\n" +
            "    FROM parcels_packages pp\n" +
            "    GROUP BY pp.parcel_id\n" +
            "),\n" +
            "SpecificCCP AS (\n" +
            "    SELECT\n" +
            "        sp.parcel_id,\n" +
            "        MIN(tlccp.id) AS specific_tlccp_id\n" +
            "    FROM shipments_parcels sp\n" +
            "    LEFT JOIN travel_logs_ccp tlccp \n" +
            "        ON tlccp.specific_parcel_id = sp.parcel_id\n" +
            "    WHERE tlccp.invoice_status = 'timbrado'\n" +
            "    GROUP BY sp.parcel_id\n" +
            "),\n" +
            "ccp AS (\n" +
            "    SELECT\n" +
            "        sp.parcel_id,\n" +
            "        COALESCE(\n" +
            "            MAX(CASE WHEN tlccp.specific_parcel_id = sp.parcel_id THEN tlccp.id END),\n" +
            "            MAX(CASE WHEN tlccp.customer_id IS NULL THEN tlccp.id END)\n" +
            "        ) AS courier_tlccp_id,\n" +
            "        COALESCE(\n" +
            "            MIN(CASE WHEN tlccp.specific_parcel_id = sp.parcel_id THEN tlccp.id END),\n" +
            "            MIN(CASE WHEN tlccp.customer_id = p.customer_id THEN tlccp.id END)\n" +
            "        ) AS freight_tlccp_id\n" +
            "    FROM shipments_parcels sp\n" +
            "    LEFT JOIN travel_logs tl ON tl.load_id = sp.shipment_id\n" +
            "    LEFT JOIN travel_logs_ccp tlccp ON tlccp.travel_log_id = tl.id\n" +
            "    LEFT JOIN parcels p ON p.id = sp.parcel_id\n" +
            "    WHERE tl.has_stamp = TRUE\n" +
            "      AND tlccp.invoice_status = 'timbrado'\n" +
            "    GROUP BY sp.parcel_id\n" +
            ")\n" +
            "SELECT \n" +
            "    tlccp.uuid\n" +
            "FROM \n" +
            "    travel_logs_ccp tlccp\n" +
            "LEFT JOIN (\n" +
            "    SELECT \n" +
            "        p.id AS parcel_id,\n" +
            "        CASE\n" +
            "            WHEN specific_ccp.specific_tlccp_id IS NOT NULL THEN specific_ccp.specific_tlccp_id\n" +
            "            WHEN pt.parcel_type = 'courier' THEN ccp.courier_tlccp_id\n" +
            "            WHEN pt.parcel_type = 'freight' THEN ccp.freight_tlccp_id\n" +
            "            WHEN pt.parcel_type = 'mixed' THEN COALESCE(ccp.freight_tlccp_id, ccp.courier_tlccp_id)\n" +
            "        END AS travel_logs_ccp_id\n" +
            "    FROM parcels p\n" +
            "    LEFT JOIN ParcelTypes pt ON pt.parcel_id = p.id\n" +
            "    LEFT JOIN SpecificCCP specific_ccp ON specific_ccp.parcel_id = p.id\n" +
            "    LEFT JOIN ccp ON ccp.parcel_id = p.id\n" +
            ") resolved_ccp ON tlccp.id = resolved_ccp.travel_logs_ccp_id\n" +
            "WHERE \n" +
            "    resolved_ccp.parcel_id = ?\n" +
            "LIMIT 1";

    private static final String  QUERY_GET_INVOICES_FOR_PAYMENT_COMPLEMENT = "SELECT \n" +
            "  i.id,\n" +
            "  i.amount,\n" +
            "  i.discount,\n" +
            "  i.iva,\n" +
            "  i.iva_withheld,\n" +
            "  i.total_amount,\n" +
            "  i.created_at,\n" +
            "  i.email,\n" +
            "  i.cfdi_use,\n" +
            "  i.service_type,\n" +
            "  i.uuid,\n" +
            "  i.cfdi_payment_form,\n" +
            "  i.cfdi_payment_method,\n" +
            "  i.available_subtotal_for_complement,\n" +
            "  i.available_iva_for_complement,\n" +
            "  i.available_iva_withheld_for_complement,\n" +
            "  i.available_amount_for_complement,\n" +
            "  cbi.id as customer_billing_information_id,\n" +
            "  cbi.name as billing_name,\n" +
            "  cbi.rfc as billing_rfc,\n" +
            "  cbi.legal_person as billing_legal_person,\n" +
            "  cbi.zip_code as billing_zip_code,\n" +
            "  regimen.c_RegimenFiscal as billing_regimen_fiscal,\n" +
            "  CASE \n" +
            "    WHEN i.service_type = 'parcel' THEN\n" +
            "      CASE\n" +
            "        WHEN i.is_multiple = 1 THEN \n" +
            "          CONCAT(\n" +
            "            CASE \n" +
            "              WHEN c.parcel_type IS NOT NULL AND i.payment_condition = 'cash' THEN 'GC-CO'\n" +
            "              WHEN c.parcel_type IS NOT NULL AND i.payment_condition = 'credit' THEN 'GC-CR'\n" +
            "              ELSE 'PG'\n" +
            "            END,\n" +
            "            ' ',\n" +
            "            (SELECT id FROM parcels WHERE parcel_tracking_code = i.reference LIMIT 1)\n" +
            "          )\n" +
            "        WHEN i.is_multiple = 0 THEN \n" +
            "          CONCAT(\n" +
            "            CASE \n" +
            "              WHEN i.is_global = 1 THEN 'PG'\n" +
            "              WHEN c.parcel_type IS NOT NULL AND i.payment_condition = 'cash' THEN 'GC-CO'\n" +
            "              WHEN c.parcel_type IS NOT NULL AND i.payment_condition = 'credit' THEN 'GC-CR'\n" +
            "              ELSE 'PG'\n" +
            "            END,\n" +
            "            ' ',\n" +
            "            CASE \n" +
            "              WHEN i.is_global = 1 THEN i.id\n" +
            "              ELSE (SELECT id FROM parcels WHERE invoice_id = i.id LIMIT 1)\n" +
            "            END\n" +
            "          )\n" +
            "        ELSE ''\n" +
            "      END\n" +
            "    WHEN i.service_type = 'guia_pp' THEN\n" +
            "      CASE\n" +
            "        WHEN i.is_multiple = 1 THEN \n" +
            "          CONCAT(\n" +
            "            CASE \n" +
            "              WHEN c.parcel_type IS NOT NULL AND i.payment_condition = 'cash' THEN 'GPP-CO'\n" +
            "              WHEN c.parcel_type IS NOT NULL AND i.payment_condition = 'credit' THEN 'GPP-CR'\n" +
            "              ELSE 'PG'\n" +
            "            END,\n" +
            "            ' ',\n" +
            "            (SELECT id FROM parcels_prepaid WHERE tracking_code = i.reference LIMIT 1)\n" +
            "          )\n" +
            "        WHEN i.is_multiple = 0 THEN \n" +
            "          CONCAT(\n" +
            "            CASE \n" +
            "              WHEN i.is_global = 1 THEN 'PG'\n" +
            "              WHEN c.parcel_type IS NOT NULL AND i.payment_condition = 'cash' THEN 'GPP-CO'\n" +
            "              WHEN c.parcel_type IS NOT NULL AND i.payment_condition = 'credit' THEN 'GPP-CR'\n" +
            "              ELSE 'PG'\n" +
            "            END,\n" +
            "            ' ',\n" +
            "            CASE \n" +
            "              WHEN i.is_global = 1 THEN i.id\n" +
            "              ELSE (SELECT id FROM parcels_prepaid WHERE invoice_id = i.id LIMIT 1)\n" +
            "            END\n" +
            "          )\n" +
            "        ELSE ''\n" +
            "      END\n" +
            "    ELSE ''\n" +
            "  END AS invoice_folio,\n" +
            "  COALESCE((\n" +
            "    SELECT MAX(pcd.installment_number)\n" +
            "    FROM payment_complement_detail pcd\n" +
            "    WHERE pcd.invoice_id = i.id AND pcd.status = 1\n" +
            "  ), 0) AS payment_complement_count\n" +
            "FROM invoice i\n" +
            "LEFT JOIN customer_billing_information cbi ON cbi.id = i.customer_billing_information_id\n" +
            "LEFT JOIN customer c ON c.id = cbi.customer_id\n" +
            "LEFT JOIN c_RegimenFiscal regimen ON regimen.id = cbi.c_RegimenFiscal_id\n" +
            "WHERE \n" +
            "i.invoice_status = 'done'";

    private static final String  QUERY_GET_INVOICE_DATA = "SELECT\n " +
            "i.id as invoice_id,\n" +
            "i.total_amount,\n" +
            "i.invoice_status,\n" +
            "i.document_id,\n" +
            "i.iva,\n" +
            "i.iva_withheld,\n" +
            "i.amount,\n" +
            "i.discount,\n" +
            "i.uuid,\n" +
            "i.description,\n" +
            "i.service_subtype,\n" +
            "b.prefix as branchoffice_prefix,\n" +
            "b.name as branchoffice_name\n" +
            "FROM invoice i\n" +
            "LEFT JOIN branchoffice b ON b.id = i.branchoffice_id\n" +
            "WHERE\n" +
            "i.is_global = 1\n" +
            "AND i.global_period = ?\n" +
            "AND i.service_type = ?\n" +
            "AND i.status = 1";

    private static final String QUERY_GET_SERVICES_BY_INVOICE = "SELECT\n" +
            "  t.invoice_id,\n" +
            "  t.service_type,\n" +
            "  t.uuid,\n" +
            "  t.invoice_folio,\n" +
            "  t.p_id,\n" +
            "  t.tracking_code,\n" +
            "  t.p_iva,\n" +
            "  t.p_iva_withheld,\n" +
            "  t.p_total_amount,\n" +
            "  t.p_debt\n" +
            "FROM (\n" +
            "  SELECT\n" +
            "    i.id AS invoice_id,\n" +
            "    i.service_type,\n" +
            "    i.uuid,\n" +
            "    CONCAT(i.cfdi_serie, ' ', i.cfdi_folio) AS invoice_folio,\n" +
            "    p.id AS p_id,\n" +
            "    p.parcel_tracking_code AS tracking_code,\n" +
            "    p.iva AS p_iva,\n" +
            "    p.parcel_iva AS p_iva_withheld,\n" +
            "    p.total_amount AS p_total_amount,\n" +
            "    p.debt AS p_debt\n" +
            "  FROM parcels AS p\n" +
            "  JOIN invoice AS i\n" +
            "    ON i.id = p.invoice_id\n" +
            "   AND i.service_type = 'parcel'\n" +
            "\n" +
            "  UNION ALL\n" +
            "\n" +
            "  SELECT\n" +
            "    i.id AS invoice_id,\n" +
            "    i.service_type,\n" +
            "    i.uuid,\n" +
            "    CONCAT(i.cfdi_serie, ' ', i.cfdi_folio) AS invoice_folio,\n" +
            "    pp.id AS p_id,\n" +
            "    pp.tracking_code AS tracking_code,\n" +
            "    pp.iva AS p_iva,\n" +
            "    pp.parcel_iva AS p_iva_withheld,\n" +
            "    pp.total_amount AS p_total_amount,\n" +
            "    pp.debt AS p_debt\n" +
            "  FROM parcels_prepaid AS pp\n" +
            "  JOIN invoice AS i\n" +
            "    ON i.id = pp.invoice_id\n" +
            "   AND i.service_type = 'guia_pp'\n" +
            ") AS t\n" +
            "WHERE 1=1";
}
