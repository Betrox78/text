/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.customers;

import database.commons.DBVerticle;
import database.commons.ErrorCodes;
import database.commons.GenericQuery;
import database.invoicing.InvoiceDBV;
import database.invoicing.InvoiceDBV.InvoiceInstance;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import models.DeepLinkModel;
import service.commons.MailVerticle;
import utils.*;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.client.WebClient;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static database.money.ReportDBV.END_DATE;
import static database.money.ReportDBV.INIT_DATE;
import static java.util.stream.Collectors.toList;
import static service.commons.Constants.*;

/**
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class CustomerDBV extends DBVerticle {

    public static final String ACTION_REGISTER = "CustomerDBV.register";
    public static final String ACTION_EXTERNAL_REGISTER = "CustomerDBV.externalRegister";
    public static final String ACTION_APP_CLIENT_REGISTER = "CustomerDBV.appClientRegister";
    public static final String ACTION_REPORT = "CustomerDBV.report";
    public static final String ACTION_CONFIRM_ACCOUNT = "CustomerDBV.confirmAccount";
    public static final String ACTION_REGISTER_BILLING_INFORMATION = "CustomerDBV.registerBillingInformation";
    public static final String ACTION_CONFIRM_BILLING_INFORMATION = "CustomerDBV.confirmBillingInformation";
    public static final String ACTION_TRIGGER_CUSTOMER_INVOICES = "CustomerDBV.triggerCustomerInvoices";
    public static final String ACTION_SYNC_CUSTOMER_BILLINGS = "CustomerDBV.triggerCustomerBillings";
    public static final String ACTION_FIND_BILLING_INFORMATION_BY_CUSTOMER_ID = "CustomerDBV.findBillingInformationByCustomerID";
    public static final String ACTION_FIND_BILLING_INFORMATION_BY_RFC_AND_ZIP_CODE = "CustomerDBV.findBillingInformationByRFCAndZipCode";
    public static final String ACTION_SEARCH_BY_NAME_AND_LASTNAME = "CustomerDBV.searchCustomerByName";
    public static final String ACTION_LOCK_CUSTOMER_CREDIT = "CustomerDBV.lockCustomerCredit";
    public static final String ACTION_GET_DEBTS = "CustomerDBV.getDebtListByCustomerId";
    public static final String ACTION_SUBSCRIPTION = "CustomerDBV.subscription";
    public static final String ACTION_GET_CUSTOMER_CREDIT = "CustomerDBV.getCustomerCredit";
    public static final String ACTION_GET_CUSTOMER_CREDIT_BY_PARCELID = "CustomerDBV.getCustomerCreditbyTrackingCode";
    public static final String ACTION_SEARCH_V2 = "CustomerDBV.searchV2";

    private static String URI = "api.cfdi.allabordo.com";
    private static String URI_PARCEL = "ptx.cfdi.allabordo.com";
    public static final String ACTION_CUSTOMER_WALLET_REPORT = "CustomerDBV.walletReport";
    public static final String ACTION_CUSTOMER_WALLET_DETAIL_REPORT = "CustomerDBV.walletDetailReport";
    public static final String ACTION_GET_CUSTOMER_DEBT = "CustomerDBV.getCustomerDebt";
    public static final String ACTION_CREATE_REPORT_CUSTOMERS = "CustomerDBV.reportCustomers";
    public static final String ACTION_GET_CUSTOMER_BY_USER_ID = "CustomerDBV.getCustomerByUserId";
    public static final String ACTION_UPDATE_CUSTOMER_INFO = "CustomerDBV.updateCustomerInfo";
    public static final String ACTION_BILLING_INFORMATION_RFC = "CustomerDBV.checkRFC";
    public static final String ACTION_BILLING_DELETE_INFO = "CustomerDBV.deleteBilling";
    public static final String ACTION_ACCOUNT_STATUS_REPORT = "CustomerDBV.accountStatusReport";
    public static final String ACTION_GET_DETAIL = "CustomerDBV.getDetail";
    public static final String ACTION_UPDATE_BASIC_INFO = "CustomerDBV.updateBasicInfo";
    public static final String ACTION_CREATE_PASSENGER = "CustomerDBV.createPassenger";
    public static final String ACTION_UPDATE_PASSENGER = "CustomerDBV.updatePassenger";
    public static final String ACTION_GET_PASSENGERS_BY_CUSTOMER_ID = "CustomerDBV.getPassengersByCustomerId";
    public static final String ACTION_CREATE_FCM_TOKEN = "CustomerDBV.createFCMToken";
    public static final String ACTION_GET_FCM_TOKENS = "CustomerDBV.getFCMTokens";
    public static final String ACTION_VERIFY_WITH_PHONE = "CustomerDBV.verifyWithPhone";
    public static final String ACTION_GET_CUSTOMERS_BY_ADVISER_ID = "CustomerDBV.getCustomersByAdviserId";
    public static final String ACTION_FIND_ALL_BILLING_INFORMATION_BY_CUSTOMER_ID = "CustomerDBV.findAllBillingInformationByCustomerID";
    public static final String ACTION_GET_SERVICES_WITH_DEBT = "CustomerDBV.getServicesWithDebt";
    public static final String ACTION_GET_CUSTOMERS_CATALOGUE_LIST = "CustomerDBV.getCustomersCatalogueList";
    public static final String ACTION_IS_ASSIGNED = "CustomerDBV.isAssigned";


    private static boolean DOUBLE_INVOICING = false;
    public static String CUSTOMER_INVOICE_PREFIX = "D";
    private static String TOTAL_DEBT = "total_debt";
    private static final String CREATE = "create";
    private static final String TERMINAL_ID = "terminal_id";
    public static final String ONLY_DEBTORS = "only_debtors";
    private static final String DAYS_0_7 = "days_0_7";
    private static final String DAYS_8_14 = "days_8_14";
    private static final String DAYS_15_21 = "days_15_21";
    private static final String DAYS_22_30 = "days_22_30";
    private static final String PREVIOUS_DEBT = "previous_debt";
    private static final String CREDIT_PURCHASES = "credit_purchases";
    private static final String DEBT_PAYMENT = "debt_payment";
    private static final String CREDIT_LIMIT = "credit_limit";
    private static final String CREDIT_AVAILABLE = "credit_available";
    private static final String CUSTOMER_NAME = "customer_name";
    private static final String QUANTITY_PRODUCTS = "quantity_products";
    private static final String SALES_DETAIL = "sales_detail";
    private static final String ROUTE = "route";

    @Override
    public String getTableName() {
        return "customer";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        JsonObject _config = config();
        URI = _config.getString("invoice_server_host");
        URI_PARCEL = _config.getString("invoice_parcel_server_host");
        DOUBLE_INVOICING = _config.getBoolean("double_invoicing");
        CUSTOMER_INVOICE_PREFIX = _config.getString("customer_invoice_prefix");
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_REGISTER:
                this.register(message);
                break;
            case ACTION_EXTERNAL_REGISTER:
                this.externalRegister(message);
                break;
            case ACTION_APP_CLIENT_REGISTER:
                this.appClientRegister(message);
                break;
            case ACTION_REPORT:
                this.report(message);
                break;
            case ACTION_CONFIRM_ACCOUNT:
                this.confirmAccount(message);
                break;
            case ACTION_REGISTER_BILLING_INFORMATION:
                this.registerBillingInformation(message);
                break;
            case ACTION_CONFIRM_BILLING_INFORMATION:
                this.confirmRegisterBillingInformation(message);
                break;
            case ACTION_FIND_BILLING_INFORMATION_BY_CUSTOMER_ID:
                this.findBillingInformationByCustomerID(message);
                break;
            case ACTION_FIND_BILLING_INFORMATION_BY_RFC_AND_ZIP_CODE:
                this.findBillingInformationByRFCAndZipCode(message);
                break;
            case ACTION_SUBSCRIPTION:
                this.subscription(message);
                break;
            case ACTION_SEARCH_BY_NAME_AND_LASTNAME:
                this.searchCustomerByName(message);
                break;
            case ACTION_LOCK_CUSTOMER_CREDIT:
                this.lockCustomerCredit(message);
                break;
            case ACTION_TRIGGER_CUSTOMER_INVOICES:
                this.triggerProcessBillingInvoices(message);
                break;
            case ACTION_GET_CUSTOMER_CREDIT:
                this.getCustomerCredit(message);
                break;
            case ACTION_GET_CUSTOMER_CREDIT_BY_PARCELID:
                this.getCustomerCreditbyParcelId(message);
                break;
            case ACTION_SYNC_CUSTOMER_BILLINGS:
                this.syncPendingBilling(message);
                break;
            case ACTION_GET_DEBTS:
                this.getDebtListByCustomerId(message);
                break;
            case ACTION_CUSTOMER_WALLET_REPORT:
                this.walletReport(message);
                break;
            case ACTION_CREATE_REPORT_CUSTOMERS:
                this.reportCustomers(message);
                break;
            case ACTION_CUSTOMER_WALLET_DETAIL_REPORT:
                this.walletDetailReport(message);
                break;
            case ACTION_GET_CUSTOMER_DEBT:
                this.getCustomerDebt(message); 
                break;
            case ACTION_GET_CUSTOMER_BY_USER_ID:
                this.getCustomerByUserId(message);
                break;
            case ACTION_UPDATE_CUSTOMER_INFO:
                this.updateCustomer(message);
                break;
            case ACTION_BILLING_INFORMATION_RFC:
                this.checkRFC(message);
                break;
            case ACTION_BILLING_DELETE_INFO:
                this.deleteBilling(message);
                break;
            case ACTION_ACCOUNT_STATUS_REPORT:
                this.accountStatusReport(message);
                break;
            case ACTION_GET_DETAIL:
                this.getDetail(message);
                break;
            case ACTION_UPDATE_BASIC_INFO:
                this.updateBasicInfo(message);
                break;
            case ACTION_CREATE_PASSENGER:
                this.createPassenger(message);
                break;
            case ACTION_UPDATE_PASSENGER:
                this.updatePassenger(message);
                break;
            case ACTION_GET_PASSENGERS_BY_CUSTOMER_ID:
                this.getPassengersByCustomerId(message);
                break;
            case ACTION_CREATE_FCM_TOKEN:
                this.createFCMToken(message);
                break;
            case ACTION_GET_FCM_TOKENS:
                this.getFCMTokens(message);
                break;
            case ACTION_VERIFY_WITH_PHONE:
                this.verifyWithPhone(message);
                break;
            case ACTION_GET_CUSTOMERS_BY_ADVISER_ID:
                this.getCustomersByAdviserId(message);
                break;
            case ACTION_FIND_ALL_BILLING_INFORMATION_BY_CUSTOMER_ID:
                this.findAllBillingInformationByCustomerID(message);
                break;
            case ACTION_GET_SERVICES_WITH_DEBT:
                this.getServicesWithDebt(message);
                break;
            case ACTION_SEARCH_V2:
                this.searchV2(message);
                break;
            case ACTION_GET_CUSTOMERS_CATALOGUE_LIST:
                this.getCustomersCatalogueList(message);
                break;
            case ACTION_IS_ASSIGNED:
                this.isAssigned(message);
                break;
        }
    }

    private void register(Message<JsonObject> message){
        this.startTransaction(message, con -> {
            try {
                JsonObject customer = message.body().copy();
                customer.remove("billing_info");
                Boolean registerCustomerCBI = customer.containsKey("register_customer_cbi")
                        ? (Boolean) customer.remove("register_customer_cbi")
                        : false;
                GenericQuery customerCreate = this.generateGenericCreate(customer);
                con.updateWithParams(customerCreate.getQuery(), customerCreate.getParams(), customerReply -> {
                    try{
                        if(customerReply.failed()){
                            throw  new Exception(customerReply.cause());
                        }

                        int id = customerReply.result().getKeys().getInteger(0);

                        //insert batch of contact info and bank info
                        List<String> batch = new ArrayList<>();
                        JsonObject billingInfo = message.body().getJsonObject("billing_info");
                        if (billingInfo != null) {
                            billingInfo.put(CUSTOMER_ID, id);
                            String query = this.generateGenericCreate( "customer_billing_information" , billingInfo);
                            con.update(query, reply ->{
                                try{
                                    if(reply.failed()){
                                        throw new Exception(reply.cause());
                                    }

                                    int idBilling = reply.result().getKeys().getInteger(0);
                                    billingInfo.put(ID, idBilling);
                                    batch.add(generateGenericCreate("customer_billing_information", billingInfo));
                                    insertCustomerContPAQ(con , billingInfo).setHandler(result -> {
                                        try{
                                            if (result.failed()) {
                                                throw new Exception(reply.cause());
                                            }

                                            if(registerCustomerCBI) {
                                                JsonObject customerCBI = new JsonObject()
                                                        .put(CUSTOMER_ID, id)
                                                        .put(_CUSTOMER_BILLING_INFORMATION_ID, idBilling)
                                                        .put(CREATED_BY, customer.getInteger(CREATED_BY));
                                                String queryCustomerCBI = this.generateGenericCreate( "customer_customer_billing_info" , customerCBI);
                                                con.update(queryCustomerCBI, replyCustomerCBI ->{
                                                    try {
                                                        if(replyCustomerCBI.failed()){
                                                            throw new Exception(replyCustomerCBI.cause());
                                                        }
                                                        this.commit(con, message, new JsonObject().put(ID, id));
                                                    } catch (Exception e ) {
                                                        e.printStackTrace();
                                                        this.rollback(con, e, message);
                                                    }
                                                });
                                            } else {
                                                this.commit(con, message, new JsonObject().put(ID, id));
                                            }
                                        }catch (Exception e){
                                            e.printStackTrace();
                                            this.rollback(con, e, message);
                                        }
                                    });
                                }catch (Exception e ){
                                    e.printStackTrace();
                                    this.rollback(con, e, message);
                                }
                            });
                        } else {
                            this.commit(con, message, new JsonObject().put(ID, id));
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                        this.rollback(con, e, message);
                    }
                });
            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(con, t, message);
            }
        });
    }

    private Future<JsonObject> insertCustomerContPAQ(SQLConnection conn, JsonObject customer) {

        Future<JsonObject> future = Future.future();
        try {
            String queryAddress = "SELECT street.name AS street_name,\n"
                    .concat("county.name AS county_name,\n").concat("suburb.name AS suburb_name,\n")
                    .concat("city.name AS city_name,\n").concat("state.name AS state_name,\n")
                    .concat("country.name AS country_name\n")
                    .concat("FROM street\n")
                    .concat("LEFT JOIN county ON county.id = ?\n")
                    .concat("LEFT JOIN suburb ON suburb.id = ?\n")
                    .concat("LEFT JOIN city ON city.id = ?\n")
                    .concat("LEFT JOIN state ON state.id = ?\n")
                    .concat("LEFT JOIN country ON country.id = ?\n")
                    .concat("WHERE street.id = ?;");
            JsonArray queryParams = new JsonArray()
                    .add(customer.getValue("county_id"))
                    .add(customer.getValue("suburb_id"))
                    .add(customer.getValue("city_id"))
                    .add(customer.getValue("state_id"))
                    .add(customer.getValue("country_id"))
                    .add(customer.getValue("street_id"));
            conn.queryWithParams(queryAddress, queryParams, replyAddress -> {
                try {
                    if (replyAddress.failed()) {
                        throw new Exception(replyAddress.cause());
                    }

                    List<JsonObject> address = replyAddress.result().getRows();
                    if (address.isEmpty()) {
                        throw new Exception("Street not found");
                    }

                    // Add address information
                    customer.mergeIn(address.get(0));

                    doInsertCustomerContPAQ(conn, customer, URI, InvoiceInstance.GENERAL, 1).whenComplete((JsonObject reply, Throwable replyError) -> {
                        try {
                            if (replyError != null) {
                                throw new Exception(replyError);
                            }

                            if (DOUBLE_INVOICING) {
                                doInsertCustomerContPAQ(conn, customer, URI_PARCEL, InvoiceInstance.PARCEL, 2).whenComplete((JsonObject replyParcel, Throwable replyErrorParcel) -> {
                                    try {
                                        if (replyErrorParcel != null) {
                                            throw new Exception(replyErrorParcel);
                                        }

                                        future.complete(replyParcel);

                                    } catch(Exception ex) {
                                        ex.printStackTrace();
                                        future.fail(ex);
                                    }
                                });
                            } else {
                                future.complete(reply);
                            }

                        } catch(Exception ex) {
                            ex.printStackTrace();
                            future.fail(ex);
                        }
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

    private CompletableFuture<JsonObject> doInsertCustomerContPAQ(SQLConnection conn, JsonObject customer, String host, InvoiceInstance instance, Integer empresa) {

        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        try {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Integer ID = customer.getInteger("id");
            JsonObject customerContPAQ = new JsonObject();
            customerContPAQ.put("cCodigoCliente", CUSTOMER_INVOICE_PREFIX.concat(ID.toString()));
            customerContPAQ.put("cRazonSocial",customer.getString("name"));
            customerContPAQ.put("cFechaAlta", format.format(UtilsDate.parse_yyyy_MM_dd(UtilsDate.format_yyyy_MM_dd(UtilsDate.getLocalDate()))));
            customerContPAQ.put("cRFC",customer.getString("rfc"));
            customerContPAQ.put("cCURP","");
            customerContPAQ.put("cDenComercial","");
            customerContPAQ.put("cRepLegal","");
            customerContPAQ.put("cNombreMoneda","Peso Mexicano");
            customerContPAQ.put("cListaPreciosCliente",0);
            customerContPAQ.put("cDescuentoMovto",0.0);
            customerContPAQ.put("cBanVentaCredito",0);
            customerContPAQ.put("cCodigoValorClasificacionCliente1","");
            customerContPAQ.put("cCodigoValorClasificacionCliente2","");
            customerContPAQ.put("cCodigoValorClasificacionCliente3","");
            customerContPAQ.put("cCodigoValorClasificacionCliente4","");
            customerContPAQ.put("cCodigoValorClasificacionCliente5","");
            customerContPAQ.put("cCodigoValorClasificacionCliente6","");
            customerContPAQ.put("cTipoCliente",1);
            customerContPAQ.put("cEstatus",1);
            customerContPAQ.put("cFechaBaja","0001-01-01T00:00:00");
            customerContPAQ.put("cFechaUltimaRevision","0001-01-01T00:00:00");
            customerContPAQ.put("cLimiteCreditoCliente",0);
            customerContPAQ.put("cDiasCreditoCliente",0);
            customerContPAQ.put("cBanExcederCredito",0);
            customerContPAQ.put("cDescuentoProntoPago",0.0);
            customerContPAQ.put("cDiasProntoPago",0);
            customerContPAQ.put("cInteresMoratorio",0.0);
            customerContPAQ.put("cDiaPago",0);
            customerContPAQ.put("cDiasRevision",0);
            customerContPAQ.put("cMensajeria","");
            customerContPAQ.put("cDiasEmbarqueCliente",0);
            customerContPAQ.put("cCodigoAlmacen","");
            customerContPAQ.put("cCodigoAgenteVenta","");
            customerContPAQ.put("cCodigoAgenteCobro","");
            customerContPAQ.put("cRestriccionAgente",0);
            customerContPAQ.put("cImpuesto1",0.0);
            customerContPAQ.put("cImpuesto2",0.0);
            customerContPAQ.put("cImpuesto3",0.0);
            customerContPAQ.put("cRetencionCliente1",0.0);
            customerContPAQ.put("cRetencionCliente2",0.0);
            customerContPAQ.put("cCodigoValorClasificacionProveedor1","");
            customerContPAQ.put("cCodigoValorClasificacionProveedor2","");
            customerContPAQ.put("cCodigoValorClasificacionProveedor3","");
            customerContPAQ.put("cCodigoValorClasificacionProveedor4","");
            customerContPAQ.put("cCodigoValorClasificacionProveedor5","");
            customerContPAQ.put("cCodigoValorClasificacionProveedor6","");
            customerContPAQ.put("cLimiteCreditoProveedor",0.0);
            customerContPAQ.put("cDiasCreditoProveedor",0);
            customerContPAQ.put("cTiempoEntrega",0);
            customerContPAQ.put("cDiasEmbarqueProveedor",0);
            customerContPAQ.put("cImpuestoProveedor1",0.0);
            customerContPAQ.put("cImpuestoProveedor2",0.0);
            customerContPAQ.put("cImpuestoProveedor3",0.0);
            customerContPAQ.put("cRetencionProveedor1",0.0);
            customerContPAQ.put("cRetencionProveedor2",0.0);
            customerContPAQ.put("cBanInteresMoratorio",0);
            customerContPAQ.put("cTextoExtra1","");
            customerContPAQ.put("cTextoExtra2","");
            customerContPAQ.put("cTextoExtra3","");
            customerContPAQ.put("cFechaExtra","0001-01-01T00:00:00");
            customerContPAQ.put("cImporteExtra1",0.0);
            customerContPAQ.put("cImporteExtra2",0.0);
            customerContPAQ.put("cImporteExtra3",0.0);
            customerContPAQ.put("cImporteExtra4",0.0);
            customerContPAQ.put("cTipoCatalogo", 1);
            customerContPAQ.put("cTipoDireccion", 1);
            customerContPAQ.put("cCodigoPostal", customer.getValue("zip_code"));
            customerContPAQ.put("cPais", customer.getValue("country_name"));
            customerContPAQ.put("cEstado", customer.getValue("state_name"));
            customerContPAQ.put("cCiudad", customer.getValue("city_name"));
            customerContPAQ.put("cColonia", customer.getValue("suburb_name", ""));
            customerContPAQ.put("cNombreCalle", customer.getValue("street_name"));
            customerContPAQ.put("cNumeroExterior", customer.getValue("no_ext"));
            customerContPAQ.put("cNumeroInterior", customer.getValue("no_int", ""));
            customerContPAQ.put("cDireccionWeb", "cliente.com");
            customerContPAQ.put("cEmail", "correo@mail.com");
            customerContPAQ.put("cTelefono1", "6681967928");
            customerContPAQ.put("cTelefono2", "");
            customerContPAQ.put("cTelefono3", "");
            customerContPAQ.put("cTelefono4", "");
            customerContPAQ.put("cTextoExtra", "");
            customerContPAQ.put("CMETODOPAG", "01");
            customerContPAQ.put("CBANCFD", 1);
            customerContPAQ.put("CUSOCFDI", "P01");
            customerContPAQ.put("empresa", empresa);
            customerContPAQ.put("estatusServicio", 0);
            customerContPAQ.put("tipoPeticion", "POST");

            WebClient client = WebClient.create(this.vertx);

            String insertCustomer = this.generateGenericCreate("CONTPAQ_ClientesProvedores", customerContPAQ);
            conn.update(insertCustomer, (AsyncResult<UpdateResult> reply) -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    final int contpaqID = reply.result().getKeys().getInteger(0);

                    String queryUpdate = "UPDATE customer_billing_information SET "
                            .concat(instance.getFieldId()).concat(" = ? WHERE id = ?");
                    JsonArray params = new JsonArray()
                            .add(contpaqID).add(ID);

                    conn.queryWithParams(queryUpdate, params, replyUpdate -> {
                        try {
                            if (replyUpdate.failed()) {
                                throw new Exception(replyUpdate.cause());
                            }

                            future.complete(new JsonObject().put("id", contpaqID));

                        } catch(Exception ex) {
                            ex.printStackTrace();
                            future.completeExceptionally(ex);
                        }
                    });
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
                client.close();
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> insertClientContpaq(SQLConnection conn, JsonObject client) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String insert = this.generateGenericCreate("CONTPAQ_ClientesProvedores", client);
        conn.update(insert, (AsyncResult<UpdateResult> reply) -> {
            try {
                if (reply.succeeded()) {
                    final int id = reply.result().getKeys().getInteger(0);
                    client.put("id", id);
                    future.complete(client);
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

    private void registerBillingInformation(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body();
                String insert = generateGenericCreate("customer_billing_information", body);
                conn.update(insert, result -> {
                    try {
                        if (result.failed()) {
                            throw new Exception(result.cause());
                        }

                        int id = result.result().getKeys().getInteger(0);
                        body.put("id", id);
                        insertCustomerContPAQ(conn, body).setHandler(reply -> {
                            try {
                                if (reply.failed()) {
                                    throw new Exception(reply.cause());
                                }

                                this.commit(conn, message, new JsonObject().put("id", id));

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
    }

    private void confirmRegisterBillingInformation(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body();
                String code = body.getString("cCodigoCliente").replace(CUSTOMER_INVOICE_PREFIX, "");
                Integer contpaqID = body.getInteger("id");
                Integer billingID = Integer.valueOf(code);

                Integer instanceId = body.getInteger("instance_id");
                InvoiceInstance instance = InvoiceInstance.getInstanceById(instanceId);
                if (instance == null) {
                    throw new Exception("Invalid instance id for invoicing".concat(String.valueOf(instanceId)));
                }

                doConfirmRegisterBillingInformation(conn, billingID, contpaqID, instance).setHandler(reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        this.commit(conn, message, new JsonObject());

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

    private Future<Void> doConfirmRegisterBillingInformation(SQLConnection conn, Integer billingID,
                                                             Integer contpaqID, InvoiceDBV.InvoiceInstance instance) {
        Future<Void> future = Future.future();
        try {
            String contpaqIDField = instance.getFieldId();
            String contpaqStatusField = instance.getFieldStatus();
            String update = "UPDATE customer_billing_information SET ".concat(contpaqStatusField)
                    .concat(" = 'done' WHERE id = ? AND ")
                    .concat(contpaqIDField).concat(" = ?;");

            JsonArray params = new JsonArray().add(billingID)
                    .add(contpaqID);

            conn.updateWithParams(update, params, result -> {
                try {
                    if (result.failed()) {
                        throw new Exception(result.cause());
                    }

                    if (result.result().getUpdated() == 0) {
                        throw new Exception("Customer: not found");
                    }

                    future.complete();

                    // Process all pending invoices
                    processBillingInvoices(billingID);

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

    private void syncPendingBilling(Message<JsonObject> message) {
        startTransaction(message, conn -> conn.query(QUERY_BILLING_INFORMATION, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                List<JsonObject> billings = reply.result().getRows();
                if (billings.isEmpty()) {
                    this.commit(conn, message, new JsonObject()
                        .put("message", "All billings are synced"));
                } else {

                    List<Future> futures = billings.stream()
                            .map(b -> syncOneBilling(conn, b))
                            .collect(toList());

                    CompositeFuture.all(futures).setHandler(replyAll -> {
                        try {
                            if (replyAll.failed()) {
                                throw replyAll.cause();
                            }

                            this.commit(conn, message, new JsonObject()
                                    .put("message", "Billing synced: ".concat(String.valueOf(billings.size()))));

                        } catch (Throwable ex) {
                            ex.printStackTrace();
                            this.rollback(conn, ex, message);
                        }
                    });
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                this.rollback(conn, ex, message);
            }
        }));
    }


    private Future<JsonObject> syncOneBilling(SQLConnection conn, JsonObject billing) {
        String contpaqStatus = billing.getString("contpaq_status");
        String contpaqParcelStatus = billing.getString("contpaq_parcel_status");
        Future<JsonObject> future = Future.future();
        List<CompletableFuture<JsonObject>> tasks = new ArrayList<>();
        if (contpaqStatus.equals("pending")) {
            tasks.add(this.doInsertCustomerContPAQ(conn, billing, URI, InvoiceInstance.GENERAL, 1));
        }

        if (contpaqParcelStatus.equals("pending")) {
            tasks.add(this.doInsertCustomerContPAQ(conn, billing, URI_PARCEL, InvoiceInstance.PARCEL, 2));
        }

        CompletableFuture<Void> alltasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));
        alltasks.whenComplete((resultAllTasks, errorAllTasks) -> {
            if (errorAllTasks != null) {
                future.fail(errorAllTasks);
            } else {
                future.complete(billing);
            }
        });

        return future;
    }

    private void triggerProcessBillingInvoices(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String code = body.getString("cCodigoCliente").replace(CUSTOMER_INVOICE_PREFIX, "");
            Integer billingID = Integer.valueOf(code);
            String query = "SELECT * FROM customer_billing_information WHERE id = ?;";
            JsonArray params = new JsonArray()
                    .add(billingID);

            this.dbClient.queryWithParams(query, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    List<JsonObject> customers = reply.result().getRows();
                    if (customers.isEmpty()) {
                        throw new Exception("Customer not found: ".concat(code));
                    }

                    JsonObject customer = customers.get(0);

                    String contpaqStatus = customer.getString("contpaq_status", "pending");
                    String contpaqParcelStatus = customer.getString("contpaq_parcel_status", "pending");
                    if (contpaqStatus.equalsIgnoreCase("done") &&
                        contpaqParcelStatus.equalsIgnoreCase("done")) {
                        processBillingInvoices(billingID);
                    }

                    message.reply(new JsonObject());

                } catch (Exception e) {
                    e.printStackTrace();
                    reportQueryError(message, e);
                }
            });

        } catch(Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }
    private void processBillingInvoices(Integer billingId) {
        String query = "SELECT * FROM customer_billing_information WHERE id = ?;";
        JsonArray params = new JsonArray().add(billingId);

        this.dbClient.queryWithParams(query, params, replyFind -> {
            try {
                if (replyFind.failed()) {
                    throw new Exception(replyFind.cause());
                }

                List<JsonObject> billings = replyFind.result().getRows();
                if (billings.isEmpty()) {
                    throw new Exception("Customer billing information not found: ".concat(String.valueOf(billingId)));
                }

                JsonObject billing = billings.get(0);
                billing.put("clientCode", CUSTOMER_INVOICE_PREFIX.concat(String.valueOf(billingId)));

                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.PROCESS_BY_BILLING);
                this.vertx.eventBus().send(InvoiceDBV.class.getSimpleName(), billing, options, reply -> {
                    try {
                        if (reply.failed()) {
                            throw new Exception(reply.cause());
                        }

                        System.out.println("Invoices process for billing");

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

    }

    private void findBillingInformationByCustomerID(Message<JsonObject> message) {
        try {
            JsonArray params = new JsonArray().add(message.body().getValue("customerID"));
            doFindBillingInformation(QUERY_FIND_BILLING_INFORMATION_BY_CUSTOMER_ID, params, message);
        } catch (Exception e) {
            reportQueryError(message, e.getCause());
        }
    }

    private void findBillingInformationByRFCAndZipCode(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String query = QUERY_FIND_BILLING_INFORMATION + " cbi.rfc = ? AND cbi.zip_code = ?";
            JsonArray params = new JsonArray()
                    .add(body.getValue("rfc"))
                    .add(body.getValue("zipCode"));
            doFindBillingInformation(query, params, message);
        } catch (Exception e) {
            reportQueryError(message, e.getCause());
        }
    }

    private void doFindBillingInformation(String query, JsonArray params, Message<JsonObject> message) {
        this.dbClient.queryWithParams(query, params, reply -> {
            try {
                if (reply.failed()) {
                    throw new Exception(reply.cause());
                }

                message.reply(new JsonArray(reply.result().getRows()));

            } catch (Exception e) {
                reportQueryError(message, e);
            }
        });
    }

    private void subscription(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String email = body.getString("email");
            Integer createdBy = body.getInteger("created_by");
            String update = "INSERT INTO subscriptions (email, created_by) VALUES (?, ?);";

            JsonArray params = new JsonArray().add(email).add(createdBy);

            this.dbClient.updateWithParams(update, params, result -> {
                try {
                    if (result.failed()) {
                        throw new Exception(result.cause());
                    }

                    message.reply(new JsonObject());

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

    private void externalRegister(Message<JsonObject> message){
        this.startTransaction(message, con -> {
            JsonObject customer = message.body().copy();
            String email = customer.getString("email");
            String pass = UtilsSecurity.encodeSHA256(customer.getString("pass"));
            customer.remove("pass");
            String token = UtilsJWT.generateHash();
            customer.put("token", token);
            JsonArray param = new JsonArray().add(email);
            String fullName = customer.getString("first_name").concat(" ").concat(customer.getString("last_name"));

            Future<ResultSet> f1 = Future.future();
            Future<ResultSet> f2 = Future.future();

            con.queryWithParams(QUERY_VALIDATION_CUSTOMER_EMAIL, param, f1.completer());
            con.queryWithParams(QUERY_VALIDATION_USER_EMAIL, param, f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try{
                    if (reply.succeeded()) {

                        List<JsonObject> customerList = reply.result().<ResultSet>resultAt(0).getRows();
                        List<JsonObject> userList = reply.result().<ResultSet>resultAt(1).getRows();

                        if (!userList.isEmpty()){
                            this.rollback(con, new Throwable("Email already in use"), message);
                        } else {
                            JsonObject existingCustomer = customerList.isEmpty() ? null : customerList.get(0);
                            JsonObject userInsertObj = this.getInsertUserObject(email, pass, existingCustomer, fullName, customer);
                            String userCreate = this.generateGenericCreate("users", userInsertObj);
                            con.update(userCreate, userReply -> {
                                try{
                                    if (userReply.succeeded()){
                                        Integer userID = userReply.result().getKeys().getInteger(0);
                                        customer.put("user_id", userID);
                                        this.validateCustomer(con, email).whenComplete((resultValidateCustomer, errorValidatecustomer) -> {
                                            try{
                                                if (errorValidatecustomer != null){
                                                    errorValidatecustomer.printStackTrace();
                                                    this.rollback(con, errorValidatecustomer, message);
                                                } else {
                                                    if (resultValidateCustomer.equals(CREATE)){
                                                        GenericQuery customerCreate = this.generateGenericCreate(customer);
                                                        con.updateWithParams(customerCreate.getQuery(), customerCreate.getParams(), customerReply -> {
                                                            try{
                                                                if (customerReply.succeeded()){
                                                                    Integer customerID = customerReply.result().getKeys().getInteger(0);
                                                                    this.sendConfirmationEmail(con, message, customerID, userID, fullName, email, customer.getString("token"), true);
                                                                } else {
                                                                    customerReply.cause().printStackTrace();
                                                                    this.rollback(con, customerReply.cause(), message);
                                                                }
                                                            } catch (Exception e){
                                                                e.printStackTrace();
                                                                this.rollback(con, e, message);
                                                            }
                                                        });
                                                    } else {
                                                        customer.put(ID, resultValidateCustomer);
                                                        GenericQuery customerUpdate = this.generateGenericUpdate("customer", customer);
                                                        con.updateWithParams(customerUpdate.getQuery(), customerUpdate.getParams(), customerReply -> {
                                                            try{
                                                                if (customerReply.succeeded()){
                                                                    Integer customerID = (Integer) resultValidateCustomer;
                                                                    //commit
                                                                    this.sendConfirmationEmail(con, message, customerID, userID, fullName, email, customer.getString("token"), false);
                                                                } else {
                                                                    customerReply.cause().printStackTrace();
                                                                    this.rollback(con, customerReply.cause(), message);
                                                                }
                                                            } catch (Exception e){
                                                                e.printStackTrace();
                                                                this.rollback(con, e, message);
                                                            }
                                                        });
                                                    }
                                                }
                                            } catch (Exception e){
                                                e.printStackTrace();
                                                this.rollback(con, e, message);
                                            }
                                        });

                                    } else {
                                        userReply.cause().printStackTrace();
                                        this.rollback(con, userReply.cause(), message);
                                    }
                                } catch (Exception e){
                                    e.printStackTrace();
                                    this.rollback(con, e, message);
                                }
                            });
                        }
                    } else {
                        reply.cause().printStackTrace();
                        this.rollback(con, reply.cause(), message);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    this.rollback(con, e, message);
                }
            });

            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            /*UtilsBridge authBridge = new UtilsBridge(this.vertx, config().getString("auth_host"), config().getInteger("auth_port"));

            con.queryWithParams(QUERY_VALIDATION_CUSTOMER_EMAIL, param, reply -> {
                if (reply.succeeded()){
                    List<JsonObject> rows = reply.result().getRows();
                    if (rows.size() > 0){
                        JsonObject result = rows.get(0);
                        // asign existing full name customer
                        String fullNameExisting = result.getString("first_name").concat(" ").concat(result.getString("last_name"));
                        paramAuth.put("name", fullNameExisting)
                                .put("phone", rows.get(0).getString("phone"));
                    } else {
                        // asing name of params
                        paramAuth.put("name", fullName)
                                .put("phone", customer.getString("phone"));
                    }



                    authBridge.get("/employees?query=email='" + email + "'").whenComplete((JsonObject responseEmail, Throwable error) -> {
                        if (error != null) {
                            this.rollback(con, error, message);
                        } else if(responseEmail.getJsonArray("data").size() != 0) {
                            this.rollback(con, new Throwable("The email already exists in auth"), message);
                        } else {
                            //register user in auth
                            authBridge.post("/employees", paramAuth).whenComplete((JsonObject response, Throwable errorInsert) -> {
                                if (errorInsert != null){
                                    try{
                                        JsonObject errorResponse = new JsonObject(errorInsert.getMessage());
                                        JsonObject data = errorResponse.getJsonObject("data");
                                        this.rollback(con, new Throwable(errorResponse.getValue("devMessage")
                                                +". "+data.getValue("name")+" "+data.getValue("error")), message);
                                    }catch(DecodeException errorInsertEx){
                                        this.rollback(con, errorInsert, message);
                                    }
                                } else {
                                    Integer status = 0;
                                    if (rows.size() > 0) {
                                        status = rows.get(0).getInteger("status");
                                    }
                                    int user_id = response.getJsonObject("data").getInteger("id");
                                    //if the mail exists in customer or if it exists and is already deleted
                                    if (rows.size() == 0  || (rows.size() > 0 && status == 3)){
                                        //register customer
                                        customer.put("user_id", user_id);
                                        GenericQuery customerCreate = this.generateGenericCreate(customer);
                                        con.updateWithParams(customerCreate.getQuery(), customerCreate.getParams(), customerReply -> {
                                            if (customerReply.succeeded()) {
                                                int customer_id = customerReply.result().getKeys().getInteger(0);
                                                //send confirmation email
                                                this.sendConfirmationEmail(con, message, customer_id, user_id, fullName, email, customer.getString("token"), true);
                                            } else {
                                                this.rollback(con, customerReply.cause(), message);
                                            }
                                        });
                                    }else if (rows.get(0).getInteger("user_id") != null){
                                        this.rollback(con, new Throwable("The customer is already assigned a user"), message);
                                    } else {
                                        //update user_id of the existing customer
                                        int customer_id = rows.get(0).getInteger("id");
                                        JsonArray paramUpateCustomer = new JsonArray()
                                                .add(user_id)
                                                .add(token)
                                                .add(customer_id);
                                        con.updateWithParams(QUERY_UPDATE_CUSTOMER_USER_ID, paramUpateCustomer, customerReply -> {
                                            if (customerReply.succeeded()) {
                                                //send confirmation email
                                                this.sendConfirmationEmail(con, message, customer_id, user_id, fullName, email, customer.getString("token"), false);
                                            } else {
                                                this.rollback(con, customerReply.cause(), message);
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                } else {
                    this.rollback(con, reply.cause(), message);
                }
            });*/
        });
    }

    private void appClientRegister(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            JsonObject customer = message.body().copy();
            String email = customer.getString("email");
            String pass = UtilsSecurity.encodeSHA256(customer.getString("pass"));
            customer.remove("pass");
            String token = UtilsJWT.generateHash();
            customer.put("token", token);
            JsonArray param = new JsonArray().add(email);
            String fullName = customer.getString("first_name").concat(" ").concat(customer.getString("last_name"));
            String referralCode = customer.getString("referral_code");
            customer.remove("referral_code");
            Boolean socialAuth = customer.getBoolean("social_auth", false);
            customer.remove("social_auth");

            Future<ResultSet> f1 = Future.future();
            Future<ResultSet> f2 = Future.future();

            con.queryWithParams(QUERY_VALIDATION_CUSTOMER_EMAIL, param, f1.completer());
            con.queryWithParams(QUERY_VALIDATION_USER_EMAIL, param, f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try{
                    if (reply.succeeded()) {

                        List<JsonObject> customerList = reply.result().<ResultSet>resultAt(0).getRows();
                        List<JsonObject> userList = reply.result().<ResultSet>resultAt(1).getRows();

                        if (!userList.isEmpty()){
                            this.rollback(con, new Throwable("Email already in use"), message);
                        } else {
                            JsonObject existingCustomer = customerList.isEmpty() ? null : customerList.get(0);
                            JsonObject userInsertObj = this.getInsertUserAppClientObject(email, pass, existingCustomer, fullName, customer, socialAuth );
                            String userCreate = this.generateGenericCreate("users", userInsertObj);
                            con.update(userCreate, userReply -> {
                                try{
                                    if (userReply.succeeded()){
                                        Integer userID = userReply.result().getKeys().getInteger(0);
                                        customer.put("user_id", userID);

                                        this.initWallet(con, userID, referralCode).whenComplete((resultInitWallet, errorInitWallet) -> {
                                            if (errorInitWallet != null){
                                                errorInitWallet.printStackTrace();
                                                this.rollback(con, errorInitWallet, message);
                                            } else {
                                                this.validateCustomer(con, email).whenComplete((resultValidateCustomer, errorValidatecustomer) -> {
                                                    try{
                                                        if (errorValidatecustomer != null){
                                                            errorValidatecustomer.printStackTrace();
                                                            this.rollback(con, errorValidatecustomer, message);
                                                        } else {
                                                            if (resultValidateCustomer.equals(CREATE)){
                                                                GenericQuery customerCreate = this.generateGenericCreate(customer);
                                                                con.updateWithParams(customerCreate.getQuery(), customerCreate.getParams(), customerReply -> {
                                                                    try{
                                                                        if (customerReply.succeeded()){
                                                                            Integer customerID = customerReply.result().getKeys().getInteger(0);
                                                                            this.commit(con, message, new JsonObject()
                                                                                    .put("new_customer", true)
                                                                                    .put("id_customer", customerID)
                                                                                    .put("user_id", userID));
                                                                        } else {
                                                                            customerReply.cause().printStackTrace();
                                                                            this.rollback(con, customerReply.cause(), message);
                                                                        }
                                                                    } catch (Exception e){
                                                                        e.printStackTrace();
                                                                        this.rollback(con, e, message);
                                                                    }
                                                                });
                                                            } else {
                                                                customer.put(ID, resultValidateCustomer);
                                                                GenericQuery customerUpdate = this.generateGenericUpdate("customer", customer);
                                                                con.updateWithParams(customerUpdate.getQuery(), customerUpdate.getParams(), customerReply -> {
                                                                    try{
                                                                        if (customerReply.succeeded()){
                                                                            Integer customerID = (Integer) resultValidateCustomer;
                                                                            //commit
                                                                            this.commit(con, message, new JsonObject()
                                                                                    .put("new_customer", false)
                                                                                    .put("id_customer", customerID)
                                                                                    .put("user_id", userID));
                                                                        } else {
                                                                            customerReply.cause().printStackTrace();
                                                                            this.rollback(con, customerReply.cause(), message);
                                                                        }
                                                                    } catch (Exception e){
                                                                        e.printStackTrace();
                                                                        this.rollback(con, e, message);
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    } catch (Exception e){
                                                        e.printStackTrace();
                                                        this.rollback(con, e, message);
                                                    }
                                                });
                                            }
                                        });
                                    } else {
                                        userReply.cause().printStackTrace();
                                        this.rollback(con, userReply.cause(), message);
                                    }
                                } catch (Exception e){
                                    e.printStackTrace();
                                    this.rollback(con, e, message);
                                }
                            });
                        }
                    } else {
                        reply.cause().printStackTrace();
                        this.rollback(con, reply.cause(), message);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    this.rollback(con, e, message);
                }
            });
        });
    }

    private JsonObject getInsertUserObject(String email, String pass, JsonObject existingCustomer, String fullName, JsonObject customer){
        JsonObject result = new JsonObject()
                .put("email", email)
                .put("pass", pass)
                .put("user_type", "C")
                .put("status", 2);

        if (existingCustomer == null){
            // asing name of params
            result.put("name", fullName)
                    .put("phone", customer.getString("phone"));
        } else {
            // asign existing full name customer
            String fullNameExisting = existingCustomer.getString("first_name").concat(" ").concat(existingCustomer.getString("last_name"));
            result.put("name", fullNameExisting)
                    .put("phone", existingCustomer.getString("phone"));
        }
        return result;
    }

    private JsonObject getInsertUserAppClientObject(String email, String pass, JsonObject existingCustomer, String fullName, JsonObject customer, Boolean socialAuth){
        JsonObject result = new JsonObject()
                .put("email", email)
                .put("pass", pass)
                .put("user_type", "C")
                .put("status", 1)
                .put("is_phone_verified", 1)
                .put("social_auth", socialAuth);

        if (existingCustomer == null){
            // asing name of params
            result.put("name", fullName)
                    .put("phone", customer.getString("phone"));
        } else {
            // asign existing full name customer
            String fullNameExisting = existingCustomer.getString("first_name").concat(" ").concat(existingCustomer.getString("last_name"));
            result.put("name", fullNameExisting)
                    .put("phone", existingCustomer.getString("phone"));
        }
        return result;
    }

    private CompletableFuture<Object> validateCustomer(SQLConnection conn, String email){
        CompletableFuture future = new CompletableFuture();
        conn.queryWithParams("SELECT * FROM customer WHERE email = ?;", new JsonArray().add(email), reply -> {
            try {
                if (reply.succeeded()){
                    List<JsonObject> customerResult = reply.result().getRows();
                    if (customerResult.isEmpty()){
                        future.complete(CREATE);
                    } else {
                        JsonObject customerObj = customerResult.get(0);
                        if (customerObj.getInteger(STATUS).equals(3)){
                            future.completeExceptionally(new Throwable("Customer has been deleted"));
                        } else if(customerObj.getBoolean("is_verified")){
                            future.completeExceptionally(new Throwable("Customer has been verified"));
                        } else if(customerObj.getInteger("user_id") != null){
                            future.completeExceptionally(new Throwable("Customer already has a user"));
                        } else {
                            future.complete(customerObj.getInteger(ID));
                        }
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

    private void sendConfirmationEmail(SQLConnection con, Message<JsonObject> message,
                                       Integer customerId, Integer userId, String fullName, String email, String token, Boolean newCustomer){
        DeliveryOptions options = new DeliveryOptions()
                .addHeader(ACTION, MailVerticle.ACTION_SEND_HTML_TEMPLATE_MAIL_MONGODB);

        this.vertx.eventBus().send(MailVerticle.class.getSimpleName(), new JsonObject()
                .put("template", "confirmationemail.html")
                .put("to", email)
                .put("subject", "Correo de confirmación")
                .put("body", new JsonObject()
                        .put("name", fullName)
                        .put("link", config().getString("web_server_hostname").concat("/confirmarcuenta/?token=").concat(token))), options, replySend -> {
                        try{
                            if(replySend.failed()) {
                                throw new Exception(replySend.cause());
                            }
                            this.commit(con, message, new JsonObject()
                                    .put("new_customer", newCustomer)
                                    .put("id_customer", customerId)
                                    .put("user_id", userId)
                                    .put("email_status", replySend.succeeded()));
                        }catch (Exception ex) {
                            replySend.cause().printStackTrace();
                            this.rollback(con, replySend.cause(), message);
                        }
        });
    }

    private void confirmAccount(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            JsonObject body = message.body();
            String token = body.getString("token");
            JsonArray param = new JsonArray().add(token);

            Future<ResultSet> f1 = Future.future();
            Future<ResultSet> f2 = Future.future();

            con.queryWithParams(QUERY_TOKEN_EXISTS, param, f1.completer());
            con.queryWithParams(QUERY_GET_VERIFICATION, param, f2.completer());

            CompositeFuture.all(f1, f2).setHandler(reply -> {
                try{
                    if(reply.failed()){
                        throw  new Exception(reply.cause());
                    }
                    Integer existsToken = Integer.valueOf(String.valueOf(reply.result().<ResultSet>resultAt(0).getRows().size()));
                    if (existsToken > 0) {
                        Integer id = reply.result().<ResultSet>resultAt(1).getRows().get(0).getInteger("id");
                        Integer userId = reply.result().<ResultSet>resultAt(1).getRows().get(0).getInteger("user_id");
                        Boolean isVerified = reply.result().<ResultSet>resultAt(1).getRows().get(0).getBoolean("is_verified");
                        if (!isVerified) {
                            JsonArray paramUpdate = new JsonArray().add(id);
                            //update is_verified field of customer for verify account
                            con.queryWithParams(QUERY_UPDATE_VERIFIED_STATUS, paramUpdate, replyUpdate -> {
                                try{
                                    if(replyUpdate.failed()){
                                        throw  new Exception(replyUpdate.cause());
                                    }
                                    con.queryWithParams("SELECT * FROM users WHERE id = ?;", new JsonArray().add(userId), replyUserVerify -> {
                                        try{
                                            if (replyUserVerify.succeeded()) {
                                                List<JsonObject> resultUserVerify = replyUserVerify.result().getRows();
                                                if (!resultUserVerify.isEmpty()) {
                                                    JsonObject userObj = resultUserVerify.get(0);
                                                    Integer idEmployee = userObj.getInteger("id");
                                                    Integer statusEmployee = userObj.getInteger("status");
                                                    if (statusEmployee == 2) {
                                                        //update status to active
                                                        GenericQuery userUpdate = this.generateGenericUpdate("users", new JsonObject().put("id", idEmployee).put("status", 1));
                                                        con.updateWithParams(userUpdate.getQuery(), userUpdate.getParams(), replyUserUpdate ->{
                                                            try{
                                                                if (replyUserUpdate.succeeded()){
                                                                    this.commit(con, message, new JsonObject()
                                                                            .put("customer_id", id)
                                                                            .put("is_verified", true)
                                                                            .put("employee_id", idEmployee)
                                                                            .put("employee_status", "Verified"));
                                                                } else {
                                                                    replyUserUpdate.cause().printStackTrace();
                                                                    this.rollback(con, replyUserUpdate.cause(), message);
                                                                }
                                                            } catch (Exception e){
                                                                e.printStackTrace();
                                                                this.rollback(con, e, message);
                                                            }
                                                        });
                                                    } else {
                                                        this.rollback(con, new Throwable("Client has already been activated\n"), message);
                                                    }
                                                } else {

                                                }
                                            } else {
                                                replyUserVerify.cause().printStackTrace();
                                                this.rollback(con, replyUserVerify.cause(), message);
                                            }
                                        } catch (Exception e){
                                            e.printStackTrace();
                                            this.rollback(con, e, message);
                                        }
                                    });

                                }catch(Exception e){
                                    replyUpdate.cause().printStackTrace();
                                    this.rollback(con, replyUpdate.cause(), message);
                                }

                            });
                        } else {
                            this.rollback(con, new Throwable("This account has already been verified"), message);
                        }
                    } else {
                        this.rollback(con, new Throwable("Token does not exist"), message);
                    }

                }catch(Exception e){
                    reply.cause().printStackTrace();
                    this.rollback(con, reply.cause(), message);
                }

            });
        });
    }

    private void report(Message<JsonObject> message) {
        Future<ResultSet> f1 = Future.future();
        Future<ResultSet> f2 = Future.future();
        Future<ResultSet> f3 = Future.future();

        this.dbClient.query(QUERY_REPORT_CUSTOMERS, f1.completer());
        this.dbClient.query(QUERY_REPORT_CUSTOMERS_BILLING_INFO, f2.completer());
        this.dbClient.query(QUERY_REPORT_CUSTOMERS_CASE_FILES, f3.completer());

        CompositeFuture.all(f1, f2, f3).setHandler(reply -> {
            try{
                if(reply.failed()){
                    throw  new Exception(reply.cause());
                }
                List<JsonObject> customers = reply.result().<ResultSet>resultAt(0).getRows();
                List<JsonObject> billingInfo = reply.result().<ResultSet>resultAt(1).getRows();
                List<JsonObject> caseFiles = reply.result().<ResultSet>resultAt(2).getRows();

                for (JsonObject customer : customers) {
                    List<JsonObject> customerBillingInfo = billingInfo.stream()
                            .filter(b -> b.getInteger("customer_id").equals(customer.getInteger("id")))
                            .collect(toList());

                    List<JsonObject> customerCaseFiles = caseFiles.stream()
                            .filter(b -> b.getInteger("customer_id").equals(customer.getInteger("id")))
                            .collect(toList());

                    customer.put("billing_info", customerBillingInfo);
                    customer.put("case_files", customerCaseFiles);
                }
                message.reply(new JsonArray(customers));

            }catch(Exception e){
                message.fail(ErrorCodes.DB_ERROR.ordinal(), reply.cause().getMessage());

            }

        });

    }

    private void searchCustomerByName(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String searchTerm = body.getString("searchTerm");
            Integer limit = body.getInteger("limit");

            searchCustomerByName(searchTerm, limit).whenComplete((results, err) -> {
                try {
                    if (err != null) {
                        throw err;
                    }
                    message.reply(new JsonArray(results));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Exception ex) {
            reportQueryError(message, ex);
        }
    }

    private CompletableFuture<List<JsonObject>> searchCustomerByName(String searchTerm, Integer limit) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        try {
            String QUERY = QUERY_SEARCH_ADVANCED;
            JsonArray param = new JsonArray();
            int customerId = 0;
            try {
                customerId = Integer.parseInt(searchTerm);
            } catch (Throwable t) {
                searchTerm = "%" + searchTerm + "%";
            }

            if (customerId == 0) {
                QUERY = QUERY.concat(SEARCH_TERM_FILTER);
                searchTerm = searchTerm.replace(' ', '%');
                param.add(searchTerm);
            } else {
                QUERY = QUERY.concat(SEARCH_ID_FILTER);
                param.add(customerId);
            }

            QUERY = QUERY.concat(SEARCH_ORDER_BY);
            if (limit > 0) QUERY += "LIMIT " + limit;
            dbClient.queryWithParams(QUERY, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(reply.result().getRows());
                } catch (Throwable ex) {
                    future.completeExceptionally(ex);
                }
            });
        } catch (Exception ex) {
            future.completeExceptionally(ex);
        }
        return future;
    }

    private void getDebtListByCustomerId(Message<JsonObject> message) {
        try {
            Integer customerId = message.body().getInteger("customer_id");
            JsonArray services = message.body().getJsonArray("services");
            JsonObject result = new JsonObject();
            for (Integer i = 0; i < services.size(); i++) {
                String service = services.getString(i);
                String QUERY = "";
                JsonArray param = new JsonArray().add(customerId);
                QUERY = service.equals("parcel") ? QUERY_SELECT_PARCEL_DEBTS : service.equals("guiapp") ? QUERY_SELECT_GUIAPP_DEBTS : service.equals("prepaid") ? QUERY_SELECT_PREPAID_DEBTS : QUERY_SELECT_BOARDING_PASS_DEBTS;
                Integer finalI = i;
                dbClient.queryWithParams(QUERY, param, reply -> {
                    try {
                        if (reply.failed()) {
                            throw reply.cause();
                        }
                        List<JsonObject> debtCustomer = reply.result().getRows();
                        result.put(service, debtCustomer);

                        if(finalI == services.size() - 1){
                            message.reply(result);
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        reportQueryError(message, ex);
                    }
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void lockCustomerCredit(Message<JsonObject> message){
        try{
            Integer customerId = message.body().getInteger("customer_id");
            this.dbClient.queryWithParams("SELECT has_sales_blocked FROM customer WHERE id = ?", new JsonArray().add(customerId), reply->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("Customer not found");
                    }
                    message.reply(result.get(0));
                } catch (Exception e){
                    reportQueryError(message,e);
                }
            });
        }catch (Exception ex){
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void getCustomerCredit (Message<JsonObject> message) {
        try {
            Integer customerId = message.body().getInteger("customer_id");
            JsonArray param = new JsonArray()
                    .add(customerId)
                    .add(customerId)
                    .add(customerId)
                    .add(customerId)
                    .add(customerId)
                    .add(customerId)
                    .add(customerId);
            dbClient.queryWithParams(QUERY_GET_CUSTOMER_CREDIT_DATA,param,reply ->{
                try{
                    if(reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> resultCreditCustomer= reply.result().getRows();
                    if(resultCreditCustomer.isEmpty()){
                        message.reply(new JsonObject());
                    } else {
                        message.reply(resultCreditCustomer.get(0));
                    }
                } catch (Throwable ex){
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void getCustomerCreditbyParcelId(Message<JsonObject> message) {
        try {
            Integer tracking_code = message.body().getInteger("parcelTrackingCode");
            JsonArray param = new JsonArray().add(tracking_code);
            dbClient.queryWithParams(QUERY_GET_CREDIT_BY_PARCELID, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> resultCreditCustomer = reply.result().getRows();
                    if (resultCreditCustomer.isEmpty()) {
                        message.reply(new JsonObject());
                    }

                    message.reply(resultCreditCustomer.get(0));

                } catch (Throwable ex) {
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void reportCustomers(Message<JsonObject> message){
        JsonObject body = message.body();
        String dateInit = body.getString("init_date");
        String dateEnd = body.getString("end_date");
        JsonArray params = new JsonArray().add(dateInit).add(dateEnd);
        String QUERY = QUERY_REPORT_LAST_CUSTOMERS;

        this.getReportLastCustomers( QUERY, params ).whenComplete(( resultReport , errorReport ) -> {
            if ( errorReport != null ){
                reportQueryError(message, errorReport);
            } else {
                message.reply(resultReport);
            }
        });
    }

    private CompletableFuture<JsonArray> getReportLastCustomers(String QUERY , JsonArray params){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        this.dbClient.queryWithParams( QUERY , params , replyReport -> {
            try{
                if (replyReport.succeeded()){
                    List<JsonObject> resultReport = replyReport.result().getRows();
                    LocalDate today = LocalDate.now();

                    resultReport.forEach(r -> {
                        if(!r.getString("birthday").equals("No registrado")){
                            LocalDate birthday = LocalDate.parse(r.getString("birthday"));
                            Period p = Period.between(birthday, today );

                            r.put("customer_age", p.getYears());

                        }else {
                            r.put("customer_age", "Falta cumpleaños");
                        }
                    });

                    future.complete(new JsonArray(resultReport));

                } else {
                    future.completeExceptionally(replyReport.cause());
                }
            } catch(Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void walletReport(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonArray sellers = body.getJsonArray("seller_array");
            Integer branchofficeId = body.getInteger("branchoffice_id");
            String init_date = body.getString(INIT_DATE);
            String end_date = body.getString(END_DATE);
            Integer limit = message.body().getInteger("limit");
            Integer page = message.body().getInteger("page");
            boolean onlyDebtors = body.getBoolean(ONLY_DEBTORS, false);
            Integer customerId = body.getInteger(CUSTOMER_ID);
            String creditExtraConditions = "";
            String customerFilter = "";
            String customerPPFilter = "";
            String rowFilter = "cc.credit_limit > 0";
            String QUERY = CUSTOMER_WALLET_REPORT;
            if(end_date == null){
                end_date = UtilsDate.sdfDataBase(new Date());
            }

            JsonArray params = new JsonArray()
                    .add(end_date)
                    .add(end_date)
                    .add(end_date)
                    .add(end_date)
                    .add(end_date)
                    .add(end_date)
                    .add(end_date)
                    .add(end_date)
                    .add(end_date);

            if (onlyDebtors) {
                creditExtraConditions += " AND pp.debt > 0";
                rowFilter += " AND COALESCE(ps.total_debt, 0) > 0";
            }

            List<JsonArray> parcelParams = new ArrayList<>();
            if(init_date != null){
                QUERY = QUERY.replace("{RANGE_OPTIONAL}", " AND (CONVERT_TZ(pp.created_at, '+00:00', '-07:00') BETWEEN ? AND ?) ");
                JsonArray pp = new JsonArray().add(init_date).add(end_date);
                parcelParams.add(pp);
            } else {
                QUERY = QUERY.replace("{RANGE_OPTIONAL}", "");
            }

            List<JsonArray> customerParams = new ArrayList<>();
            if (customerId != null) {
                customerFilter += " AND c.id = ?";
                customerPPFilter += " AND pp.customer_id = ?";
                JsonArray cp = new JsonArray().add(customerId);
                customerParams.add(cp);
            }

            if(branchofficeId != null) {
                customerFilter += " AND c.branchoffice_id = ?";
                customerPPFilter += " AND c.branchoffice_id = ?";
                JsonArray cp = new JsonArray().add(branchofficeId);
                customerParams.add(cp);
            }

            if (sellers != null) {
                StringBuilder placeholders = new StringBuilder();
                JsonArray sellerIds = new JsonArray();

                for (int i = 0; i < sellers.size(); i++) {
                    Integer id = sellers.getJsonObject(i).getInteger("id");
                    sellerIds.add(id);
                    placeholders.append("?");
                    if (i < sellers.size() - 1) {
                        placeholders.append(",");
                    }
                }
                customerFilter += " AND c.user_seller_id IN (" + placeholders + ")";
                customerPPFilter += " AND c.user_seller_id IN (" + placeholders + ")";

                JsonArray cp = new JsonArray();
                for (int i = 0; i < sellerIds.size(); i++) {
                    cp.add(sellerIds.getInteger(i));
                }
                customerParams.add(cp);
            }

            for (JsonArray pp : parcelParams) {
                addAllElements(params, pp);
            }

            for (JsonArray cp : customerParams) {
                addAllElements(params, cp);
            }

            for (JsonArray pp : parcelParams) {
                addAllElements(params, pp);
            }

            for (JsonArray cp : customerParams) {
                addAllElements(params, cp);
            }

            for (JsonArray cp : customerParams) {
                addAllElements(params, cp);
            }

            for (JsonArray cp : customerParams) {
                addAllElements(params, cp);
            }

            QUERY = QUERY.replace("{CREDIT_EXTRA_CONDITIONS}", creditExtraConditions);
            QUERY = QUERY.replace("{CUSTOMER_PARCEL_WHERE}", customerPPFilter);
            QUERY = QUERY.replace("{CUSTOMER_WHERE}", customerFilter);
            QUERY = QUERY.replace("{ROW_FILTER}", rowFilter);

            String COUNT_QUERY = QUERY;
            JsonArray countParams = params.copy();

            if(page != null) {
                QUERY = QUERY.concat(" LIMIT ?, ? ");
                params.add((page - 1) * limit).add(limit);
            }

            this.dbClient.queryWithParams(QUERY, params, replyL -> {
                try {
                    if (replyL.failed()){
                        throw replyL.cause();
                    }

                    List<JsonObject> resultsList = replyL.result().getRows();

                    if(page != null){
                        this.dbClient.queryWithParams("select count(*) as count from ({QUERY_REPORT}) as f".replace("{QUERY_REPORT}", COUNT_QUERY), countParams, reply -> {
                            try {
                                if (reply.failed()){
                                    throw reply.cause();
                                }

                                JsonObject count = reply.result().getRows().get(0);
                                JsonObject results = new JsonObject()
                                        .put("count", count.getInteger("count", 0))
                                        .put("items", resultsList.size())
                                        .put("results", resultsList);

                                message.reply(results);

                            } catch (Throwable t){
                                reportQueryError(message, t);
                            }
                        });
                    }else{
                        message.reply(new JsonArray(resultsList));
                    }

                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private static void addAllElements(JsonArray destination, JsonArray source) {
        for (int i = 0; i < source.size(); i++) {
            destination.add(source.getValue(i));
        }
    }

    private String getDebtParcelFilter(List<JsonObject> result, String date, Boolean onlyDebtors, Integer customerId, JsonArray params){
            String QUERY = "";

            if (customerId != null){
                QUERY += " AND c.id = ? ";
                params.add(customerId);
            }

            if (onlyDebtors){
                QUERY += " AND p.debt > 0 \n";
            }

            return QUERY;
    }

    private String getDebtBoardingPassFilter(List<JsonObject> result, String date, Boolean onlyDebtors, Integer customerId, JsonArray params){
            String QUERY = "";

            if (customerId != null){
                QUERY += " AND c.id = ? ";
                params.add(customerId);
            }

            if (onlyDebtors){
                QUERY += " AND bp.debt > 0 \n";
            }
            return QUERY;
    }

    private void getCustomerDebt(Message<JsonObject> message){
        try {
            JsonObject body = message.body();
            Integer customerId = body.getInteger(CUSTOMER_ID);
            String code = body.getString("code");
            String queryValue = code.substring(0,1);
            JsonArray param = new JsonArray().add(customerId).add(code);
            String QUERY = queryValue.equals("B") ? BOARDINGPASS_DEBT : queryValue.equals("G") ? PARCEL_DEBT : queryValue.equals("P")  ? PARCEL_PREPAID_DEBT : RENTAL_DEBT;
            if(code.substring(0,2).equals("BP")) {
                QUERY = QUERY_SELECT_PREPAID_DEBTS;
                QUERY = QUERY.concat(" AND ppt.reservation_code = ? ");
            }
            dbClient.queryWithParams(QUERY, param, reply ->{
                try{
                    if(reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> resultDetail = reply.result().getRows();
                    if (resultDetail.isEmpty()){
                        throw new Exception("Code not found");
                    }
                    JsonObject detail = resultDetail.get(0);
                    message.reply(detail);
                }catch (Throwable ex){
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }
    private void walletDetailReport(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String QUERY = QUERY_CUSTOMER_WALLET_DETAIL_REPORT;
            Integer customer_id = body.getInteger(CUSTOMER_ID);
            String init_date = body.getString(INIT_DATE);
            String end_date = body.getString(END_DATE);
            Integer limit = body.getInteger("limit", 200);
            Integer page = body.getInteger("page");
            String rowFilter = "";
            Boolean isTotals = body.getBoolean("totals", false);
            String selectPart = isTotals ? SELECT_WALLET_DETAIL_TOTALS : SELECT_WALLET_DETAIL;
            String orderLimitPart = isTotals ? "" : "ORDER BY created_at DESC";

            if(end_date == null){
                end_date = UtilsDate.sdfDataBase(new Date());
            }

            JsonArray params = new JsonArray()
                    .add(end_date)
                    .add(end_date)
                    .add(end_date)
                    .add(end_date)
                    .add(end_date)
                    .add(end_date)
                    .add(end_date)
                    .add(end_date)
                    .add(end_date);

            List<JsonArray> parcelParams = new ArrayList<>();
            if(init_date != null){
                QUERY = QUERY.replace("{RANGE_OPTIONAL}", " AND (CONVERT_TZ(pp.created_at, '+00:00', '-07:00') BETWEEN ? AND ?) ");
                JsonArray pp = new JsonArray().add(init_date).add(end_date);
                parcelParams.add(pp);
            } else {
                QUERY = QUERY.replace("{RANGE_OPTIONAL}", "");
            }

            for (JsonArray pp : parcelParams) {
                addAllElements(params, pp);
            }
            params.add(customer_id);
            for (JsonArray pp : parcelParams) {
                addAllElements(params, pp);
            }
            params.add(customer_id);
            params.add(customer_id);

            QUERY = QUERY.replace("{SELECT_DETAIL}", selectPart);
            QUERY = QUERY.replace("{ORDER_LIMIT}", orderLimitPart);
            QUERY = QUERY.replace("{ROW_FILTER}", rowFilter);

            String COUNT_QUERY = QUERY;
            JsonArray countParams = params.copy();

            if(page != null && !isTotals) {
                QUERY = QUERY.concat(" LIMIT ?, ? ");
                params.add((page - 1) * limit).add(limit);
            }

            this.dbClient.queryWithParams(QUERY, params, replyL -> {
                try {
                    if (replyL.failed()){
                        throw replyL.cause();
                    }

                    List<JsonObject> resultsList = replyL.result().getRows();

                    if(page != null && !isTotals){
                        this.dbClient.queryWithParams("select count(*) as count from ({QUERY_REPORT}) as f".replace("{QUERY_REPORT}", COUNT_QUERY), countParams, reply -> {
                            try {
                                if (reply.failed()){
                                    throw reply.cause();
                                }

                                JsonObject count = reply.result().getRows().get(0);
                                JsonObject results = new JsonObject()
                                        .put("count", count.getInteger("count", 0))
                                        .put("items", resultsList.size())
                                        .put("results", resultsList);

                                message.reply(results);

                            } catch (Throwable t){
                                reportQueryError(message, t);
                            }
                        });
                    } else {
                        message.reply(new JsonArray(resultsList));
                    }
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void getCustomerByUserId(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer userId = body.getInteger(USER_ID);
            JsonArray param = new JsonArray().add(userId);
            String QUERY = QUERY_GET_CUSTOMER_BY_USER_ID;
            dbClient.queryWithParams(QUERY, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        message.reply(new JsonObject());
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void updateCustomer(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer id = body.getInteger("id");
            String email = body.getString("email");
            String phone = body.getString("phone");
            JsonArray params = new JsonArray().add(email);
            this.dbClient.queryWithParams(QUERY_GET_TIMES_EMAIL_REPEAT, params, reply -> {
                try {

                } catch (Throwable t) {
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
               if (reply.succeeded()) {
                   List<JsonObject> resultSelect = reply.result().getRows();
                   Integer count = resultSelect.get(0).getInteger("val");
                   if (count > 2) {
                       message.reply(new JsonObject().put("max_number", true));
                   } else {
                       JsonObject service = new JsonObject().put("id", id).put("email", email).put("phone", phone);
                       //String queryUpdate = this.generateGenericUpdate("customer", service );
                       GenericQuery gen = this.generateGenericUpdate("customer", service);
                       this.dbClient.updateWithParams(gen.getQuery(), gen.getParams(), replyUpdate -> {
                           try {
                               if (replyUpdate.failed()) {
                                   throw  replyUpdate.cause();
                               } else {
                                   message.reply(new JsonObject().put("message", "Updated"));
                               }
                           } catch (Throwable e) {
                               reportQueryError(message, e);
                           }
                       });
                   }

               } else {
                   reportQueryError(message, reply.cause());
               }
            });
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void checkRFC(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            String QUERY = QUERY_CHECK_RFC_BILLING_INFO;
            JsonArray params = new JsonArray().add(body.getString("rfc")).add(body.getInteger("id")).add(body.getInteger("customer_id"));

            this.dbClient.queryWithParams(QUERY, params, reply -> {
               try {
                   if (reply.failed()) {
                       throw reply.cause();
                   }

                   List<JsonObject>  response = reply.result().getRows();

                   if(response.size() == 0) {
                       //message.reply(new JsonObject().put("exists", false));
                       message.reply(new JsonObject().put("exists", false));
                   } else {
                       message.reply(new JsonObject().put("exists", true));
                   }
                   //message.reply(response.get(0));

               } catch (Throwable ex) {
                   reportQueryError(message,ex);
               }
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void deleteBilling(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();


            GenericQuery update = this.generateGenericUpdate("customer_billing_information" ,body);
            this.dbClient.updateWithParams(update.getQuery(),update.getParams(), reply -> {
                try {
                    if (reply.failed()) {
                        reportQueryError(message, reply.cause());
                    }

                    message.reply(reply.succeeded());
                } catch (Throwable ex) {
                    reportQueryError(message, ex);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void accountStatusReport(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer customerID = body.getInteger("customerID");
            String service = body.getString("service");
            Integer cityID = body.getInteger("cityID");
            Integer branchOfficeID = body.getInteger("branchOfficeID");

            Date initDate = UtilsDate.parse_yyyy_MM_dd(body.getString("initDate"));
            Date endDate = UtilsDate.parse_yyyy_MM_dd(body.getString("endDate"));
            UtilsDate.RangeDate range = new UtilsDate.RangeDate(initDate, endDate);
            range.endIsGreaterThanNow();

            String initDateFormat = UtilsDate.format_yyyy_MM_dd(initDate);
            String endDateFormat = UtilsDate.format_yyyy_MM_dd(endDate);

            List<Future> tasks = new ArrayList<>();

            Future<ResultSet> customerDetailFuture = Future.future();
            this.dbClient.queryWithParams(QUERY_ACCOUNT_STATUS_DEBT_CUSTOMER_DETAIL, new JsonArray().add(customerID), customerDetailFuture.completer());
            tasks.add(customerDetailFuture);

            Future<ResultSet> debtHistoryFuture = Future.future();
            this.dbClient.query(getAccoutStatusHistoryByService(service, customerID, initDateFormat, endDateFormat, cityID, branchOfficeID), debtHistoryFuture.completer());
            tasks.add(debtHistoryFuture);

            Future<ResultSet> currentDebtParcelsFuture = Future.future();
            this.dbClient.queryWithParams(QUERY_SELECT_PARCEL_DEBTS, new JsonArray().add(customerID), currentDebtParcelsFuture.completer());
            tasks.add(currentDebtParcelsFuture);

            Future<ResultSet> currentDebtParcelsPrepaidFuture = Future.future();
            this.dbClient.queryWithParams(QUERY_SELECT_GUIAPP_DEBTS, new JsonArray().add(customerID), currentDebtParcelsPrepaidFuture.completer());
            tasks.add(currentDebtParcelsPrepaidFuture);

            CompositeFuture.all(tasks).setHandler(replyRes -> {
                try {
                    if (replyRes.failed()) {
                        throw replyRes.cause();
                    }

                    getTotalDebtsToDateByService(service, customerID, initDateFormat, cityID, branchOfficeID).whenComplete((resTD, errorTD) -> {
                        try {
                            if (errorTD != null) {
                                throw errorTD;
                            }

                            JsonObject debtToDate = new JsonObject()
                                    .put("created_at","1900-01-01 00:00:00")
                                    .put("detail", "DEBT_TO_DATE")
                                    .put("total_amount", resTD)
                                    .put("paid", 0.0);

                            if(replyRes.result().<ResultSet>resultAt(0).getRows().isEmpty()){
                                throw new Exception("Customer not found");
                            }

                            JsonObject resultCustomerDetail = replyRes.result().<ResultSet>resultAt(0).getRows().get(0);
                            List<JsonObject> resultDebtHistory = replyRes.result().<ResultSet>resultAt(1).getRows();
                            resultDebtHistory.add(0, debtToDate);

                            Double totalDebt = resultDebtHistory.stream().mapToDouble(value -> (value.getDouble("total_amount"))).sum();
                            Double totalPaid = resultDebtHistory.stream().mapToDouble(value -> (value.getDouble("paid"))).sum();

                            List<JsonObject> resultCurrentDebtParcels = replyRes.result().<ResultSet>resultAt(2).getRows();
                            double currentDebtP = 0.0;
                            if (!resultCurrentDebtParcels.isEmpty()) {
                                currentDebtP = resultCurrentDebtParcels.stream().mapToDouble(value -> (value.getDouble("debt"))).sum();
                                currentDebtP = (double) Math.round(currentDebtP * 100.0) / 100.0;
                            }

                            List<JsonObject> resultCurrentDebtParcelsPrepaid = replyRes.result().<ResultSet>resultAt(3).getRows();
                            double currentDebtPP = 0.0;
                            if(!resultCurrentDebtParcelsPrepaid.isEmpty()) {
                                currentDebtPP = resultCurrentDebtParcelsPrepaid.stream().mapToDouble(value -> (value.getDouble("debt"))).sum();
                                currentDebtPP = (double) Math.round(currentDebtPP * 100.0) / 100.0;
                            }

                            double currentDebt = 0.0;
                            switch (service) {
                                case "parcels":
                                    currentDebt = currentDebtP;
                                    break;
                                case "parcelsPrepaid":
                                    currentDebt = currentDebtPP;
                                    break;
                                default:
                                    currentDebt = currentDebtP + currentDebtPP;
                                    break;
                            }

                            resultCustomerDetail
                                    .put("global_total_amount", totalDebt)
                                    .put("global_total_paid", totalPaid)
                                    .put("current_debt", currentDebt);

                            message.reply(new JsonObject()
                                    .put("customer_detail", resultCustomerDetail)
                                    .put("history", resultDebtHistory));

                        } catch (Throwable t) {
                            t.printStackTrace();
                            reportQueryError(message, t);
                        }
                    });

                } catch (Throwable t) {
                    t.printStackTrace();
                    reportQueryError(message, t);
                }
            });

        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private String getAccoutStatusHistoryByService(String service, Integer customerID, String initDateFormat, String endDateFormat, Integer cityId, Integer branchOfficeID) {

        String cityParam = "";
        String branchOfficeParam = "";
        String cityParamDebtPayments = "";
        String branchOfficeParamDebtPayments = "";

        if (cityId != null) {
            cityParam = "AND city.id =" + cityId;
            cityParamDebtPayments = "AND (porigin.city_id =" + cityId + " OR pporigin.city_id =" + cityId + ")";
        }

        if (branchOfficeID != null) {
            branchOfficeParam = "AND origin.id =" + branchOfficeID;
            branchOfficeParamDebtPayments = "AND (porigin.id = " + branchOfficeID + " OR pporigin.id = " + branchOfficeID + ")";
        }

        String QUERY_PARCELS = String.format(QUERY_ACCOUNT_STATUS_DEBT_PARCELS, customerID, initDateFormat, endDateFormat, cityParam, branchOfficeParam);
        String QUERY_PARCELS_PREPAID = String.format(QUERY_ACCOUNT_STATUS_DEBT_PARCELS_PREPAID, customerID, initDateFormat, endDateFormat, cityParam, branchOfficeParam);
        String QUERY_DEBT_PAYMENTS = "";
        String QUERY = "";

        switch (service) {
            case "parcels":
                QUERY_DEBT_PAYMENTS = String.format(QUERY_ACCOUNT_STATUS_DEBT_PAYMENTS, customerID, initDateFormat, endDateFormat,
                        "AND dp.parcel_id IS NOT NULL", cityParamDebtPayments, branchOfficeParamDebtPayments);
                QUERY = String.format("(%s) UNION (%s) ORDER BY created_at;", QUERY_PARCELS, QUERY_DEBT_PAYMENTS);
                break;
            case "parcelsPrepaid":
                QUERY_DEBT_PAYMENTS = String.format(QUERY_ACCOUNT_STATUS_DEBT_PAYMENTS, customerID, initDateFormat, endDateFormat,
                        "AND dp.parcel_prepaid_id IS NOT NULL", cityParamDebtPayments, branchOfficeParamDebtPayments);
                QUERY = String.format("(%s) UNION (%s) ORDER BY created_at;", QUERY_PARCELS_PREPAID, QUERY_DEBT_PAYMENTS);
                break;
            default:
                QUERY_DEBT_PAYMENTS = String.format(QUERY_ACCOUNT_STATUS_DEBT_PAYMENTS, customerID, initDateFormat, endDateFormat,
                        "", cityParamDebtPayments, branchOfficeParamDebtPayments);
                QUERY = String.format("(%s) UNION (%s) UNION (%s) ORDER BY created_at;", QUERY_PARCELS, QUERY_PARCELS_PREPAID, QUERY_DEBT_PAYMENTS);
                break;
        }
        return QUERY;
    }

    private CompletableFuture<Double> getTotalDebtsToDateByService(String service, Integer customerID, String initDateFormat, Integer cityID, Integer branchOfficeID){
        CompletableFuture<Double> future = new CompletableFuture<>();
        try {

            String cityIDParam = cityID != null ? "AND origin.city_id = "+cityID : "";
            String branchOfficeIDParam = branchOfficeID != null ? "AND origin.id = "+branchOfficeID : "";

            List<Future> tasks = new ArrayList<>();
            String QUERY_P = String.format(QUERY_ACCOUNT_STATUS_DEBT_TO_DATE_PARCELS,
                    customerID, initDateFormat, initDateFormat, cityIDParam, branchOfficeIDParam,
                    customerID, initDateFormat, cityIDParam, branchOfficeIDParam);
            String QUERY_PP = String.format(QUERY_ACCOUNT_STATUS_DEBT_TO_DATE_PARCELS_PREPAID,
                    customerID, initDateFormat, initDateFormat, cityIDParam, branchOfficeIDParam,
                    customerID, initDateFormat, cityIDParam, branchOfficeIDParam);

            switch (service) {
                case "parcels":
                        tasks.add(this.getTotalDebtsToDate(QUERY_P));
                    break;
                case "parcelsPrepaid":
                        tasks.add(this.getTotalDebtsToDate(QUERY_PP));
                    break;
                default:
                        tasks.add(this.getTotalDebtsToDate(QUERY_P));
                        tasks.add(this.getTotalDebtsToDate(QUERY_PP));
                    break;
            }

            CompositeFuture.all(tasks).setHandler(replyRes -> {
                try {
                    if (replyRes.failed()) {
                        throw replyRes.cause();
                    }

                    Double total = 0.0;
                    for(int i = 0; i < replyRes.result().list().size(); i++){
                        total += replyRes.result().<Double>resultAt(i);
                    }

                    future.complete((double) Math.round(total * 100.0) / 100.0);

                } catch (Throwable ex) {
                    ex.printStackTrace();
                    future.completeExceptionally(ex);
                }
            });


        } catch(Throwable e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    private Future getTotalDebtsToDate(String QUERY){
        Future<Double> future = Future.future();
        dbClient.query(QUERY, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                JsonObject debtsToDateObj = reply.result().getRows().get(0);
                Double totalAmount = debtsToDateObj.getDouble("total_amount");
                Double totalPayments = debtsToDateObj.getDouble("total_payments");

                future.complete(totalAmount - totalPayments);
            } catch(Throwable e){
                e.printStackTrace();
                future.fail(e);
            }
        });
        return future;
    }

    private void getDetail(Message<JsonObject> message){
        try{
            Integer customerId = message.body().getInteger("id");
            this.dbClient.queryWithParams(QUERY_GET_CUSTOMER_DETAIL, new JsonArray().add(customerId), reply->{
                try{
                    if(reply.failed()){
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("Customer not found");
                    }
                    JsonObject detail = result.get(0);
                    this.dbClient.queryWithParams(QUERY_GET_CUSTOMER_DETAIL_PROMOS, new JsonArray().add(customerId), replyPromos -> {
                        try{
                            if(replyPromos.failed()){
                                throw replyPromos.cause();
                            }
                            List<JsonObject> resultPromos = replyPromos.result().getRows();
                            message.reply(detail.put("promos", new JsonArray(resultPromos)));
                        } catch (Throwable t){
                            reportQueryError(message, t);
                        }
                    });
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        }catch (Exception ex){
            reportQueryError(message, ex);
        }
    }

    private void updateBasicInfo(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            GenericQuery update = this.generateGenericUpdate("customer", body, true);

            this.dbClient.updateWithParams(update.getQuery(),update.getParams(), reply -> {
                try {
                    if (reply.failed()) {
                        reportQueryError(message, reply.cause());
                    }
                    message.reply(reply.succeeded());
                } catch (Throwable ex) {
                    reportQueryError(message, ex);
                }
            });
        } catch (Exception ex){
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    /*private CompletableFuture<Double> getTotalDebtsToDate(String QUERY){
        CompletableFuture<Double> future = new CompletableFuture<>();
        dbClient.query(QUERY, reply -> {
            try {
                if (reply.failed()) {
                    throw reply.cause();
                }

                JsonObject debtsToDateObj = reply.result().getRows().get(0);
                Double currentDebt = debtsToDateObj.getDouble("current_debt");
                Double totalPayments = debtsToDateObj.getDouble("total_payments");

                future.complete(currentDebt + totalPayments);
            } catch(Throwable e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }*/

    private void createPassenger(Message<JsonObject> message){
        this.startTransaction(message, con -> {
            JsonObject customer_passenger = message.body().copy();

            GenericQuery customerCreate = this.generateGenericCreateSendTableName("customer_passenger", customer_passenger);
            con.updateWithParams(customerCreate.getQuery(), customerCreate.getParams(), customerPassengerReply -> {
                try{
                    if (customerPassengerReply.succeeded()){
                        Integer customerPassengerID = customerPassengerReply.result().getKeys().getInteger(0);
                        this.commit(con, message, new JsonObject()
                                .put("new_customer_passenger", true)
                                .put("id_customer_passenger", customerPassengerID));
                    } else {
                        customerPassengerReply.cause().printStackTrace();
                        this.rollback(con, customerPassengerReply.cause(), message);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    this.rollback(con, e, message);
                }
            });
        });
    }

    private void updatePassenger(Message<JsonObject> message){
        this.startTransaction(message, con -> {
            JsonObject customer_passenger = message.body().copy();

            GenericQuery customerPassengerUpdate = this.generateGenericUpdate("customer_passenger", customer_passenger);
            con.updateWithParams(customerPassengerUpdate.getQuery(), customerPassengerUpdate.getParams(), customerPassengerReply -> {
                try{
                    if (customerPassengerReply.succeeded()){
                        Integer customerPassengerID = customer_passenger.getInteger("id");
                        //commit
                        this.commit(con, message, new JsonObject()
                                .put("new_customer", false)
                                .put("id_customer_passenger", customerPassengerID));
                    } else {
                        customerPassengerReply.cause().printStackTrace();
                        this.rollback(con, customerPassengerReply.cause(), message);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    this.rollback(con, e, message);
                }
            });
        });
    }

    private void getPassengersByCustomerId(Message<JsonObject> message){
        try{
            Integer customerId = message.body().getInteger("customer_id");
            this.dbClient.queryWithParams(QUERY_GET_PASSENGERS_BY_CUSTOMER_ID, new JsonArray().add(customerId), reply->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    message.reply(new JsonArray(result));
                } catch (Exception e){
                    reportQueryError(message,e);
                }
            });
        }catch (Exception ex){
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void createFCMToken(Message<JsonObject> message){
        this.startTransaction(message, con -> {
            JsonObject fcm_token = message.body().copy();

            GenericQuery fcmTokenCreate = this.generateGenericCreateSendTableName("fcm_token", fcm_token);
            con.updateWithParams(fcmTokenCreate.getQuery(), fcmTokenCreate.getParams(), fcmTokenReply -> {
                try{
                    if (fcmTokenReply.succeeded()){
                        Integer fcmTokenId = fcmTokenReply.result().getKeys().getInteger(0);
                        this.commit(con, message, new JsonObject()
                                .put("new_fcm_token", true)
                                .put("id_fcm_token", fcmTokenId));
                    } else {
                        fcmTokenReply.cause().printStackTrace();
                        this.rollback(con, fcmTokenReply.cause(), message);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    this.rollback(con, e, message);
                }
            });
        });
    }

    private void getFCMTokens(Message<JsonObject> message){
        try{
            Integer customerId = message.body().getInteger("customer_id");
            this.dbClient.queryWithParams(QUERY_GET_FCM_TOKENS_BY_CUSTOMER_ID, new JsonArray().add(customerId), reply->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    message.reply(new JsonArray(result));
                } catch (Exception e){
                    reportQueryError(message,e);
                }
            });
        } catch (Exception ex){
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void verifyWithPhone(Message<JsonObject> message){
        this.startTransaction(message, con -> {
            JsonObject params = new JsonObject();
            Integer userID = message.body().getInteger("user_id");
            params.put("id", userID).put("is_phone_verified", 1);

            GenericQuery verifyPhone = this.generateGenericUpdate("users", params);
            con.updateWithParams(verifyPhone.getQuery(), verifyPhone.getParams(), verifyPhoneReply -> {
                try{
                    if (verifyPhoneReply.succeeded()){
                        this.initWallet(con, userID, "").whenComplete((resultInitWallet, errorInitWallet) -> {
                            if (errorInitWallet != null){
                                errorInitWallet.printStackTrace();
                                this.rollback(con, errorInitWallet, message);
                            } else {
                                this.commit(con, message, new JsonObject()
                                        .put("new_user", false)
                                        .put("user_id", userID));
                            }
                        });
                    } else {
                        verifyPhoneReply.cause().printStackTrace();
                        this.rollback(con, verifyPhoneReply.cause(), message);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    this.rollback(con, e, message);
                }
            });
        });
    }

    private CompletableFuture<Void> initWallet(SQLConnection conn, Integer user_id, String referral_code) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        conn.queryWithParams(QUERY_GET_WALLET_ID_BY_USER_ID, new JsonArray().add(user_id), replyWexist -> {
            if(replyWexist.failed()) {
                future.completeExceptionally(replyWexist.cause());
            }
            List<JsonObject> walletsByUser = replyWexist.result().getRows();
            if(!walletsByUser.isEmpty()){
                future.completeExceptionally(new RuntimeException("El usuario ya cuenta con monedero electronico"));
            }

            String wallet_code = UtilsID.generateEwalletCode("CO", user_id);
            JsonObject wallet = new JsonObject()
                    .put("user_id", user_id)
                    .put("code", wallet_code);

            String insertWallet = this.generateGenericCreate("e_wallet", wallet);

            conn.update(insertWallet, replyInsertWallet -> {
                if (replyInsertWallet.failed()) {
                    future.completeExceptionally(replyInsertWallet.cause());
                }

                Integer walletId = replyInsertWallet.result().getKeys().getInteger(0);

                this.registerReferral(conn, walletId, referral_code).whenComplete((resultRegisterReferral, errReferral) -> {
                    if (errReferral != null) {
                        future.completeExceptionally(new RuntimeException("Error al registrar referido"));
                    } else {
                        this.createReferralDeepLink(conn, wallet_code, user_id, walletId).whenComplete((resultCreateReferralDeepLink, errReferralDeepLink) -> {
                            if (errReferralDeepLink != null) {
                                errReferralDeepLink.printStackTrace();
                            }

                            future.complete(null);
                        });
                    }
                });
            });
        });

        return future;
    }

    private CompletableFuture<JsonObject> registerReferral(SQLConnection conn, Integer walletId, String referral_code) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            if(referral_code.isEmpty()) {
                future.complete(new JsonObject());
            } else {
                conn.queryWithParams(QUERY_GET_ID_BY_WALLET_CODE, new JsonArray().add(referral_code), replyReferral -> {
                    if( replyReferral.failed()) {
                        future.completeExceptionally(replyReferral.cause());
                    }

                    List<JsonObject> referrals = replyReferral.result().getRows();
                    JsonObject referralObj = referrals.get(0);
                    Integer referral_id = referralObj.getInteger("id");

                    conn.updateWithParams(QUERY_SET_REFERRAL_ID_ON_WALLET, new JsonArray().add(referral_id).add(walletId), replyUpdateReferral -> {
                        try {
                            if (replyUpdateReferral.failed()) {
                                throw new Exception(replyUpdateReferral.cause());
                            }
                            future.complete(new JsonObject().put("updated", true));
                        } catch (Throwable t) {
                            future.completeExceptionally(t);
                        }
                    });
                });
            }
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Void> createReferralDeepLink(SQLConnection conn, String wallet_code, int user_id, int wallet_id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            JsonObject linkData = new JsonObject()
                    .put("referral_code", wallet_code)
                    .put("user_id", user_id);

            DeepLinkModel deepLink = new DeepLinkModel.Builder("referral", linkData)
                    .title("Usa mi código")
                    .description("Recibe 10% de descuento en tu primer viaje")
                    .channel("share")
                    .feature("referral")
                    .campaign("user_referral")
                    .stage("share_link")
                    .build();

            UtilsDeepLinks utilsDeepLinks = UtilsDeepLinks.getInstance();
            utilsDeepLinks.createLink(deepLink).whenComplete((resultCreateLink, errorCreateLink) -> {
                if (errorCreateLink != null){
                    future.completeExceptionally(errorCreateLink);
                } else {
                    JsonObject referralDeepLink = new JsonObject()
                            .put("e_wallet_id", wallet_id)
                            .put("link", resultCreateLink);

                    String insertReferralDeepLink =
                            this.generateGenericCreate("deep_link_referral", referralDeepLink);

                    conn.update(insertReferralDeepLink, replyInsertReferralDeepLink -> {
                        if (replyInsertReferralDeepLink.failed()) {
                            future.completeExceptionally(replyInsertReferralDeepLink.cause());
                        } else {
                            future.complete(null);
                        }
                    });
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getCustomersByCriteriaForReports(JsonObject data){
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

    private void getCustomersByAdviserId(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            JsonArray sellers = body.getJsonArray("seller_array");
            JsonObject complementQueryData = new JsonObject()
                    .put("sellers", sellers);

            this.getCustomersByCriteriaForReports(complementQueryData)
                    .whenComplete((resultCustomersIds, errorCustomersIds) -> {
                        String customersIdsString = resultCustomersIds.getString("customers_ids");
                        if(customersIdsString.isEmpty()) {
                            message.reply(new JsonArray());
                        } else {
                            String[] idsArray = customersIdsString.split(",");
                            JsonArray customerArray = new JsonArray();

                            for (String id : idsArray) {
                                JsonObject customer = new JsonObject();
                                customer.put("id", Integer.parseInt(id.trim()));
                                customerArray.add(customer);
                            }
                            message.reply(customerArray);
                        }
                    });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void findAllBillingInformationByCustomerID(Message<JsonObject> message) {
        try {
            JsonArray params = new JsonArray().add(message.body().getValue("customer_id"));
            doFindBillingInformation(QUERY_FIND_ALL_BILLING_INFORMATION_BY_CUSTOMER_ID, params, message);
        } catch (Exception e) {
            reportQueryError(message, e.getCause());
        }
    }

    private void getServicesWithDebt(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String serviceType = body.getString("service_type");
            String dateType = body.getString("filter_by_date");
            String initDate = body.getString("init_date");
            String endDate = body.getString("end_date");
            Integer customerId = body.getInteger("customer_id");
            Integer invoiceFolio = body.getInteger("invoice_folio");
            Boolean onlyInvoiced = body.getBoolean("only_invoiced");
            Integer limit = body.getInteger(LIMIT);
            Integer page = body.getInteger(PAGE);

            String QUERY = "";
            String QUERY_COUNT = "";
            switch (serviceType) {
                case "parcel":
                    QUERY = QUERY_GET_PARCELS_WITH_DEBT;
                    QUERY_COUNT = QUERY_COUNT_GET_PARCELS_WITH_DEBT.replace("{TABLE_NAME}", "parcels");

                    break;
                case "guia_pp":
                    QUERY = QUERY_GET_PARCELS_PREPAID_WITH_DEBT;
                    QUERY_COUNT = QUERY_COUNT_GET_PARCELS_WITH_DEBT.replace("{TABLE_NAME}", "parcels_prepaid");
                    break;
            }

            JsonArray params = new JsonArray();
            params.add(customerId).add(initDate).add(endDate);

            if(onlyInvoiced) {
                QUERY += " AND p.invoice_id IS NOT NULL";
                QUERY_COUNT += " AND p.invoice_id IS NOT NULL";
            }

            if(invoiceFolio != null) {
                QUERY = QUERY.concat(WHERE_BY_INVOICE_FOLIO);
                QUERY_COUNT = QUERY_COUNT.concat(WHERE_BY_INVOICE_FOLIO);
                params.add(invoiceFolio).add(invoiceFolio);
            }

            switch(dateType) {
                case "created_at":
                    QUERY = QUERY.replace("{DATE_FILTER}", "p.created_at").concat(" ORDER BY p.created_at DESC");
                    QUERY_COUNT = QUERY_COUNT.replace("{DATE_FILTER}", "p.created_at");
                    break;
                case "invoice_date":
                    QUERY = QUERY.replace("{DATE_FILTER}", "i.created_at").concat(" ORDER BY i.created_at DESC");
                    QUERY_COUNT = QUERY_COUNT.replace("{DATE_FILTER}", "i.created_at");
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
                    Double totalDebt = resultSet1.getRows().get(0).getDouble("total_debt");

                    List<JsonObject> parcelsList = reply.result().<ResultSet>resultAt(1).getRows();
                    JsonObject result = new JsonObject()
                            .put("count", count)
                            .put("total_debt", totalDebt)
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

    private void searchV2(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer customerId = body.getInteger(_CUSTOMER_ID);
            String searchTerm = body.getString("searchTerm");
            boolean includeDefaults = body.getBoolean("include_defaults", false);

            searchCustomerByName(searchTerm, 10).whenComplete((results, err) -> {
                try {
                    if (err != null) {
                        throw err;
                    }
                    if (!includeDefaults) {
                        message.reply(new JsonObject()
                                .put("results", new JsonArray(results))
                                .put("defaults", new JsonArray()));
                        return;
                    }
                    getFrequentCustomers(customerId).whenComplete((defaults, errDefaults) -> {
                        try {
                            if (errDefaults != null) {
                                throw errDefaults;
                            }
                            message.reply(new JsonObject()
                                    .put("results", new JsonArray(results))
                                    .put("defaults", new JsonArray(defaults)));
                        } catch (Throwable t) {
                            reportQueryError(message, t);
                        }
                    });
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Exception e) {
            reportQueryError(message, e);
        }
    }

    private CompletableFuture<List<JsonObject>> getFrequentCustomers(Integer customerId) {
        CompletableFuture<List<JsonObject>> future = new CompletableFuture<>();
        if (Objects.isNull(customerId)) {
            customerId = 0;
        }
        this.dbClient.queryWithParams(GET_FREQUENT_CUSOTMERS_BY_CUSTOMER_ID, new JsonArray().add(customerId), reply -> {
           try {
               if (reply.failed()) {
                   throw reply.cause();
               }
               future.complete(reply.result().getRows());
           } catch (Throwable t) {
               future.completeExceptionally(t);
           }
        });
        return future;
    }

    private void getCustomersCatalogueList(Message<JsonObject> message) {
        JsonObject body = message.body();
        int limit = body.getInteger("limit", 10);
        int page = body.getInteger("page", 1);
        String search = body.getString("search", "").trim();

        Integer userId = body.getInteger(USER_ID);
        List<JsonObject> permissions = body.getJsonArray("permissions").getList();
        Boolean superUser = body.getBoolean("superuser", false);
        Boolean onlyAssigned = permissions.stream().anyMatch(p -> p.getString(_NAME).equals("#onlyassigned"));

        String whereClause = "";
        JsonArray params = new JsonArray();

        if (!search.isEmpty()) {
            whereClause += " AND (\n" +
                    "  c.id = ? OR\n" +
                    "  CONCAT(c.first_name, ' ', c.last_name) LIKE CONCAT('%', ?, '%') OR\n" +
                    "  cbi.name LIKE CONCAT('%', ?, '%') OR\n" +
                    "  cbi.rfc LIKE CONCAT('%', ?, '%') OR\n" +
                    "  us.email LIKE CONCAT('%', ?, '%')\n" +
                    ")";
            for (int i = 0; i < 5; i++) {
                params.add(search);
            }
        }
        if(!superUser && onlyAssigned) {
            whereClause += " AND c.user_seller_id = ?\n";
            params.add(userId);
        }

        String dataSql = GET_CUSTOMER_CATALOGUE
                .replace("{WHERE_QUERY}", whereClause)
                + " LIMIT ? OFFSET ?";
        params.add(limit).add((page - 1) * limit);

        String finalWhereClause = whereClause;
        dbClient.queryWithParams(dataSql, params, ar -> {
            if (ar.failed()) {
                reportQueryError(message, ar.cause());
                return;
            }
            List<JsonObject> rows = ar.result().getRows();

            String countSql = "SELECT COUNT(*) AS count "
                    + GET_CUSTOMER_CATALOGUE
                    .substring(GET_CUSTOMER_CATALOGUE.indexOf("FROM"))
                    .replace("{WHERE_QUERY}", finalWhereClause)
                    .replaceAll("(?i)ORDER BY\\s+business_name", "");

            JsonArray countParams = params.copy();
            countParams.remove(countParams.size() - 1);
            countParams.remove(countParams.size() - 1);

            dbClient.queryWithParams(countSql, countParams, cr -> {
                if (cr.failed()) {
                    reportQueryError(message, cr.cause());
                    return;
                }
                int total = cr.result().getRows().get(0).getInteger("count", 0);

                JsonObject response = new JsonObject()
                        .put("count", total)
                        .put("items", limit)
                        .put("results", rows);
                message.reply(response);
            });
        });
    }

    private void isAssigned(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer userId = body.getInteger(USER_ID);
            Integer customerId = body.getInteger(_CUSTOMER_ID);

            this.dbClient.queryWithParams(QUERY_CHECK_CUSTOMER_IS_ASSIGNED_TO_SELLER, new JsonArray().add(customerId).add(userId), reply -> {
                try {
                    if(reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> results = reply.result().getRows();
                    if(results.isEmpty()) {
                        throw new Exception("Info not found");
                    }

                    JsonObject result = results.get(0);
                    Boolean isAssigned = result.getInteger("is_assigned") > 0;
                    message.reply(new JsonObject().put("is_assigned", isAssigned));
                } catch (Throwable t) {
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private static final String PATH_CUSTOMER="/api/clienteproveedor/post";
    private static final String QUERY_REPORT_CUSTOMERS = "SELECT * FROM customer WHERE status != 3;";
    private static final String QUERY_REPORT_CUSTOMERS_BILLING_INFO = "SELECT * FROM customer_billing_information WHERE status != 3;";
    private static final String QUERY_REPORT_CUSTOMERS_CASE_FILES = "SELECT * FROM customer_casefile WHERE status != 3;";
    private static final String QUERY_VALIDATION_CUSTOMER_EMAIL = "SELECT * FROM customer WHERE email = ? ORDER BY id DESC LIMIT 1";
    private static final String QUERY_VALIDATION_USER_EMAIL = "SELECT * FROM users WHERE email = ? ORDER BY id DESC LIMIT 1";
    private static final String QUERY_TOKEN_EXISTS = "SELECT token FROM customer WHERE token = ? LIMIT 1;";
    private static final String QUERY_GET_VERIFICATION = "SELECT id, user_id, is_verified FROM customer WHERE token = ?;";
    private static final String QUERY_UPDATE_VERIFIED_STATUS = "UPDATE customer SET is_verified = true WHERE id = ?;";
    private static final String QUERY_UPDATE_CUSTOMER_USER_ID = "UPDATE customer SET user_id = ?, token = ? WHERE id = ?;";
    private static final String QUERY_FIND_BILLING_INFORMATION_BY_CUSTOMER_ID = "SELECT cbi.*, " +
            "c_UsoCFDI.c_UsoCFDI, c_UsoCFDI.description AS c_UsoCFDI_description, " +
            "c_RegimenFiscal.c_RegimenFiscal, c_RegimenFiscal.description AS c_RegimenFiscal_description, " +
            "country.name AS country_name, state.name AS state_name, " +
            "street.name AS street_name," +
            "county.name AS county_name, city.name AS city_name " +
            "FROM customer_billing_information AS cbi " +
            "INNER JOIN customer_customer_billing_info AS ccbi ON ccbi.customer_billing_information_id=cbi.id " +
            "LEFT JOIN c_UsoCFDI ON cbi.c_UsoCFDI_id=c_UsoCFDI.id " +
            "LEFT JOIN c_RegimenFiscal ON cbi.c_RegimenFiscal_id=c_RegimenFiscal.id " +
            "LEFT JOIN country ON cbi.country_id=country.id " +
            "LEFT JOIN state ON cbi.state_id=state.id " +
            "LEFT JOIN county ON cbi.county_id=county.id " +
            "LEFT JOIN city ON cbi.city_id=city.id " +
            "LEFT JOIN street ON cbi.street_id = street.id  " +
            "WHERE cbi.status != 3 AND ccbi.customer_id = ?;";

    private static final String QUERY_FIND_BILLING_INFORMATION = "SELECT cbi.*, " +
            "country.name AS country_name, state.name AS state_name, " +
            "street.name AS street_name," +
            "county.name AS county_name, city.name AS city_name " +
            "FROM customer_billing_information AS cbi " +
            "LEFT JOIN country ON cbi.country_id=country.id " +
            "LEFT JOIN state ON cbi.state_id=state.id " +
            "LEFT JOIN county ON cbi.county_id=county.id " +
            "LEFT JOIN city ON cbi.city_id=city.id " +
            "LEFT JOIN street ON cbi.street_id = street.id  " +
            "WHERE cbi.status != 3 AND ";
    private static final String QUERY_SELECT_PARCEL_DEBTS = "SELECT \n" +
            "p.id,\n" +
            "p.debt,\n" +
            "p.waybill,\n" +
            "p.parcel_tracking_code code,\n" +
            "i.document_id\n" +
            "FROM customer c\n" +
            "INNER JOIN parcels p ON p.customer_id = c.id \n" +
            "LEFT JOIN invoice i on i.id = p.invoice_id\n" +
            "WHERE c.id = ?\n" +
            "AND p.debt > 0\n" +
            "AND p.parcel_status NOT IN (4, 6);";

    private static final String QUERY_SELECT_PREPAID_DEBTS = "SELECT \n" +
            "ppt.id," +
            "ppt.debt," +
            "'' waybill," +
            "ppt.reservation_code code " +
            "FROM customer c " +
            "INNER JOIN prepaid_package_travel ppt ON ppt.customer_id = c.id " +
            "WHERE c.id = ? " +
            "AND ppt.debt > 0";
    private static final String QUERY_SELECT_BOARDING_PASS_DEBTS = "SELECT\n" +
            "bp.id,\n" +
            "bp.debt,\n" +
            "'' waybill,\n" +
            "bp.reservation_code code\n" +
            "FROM customer c\n" +
            "INNER JOIN boarding_pass bp ON bp.customer_id = c.id\n" +
            "WHERE c.id = ?\n" +
            "AND boardingpass_status != 0\n" +
            "AND bp.debt > 0;";
    private static final String QUERY_SEARCH_ADVANCED = "SELECT \n" +
            "    id,\n" +
            "    first_name,\n" +
            "    last_name,\n" +
            "    gender,\n" +
            "    phone,\n" +
            "    email,\n" +
            "    birthday,\n" +
            "    status,\n" +
            "    parcel_type,\n" +
            "    invoice_email\n" +
            "FROM customer\n" +
            "WHERE status != 3 \n";
    private static final String SEARCH_TERM_FILTER = "AND CONCAT_WS(' ', first_name, last_name, phone, email) LIKE ? \n";
    private static final String SEARCH_ID_FILTER = "AND id = ? \n";
    private static final String SEARCH_ORDER_BY = "ORDER BY first_name, last_name\n";

    private static final String QUERY_GET_CREDIT_BY_PARCELID = "SELECT \n" +
            "c.id,\n" +
            "c.has_credit,\n" +
            "COALESCE(c.credit_limit, 0) credit_limit,\n" +
            "COALESCE(c.credit_time_limit, 0) credit_time_limit,\n" +
            "COALESCE(c.services_apply_credit, '') services_apply_credit,\n" +
            "COALESCE(c.credit_available, credit_limit) available_credit\n" +
            "FROM customer c\n" +
            "INNER JOIN parcels p ON p.customer_id = c.id\n" +
            "WHERE p.id = ?\n" +
            "group by c.id;";

    private static final String QUERY_BILLING_INFORMATION = "SELECT cbi.id, cbi.customer_id, cbi.name, cbi.rfc, \n" +
            "cbi.zip_code, cbi.no_ext, cbi.no_int,\n" +
            "street.name AS street_name, county.name AS county_name,\n" +
            "suburb.name AS suburb_name, city.name AS city_name,\n" +
            "state.name AS state_name, country.name AS country_name,\n" +
            "cbi.contpaq_status, cbi.contpaq_parcel_status\n" +
            "FROM customer_billing_information AS cbi \n" +
            "LEFT JOIN county ON county.id = cbi.county_id\n" +
            "LEFT JOIN suburb ON suburb.id = cbi.suburb_id\n" +
            "LEFT JOIN city ON city.id = cbi.city_id\n" +
            "LEFT JOIN state ON state.id = cbi.state_id\n" +
            "LEFT JOIN country ON country.id = cbi.country_id\n" +
            "LEFT JOIN street ON street.id = cbi.state_id\n" +
            "WHERE contpaq_status = 'pending' OR contpaq_parcel_status = 'pending';";

        /*private static final String QUERY_GET_CUSTOMER_CREDIT_DATA = "SELECT \n" +
                "c.id,\n" +
                "c.has_credit,\n" +
                "COALESCE(c.services_apply_credit, '') services_apply_credit,\n" +
                "COALESCE(c.credit_limit, 0) credit_limit,\n" +
                "COALESCE(c.credit_time_limit, 0) credit_time_limit,\n" +
                "c.credit_balance, \n" +
                "COALESCE(c.credit_available, credit_limit) available_credit,\n" +
                "COALESCE(COALESCE(SUM(p.debt), 0) + COALESCE((SELECT SUM(debt) FROM parcels_prepaid where customer_id = ? AND parcel_status != 4),0) , 0) parcel_debt,\n" +
                "COALESCE((SELECT SUM(debt) FROM boarding_pass where customer_id = ?), 0) boarding_pass_debt,\n" +
                "COALESCE(COALESCE(SUM(p.debt), 0) + COALESCE((SELECT SUM(debt) FROM parcels_prepaid where customer_id = ? AND parcel_status != 4),0) + COALESCE((SELECT SUM(debt) FROM boarding_pass where customer_id = ? AND boardingpass_status != 0),0), 0) total_debt\n" +
                "FROM customer c\n" +
                "LEFT JOIN parcels p ON p.customer_id = c.id\n" +
                "WHERE c.id = ?\n" +
                "AND p.parcel_status != 4 \n" +
                "AND p.parcel_status != 6;";*/

    private static final String QUERY_GET_CUSTOMER_CREDIT_DATA = "SELECT \n" +
            "c.id,\n" +
            "c.has_credit,\n" +
            "COALESCE(c.services_apply_credit, '') services_apply_credit,\n" +
            "COALESCE(c.credit_limit, 0) credit_limit,\n" +
            "COALESCE(c.credit_time_limit, 0) credit_time_limit,\n" +
            "c.credit_balance, \n" +
            "COALESCE(c.credit_available, credit_limit) available_credit,\n" +
            "COALESCE(COALESCE(SUM(p.debt), 0), 0) parcel_debt,\n" +
            "COALESCE((SELECT SUM(debt) FROM parcels_prepaid where customer_id = ? AND parcel_status != 4),0) guiapp_debt,\n" +
            "COALESCE((SELECT SUM(debt) FROM boarding_pass where customer_id = ?), 0) boarding_pass_debt,\n" +
            "COALESCE((SELECT SUM(debt) FROM prepaid_package_travel WHERE customer_id = ?), 0) prepaid_debt,\n "+
            "COALESCE(COALESCE(SUM(p.debt), 0) + COALESCE((SELECT SUM(debt) FROM parcels_prepaid where customer_id = ? AND parcel_status != 4),0) + " +
            "COALESCE((SELECT SUM(debt) FROM boarding_pass where customer_id = ? AND boardingpass_status != 0),0), 0) + " +
            "COALESCE((SELECT SUM(debt) FROM prepaid_package_travel WHERE customer_id = ?), 0) total_debt\n" +
            "FROM customer c\n" +
            "LEFT JOIN parcels p ON p.customer_id = c.id\n" +
            "   AND p.parcel_status != 4 AND p.parcel_status != 6\n" +
            "WHERE c.id = ?";
    private static final String CUSTOMER_WALLET_REPORT = "WITH date_ranges AS (\n" +
            "    SELECT \n" +
            "        ? AS ref_date,\n" +
            "        DATE_SUB(?, INTERVAL 7 DAY) AS start_0_7,\n" +
            "        ? AS end_0_7,\n" +
            "        DATE_SUB(?, INTERVAL 14 DAY) AS start_8_14,\n" +
            "        DATE_SUB(?, INTERVAL 8 DAY) AS end_8_14,\n" +
            "        DATE_SUB(?, INTERVAL 21 DAY) AS start_15_21,\n" +
            "        DATE_SUB(?, INTERVAL 15 DAY) AS end_15_21,\n" +
            "        DATE_SUB(?, INTERVAL 30 DAY) AS start_22_30,\n" +
            "        DATE_SUB(?, INTERVAL 22 DAY) AS end_22_30\n" +
            "),\n" +
            "parcel_sums AS (\n" +
            "    SELECT \n" +
            "        pp.customer_id,\n" +
            "        SUM(CASE \n" +
            "                WHEN pp.payment_condition = 'credit' {CREDIT_EXTRA_CONDITIONS} AND DATEDIFF(dr.ref_date, pp.created_at) <= c.credit_time_limit THEN pp.debt \n" +
            "                ELSE 0 \n" +
            "            END) AS active_amount,\n" +
            "        SUM(CASE \n" +
            "                WHEN pp.payment_condition = 'credit' {CREDIT_EXTRA_CONDITIONS} AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 1 AND c.credit_time_limit + 7 THEN pp.debt \n" +
            "                ELSE 0 \n" +
            "            END) AS days_0_7,\n" +
            "        SUM(CASE \n" +
            "                WHEN pp.payment_condition = 'credit' {CREDIT_EXTRA_CONDITIONS} AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 8 AND c.credit_time_limit + 14 THEN pp.debt \n" +
            "                ELSE 0 \n" +
            "            END) AS days_8_14,\n" +
            "        SUM(CASE \n" +
            "                WHEN pp.payment_condition = 'credit' {CREDIT_EXTRA_CONDITIONS} AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 15 AND c.credit_time_limit + 21 THEN pp.debt \n" +
            "                ELSE 0 \n" +
            "            END) AS days_15_21,\n" +
            "        SUM(CASE \n" +
            "                WHEN pp.payment_condition = 'credit' {CREDIT_EXTRA_CONDITIONS} AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 22 AND c.credit_time_limit + 30 THEN pp.debt \n" +
            "                ELSE 0 \n" +
            "            END) AS days_22_30,\n" +
            "        SUM(CASE \n" +
            "                WHEN pp.payment_condition = 'credit' {CREDIT_EXTRA_CONDITIONS} AND DATEDIFF(dr.ref_date, pp.created_at) > c.credit_time_limit + 30 THEN pp.debt \n" +
            "                ELSE 0 \n" +
            "            END) AS previous_debt,\n" +
            "        COALESCE(SUM(pp.debt), 0) AS total_debt,\n" +
            "        COALESCE(SUM(dp.amount), 0) AS debt_payment\n" +
            "    FROM parcels pp\n" +
            "    LEFT JOIN customer c ON c.id = pp.customer_id\n" +
            "    LEFT JOIN debt_payment dp ON dp.parcel_id = pp.id\n" +
            "    CROSS JOIN date_ranges dr\n" +
            "    WHERE\n" +
            "    pp.parcel_status <> 4\n" +
            "    {RANGE_OPTIONAL}\n" +
            "    {CUSTOMER_PARCEL_WHERE}\n" +
            "    GROUP BY pp.customer_id\n" +
            "),\n" +
            "parcel_prepaid_sums AS (\n" +
            "    SELECT \n" +
            "        pp.customer_id,\n" +
            "        SUM(CASE \n" +
            "                WHEN pp.payment_condition = 'credit' {CREDIT_EXTRA_CONDITIONS} AND DATEDIFF(dr.ref_date, pp.created_at) <= c.credit_time_limit THEN pp.debt \n" +
            "                ELSE 0 \n" +
            "            END) AS active_amount,\n" +
            "        SUM(CASE \n" +
            "                WHEN pp.payment_condition = 'credit' {CREDIT_EXTRA_CONDITIONS} AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 1 AND c.credit_time_limit + 7 THEN pp.debt \n" +
            "                ELSE 0 \n" +
            "            END) AS days_0_7,\n" +
            "        SUM(CASE \n" +
            "                WHEN pp.payment_condition = 'credit' {CREDIT_EXTRA_CONDITIONS} AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 8 AND c.credit_time_limit + 14 THEN pp.debt \n" +
            "                ELSE 0 \n" +
            "            END) AS days_8_14,\n" +
            "        SUM(CASE \n" +
            "                WHEN pp.payment_condition = 'credit' {CREDIT_EXTRA_CONDITIONS} AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 15 AND c.credit_time_limit + 21 THEN pp.debt \n" +
            "                ELSE 0 \n" +
            "            END) AS days_15_21,\n" +
            "        SUM(CASE \n" +
            "                WHEN pp.payment_condition = 'credit' {CREDIT_EXTRA_CONDITIONS} AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 22 AND c.credit_time_limit + 30 THEN pp.debt \n" +
            "                ELSE 0 \n" +
            "            END) AS days_22_30,\n" +
            "        SUM(CASE \n" +
            "                WHEN pp.payment_condition = 'credit' {CREDIT_EXTRA_CONDITIONS} AND DATEDIFF(dr.ref_date, pp.created_at) > c.credit_time_limit + 30 THEN pp.debt \n" +
            "                ELSE 0 \n" +
            "            END) AS previous_debt,\n" +
            "        COALESCE(SUM(pp.debt), 0) AS total_debt,\n" +
            "        COALESCE(SUM(dp.amount), 0) AS debt_payment\n" +
            "    FROM parcels_prepaid pp\n" +
            "    LEFT JOIN customer c ON c.id = pp.customer_id\n" +
            "    LEFT JOIN debt_payment dp ON dp.parcel_prepaid_id = pp.id\n" +
            "    CROSS JOIN date_ranges dr\n" +
            "    WHERE\n" +
            "    pp.parcel_status <> 4\n" +
            "    {RANGE_OPTIONAL}\n" +
            "    {CUSTOMER_PARCEL_WHERE}\n" +
            "    GROUP BY pp.customer_id\n" +
            "),\n" +
            "combined_sums AS (\n" +
            "    SELECT \n" +
            "        customer_id,\n" +
            "        active_amount,\n" +
            "        days_0_7,\n" +
            "        days_8_14,\n" +
            "        days_15_21,\n" +
            "        days_22_30,\n" +
            "        previous_debt,\n" +
            "        total_debt,\n" +
            "        debt_payment\n" +
            "    FROM parcel_sums\n" +
            "    UNION ALL\n" +
            "    SELECT \n" +
            "        customer_id,\n" +
            "        active_amount,\n" +
            "        days_0_7,\n" +
            "        days_8_14,\n" +
            "        days_15_21,\n" +
            "        days_22_30,\n" +
            "        previous_debt,\n" +
            "        total_debt,\n" +
            "        debt_payment\n" +
            "    FROM parcel_prepaid_sums\n" +
            "),\n" +
            "final_sums AS (\n" +
            "    SELECT \n" +
            "        customer_id,\n" +
            "        SUM(active_amount) AS active_amount,\n" +
            "        SUM(days_0_7) AS days_0_7,\n" +
            "        SUM(days_8_14) AS days_8_14,\n" +
            "        SUM(days_15_21) AS days_15_21,\n" +
            "        SUM(days_22_30) AS days_22_30,\n" +
            "        SUM(previous_debt) AS previous_debt,\n" +
            "        SUM(total_debt) AS total_debt,\n" +
            "        SUM(debt_payment) AS debt_payment\n" +
            "    FROM combined_sums\n" +
            "    WHERE customer_id IS NOT NULL\n" +
            "    GROUP BY customer_id\n" +
            "),\n" +
            "customer_credits AS (\n" +
            "    SELECT \n" +
            "        c.id AS customer_id,\n" +
            "        COALESCE(c.credit_limit, 0) AS credit_limit,\n" +
            "        COALESCE(c.credit_available, 0) AS credit_available,\n" +
            "        c.credit_time_limit AS credit_time_limit\n" +
            "    FROM customer c\n" +
            "    WHERE 1 = 1\n" +
            "{CUSTOMER_WHERE}\n" +
            ")\n" +
            "SELECT \n" +
            "    cs.customer_id,\n" +
            "    cs.customer_name,\n" +
            "    COALESCE(ps.active_amount, 0) AS active_amount,\n" +
            "    COALESCE(ps.days_0_7, 0) AS days_0_7,\n" +
            "    COALESCE(ps.days_8_14, 0) AS days_8_14,\n" +
            "    COALESCE(ps.days_15_21, 0) AS days_15_21,\n" +
            "    COALESCE(ps.days_22_30, 0) AS days_22_30,\n" +
            "    COALESCE(ps.previous_debt, 0) AS previous_debt,\n" +
            "    COALESCE(ps.total_debt, 0) AS total_debt,\n" +
            "    COALESCE(ps.debt_payment, 0) AS debt_payment,\n" +
            "    cc.credit_limit,\n" +
            "    cc.credit_available,\n" +
            "    cc.credit_time_limit\n" +
            "FROM final_sums ps\n" +
            "LEFT JOIN (\n" +
            "    SELECT \n" +
            "        c.id AS customer_id,\n" +
            "        CONCAT(c.first_name, ' ', c.last_name) AS customer_name\n" +
            "    FROM customer c\n" +
            "    WHERE 1 = 1\n" +
            "{CUSTOMER_WHERE}\n" +
            ") cs ON ps.customer_id = cs.customer_id\n" +
            "LEFT JOIN customer_credits cc ON ps.customer_id = cc.customer_id\n" +
            "WHERE \n" +
            "{ROW_FILTER} \n" +
            "ORDER BY cs.customer_name\n";

    private static final String QUERY_CUSTOMER_WALLET_DETAIL_REPORT = "WITH date_ranges AS (\n" +
            "    SELECT \n" +
            "        ? AS ref_date,\n" +
            "        DATE_SUB(?, INTERVAL 7 DAY) AS start_0_7,\n" +
            "        ? AS end_0_7,\n" +
            "        DATE_SUB(?, INTERVAL 14 DAY) AS start_8_14,\n" +
            "        DATE_SUB(?, INTERVAL 8 DAY) AS end_8_14,\n" +
            "        DATE_SUB(?, INTERVAL 21 DAY) AS start_15_21,\n" +
            "        DATE_SUB(?, INTERVAL 15 DAY) AS end_15_21,\n" +
            "        DATE_SUB(?, INTERVAL 30 DAY) AS start_22_30,\n" +
            "        DATE_SUB(?, INTERVAL 22 DAY) AS end_22_30\n" +
            "),\n" +
            "pagos_credit AS (\n" +
            "    SELECT \n" +
            "        dp.parcel_id,\n" +
            "        SUM(dp.amount) AS paid_amount,\n" +
            "        MAX(CONVERT_TZ(dp.created_at, '+00:00', '-07:00')) AS payment_date\n" +
            "    FROM debt_payment AS dp\n" +
            "    GROUP BY dp.parcel_id\n" +
            "),\n" +
            "pagos_credit_prepaid AS (\n" +
            "    SELECT \n" +
            "        dp.parcel_prepaid_id,\n" +
            "        SUM(dp.amount) AS paid_amount,\n" +
            "        MAX(CONVERT_TZ(dp.created_at, '+00:00', '-07:00')) AS payment_date\n" +
            "    FROM debt_payment AS dp\n" +
            "    GROUP BY dp.parcel_prepaid_id\n" +
            "),\n" +
            "parcel_sums AS (\n" +
            "    SELECT \n" +
            "        pp.customer_id,\n" +
            "        pp.id AS id,\n" +
            "        pp.created_at,\n" +
            "        DATE_ADD(pp.created_at, INTERVAL c.credit_time_limit DAY) AS expire_date,\n" +
            "        pp.parcel_tracking_code AS service_code,\n" +
            "        pp.total_amount,\n" +
            "        pp.debt,\n" +
            "        pc.paid_amount,\n" +
            "        pc.payment_date,\n" +
            "        CASE \n" +
            "            WHEN pp.payment_condition = 'credit' AND DATEDIFF(dr.ref_date, pp.created_at) <= c.credit_time_limit THEN pp.debt \n" +
            "            ELSE 0 \n" +
            "        END AS active_amount,\n" +
            "        CASE \n" +
            "            WHEN pp.payment_condition = 'credit' AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 1 AND c.credit_time_limit + 7 THEN pp.debt \n" +
            "            ELSE 0 \n" +
            "        END AS days_0_7,\n" +
            "        CASE \n" +
            "            WHEN pp.payment_condition = 'credit' AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 8 AND c.credit_time_limit + 14 THEN pp.debt \n" +
            "            ELSE 0 \n" +
            "        END AS days_8_14,\n" +
            "        CASE \n" +
            "            WHEN pp.payment_condition = 'credit' AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 15 AND c.credit_time_limit + 21 THEN pp.debt \n" +
            "            ELSE 0 \n" +
            "        END AS days_15_21,\n" +
            "        CASE \n" +
            "            WHEN pp.payment_condition = 'credit' AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 22 AND c.credit_time_limit + 30 THEN pp.debt \n" +
            "            ELSE 0 \n" +
            "        END AS days_22_30,\n" +
            "        CASE \n" +
            "            WHEN pp.payment_condition = 'credit' AND DATEDIFF(dr.ref_date, pp.created_at) > c.credit_time_limit + 30 THEN pp.debt \n" +
            "            ELSE 0 \n" +
            "        END AS previous_debt,\n" +
            "        CASE\n" +
            "            WHEN pp.debt > 0 THEN DATEDIFF(dr.ref_date, DATE_ADD(pp.created_at, INTERVAL c.credit_time_limit DAY))\n" +
            "            ELSE ''\n" +
            "        END AS expiration_days\n" +
            "    FROM parcels pp\n" +
            "    LEFT JOIN customer c ON c.id = pp.customer_id\n" +
            "    LEFT JOIN pagos_credit pc ON pc.parcel_id = pp.id\n" +
            "    CROSS JOIN date_ranges dr\n" +
            "    WHERE\n" +
            "        pp.parcel_status <> 4\n" +
            "{RANGE_OPTIONAL}\n" +
            "        AND pp.customer_id = ?" +
            "),\n" +
            "parcel_prepaid_sums AS (\n" +
            "    SELECT \n" +
            "        pp.customer_id,\n" +
            "        pp.id AS id,\n" +
            "        pp.created_at,\n" +
            "        DATE_ADD(pp.created_at, INTERVAL c.credit_time_limit DAY) AS expire_date,\n" +
            "        pp.tracking_code AS service_code,\n" +
            "        pp.total_amount,\n" +
            "        pp.debt,\n" +
            "        pcp.paid_amount,\n" +
            "        pcp.payment_date,\n" +
            "        CASE \n" +
            "            WHEN pp.payment_condition = 'credit' AND DATEDIFF(dr.ref_date, pp.created_at) <= c.credit_time_limit THEN pp.debt \n" +
            "            ELSE 0 \n" +
            "        END AS active_amount,\n" +
            "        CASE \n" +
            "            WHEN pp.payment_condition = 'credit' AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 1 AND c.credit_time_limit + 7 THEN pp.debt \n" +
            "            ELSE 0 \n" +
            "        END AS days_0_7,\n" +
            "        CASE \n" +
            "            WHEN pp.payment_condition = 'credit' AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 8 AND c.credit_time_limit + 14 THEN pp.debt \n" +
            "            ELSE 0 \n" +
            "        END AS days_8_14,\n" +
            "        CASE \n" +
            "            WHEN pp.payment_condition = 'credit' AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 15 AND c.credit_time_limit + 21 THEN pp.debt \n" +
            "            ELSE 0 \n" +
            "        END AS days_15_21,\n" +
            "        CASE \n" +
            "            WHEN pp.payment_condition = 'credit' AND DATEDIFF(dr.ref_date, pp.created_at) BETWEEN c.credit_time_limit + 22 AND c.credit_time_limit + 30 THEN pp.debt \n" +
            "            ELSE 0 \n" +
            "        END AS days_22_30,\n" +
            "        CASE \n" +
            "            WHEN pp.payment_condition = 'credit' AND DATEDIFF(dr.ref_date, pp.created_at) > c.credit_time_limit + 30 THEN pp.debt \n" +
            "            ELSE 0 \n" +
            "        END AS previous_debt,\n" +
            "        CASE\n" +
            "            WHEN pp.debt > 0 THEN DATEDIFF(dr.ref_date, DATE_ADD(pp.created_at, INTERVAL c.credit_time_limit DAY))\n" +
            "            ELSE NULL\n" +
            "        END AS expiration_days\n" +
            "    FROM parcels_prepaid pp\n" +
            "    LEFT JOIN customer c ON c.id = pp.customer_id\n" +
            "    LEFT JOIN pagos_credit_prepaid pcp ON pcp.parcel_prepaid_id = pp.id\n" +
            "    CROSS JOIN date_ranges dr\n" +
            "    WHERE\n" +
            "        pp.parcel_status <> 4\n" +
            "{RANGE_OPTIONAL}\n" +
            "        AND pp.customer_id = ?\n" +
            "),\n" +
            "customer_credits AS (\n" +
            "    SELECT \n" +
            "        c.id AS customer_id,\n" +
            "        COALESCE(c.credit_limit, 0) AS credit_limit,\n" +
            "        COALESCE(c.credit_available, 0) AS credit_available,\n" +
            "        c.credit_time_limit AS credit_time_limit\n" +
            "    FROM customer c\n" +
            "    WHERE c.id = ?\n" +
            "),\n" +
            "combined_sums AS (\n" +
            "    SELECT \n" +
            "        customer_id,\n" +
            "        id,\n" +
            "        created_at,\n" +
            "        expire_date,\n" +
            "        service_code,\n" +
            "        total_amount,\n" +
            "        debt,\n" +
            "        paid_amount,\n" +
            "        payment_date,\n" +
            "        active_amount,\n" +
            "        days_0_7,\n" +
            "        days_8_14,\n" +
            "        days_15_21,\n" +
            "        days_22_30,\n" +
            "        previous_debt,\n" +
            "        expiration_days\n" +
            "    FROM parcel_sums\n" +
            "    UNION ALL\n" +
            "    SELECT \n" +
            "        customer_id,\n" +
            "        id,\n" +
            "        created_at,\n" +
            "        expire_date,\n" +
            "        service_code,\n" +
            "        total_amount,\n" +
            "        debt,\n" +
            "        paid_amount,\n" +
            "        payment_date,\n" +
            "        active_amount,\n" +
            "        days_0_7,\n" +
            "        days_8_14,\n" +
            "        days_15_21,\n" +
            "        days_22_30,\n" +
            "        previous_debt,\n" +
            "        expiration_days\n" +
            "    FROM parcel_prepaid_sums\n" +
            ")\n" +
            "{SELECT_DETAIL}\n" +
            "{ORDER_LIMIT}";

    private static final String SELECT_WALLET_DETAIL = "SELECT \n" +
            "    customer_id,\n" +
            "    id,\n" +
            "    created_at,\n" +
            "    expire_date,\n" +
            "    service_code,\n" +
            "    total_amount,\n" +
            "    debt,\n" +
            "    paid_amount,\n" +
            "    payment_date,\n" +
            "    active_amount,\n" +
            "    days_0_7,\n" +
            "    days_8_14,\n" +
            "    days_15_21,\n" +
            "    days_22_30,\n" +
            "    previous_debt,\n" +
            "    expiration_days\n" +
            "FROM combined_sums";

    private static final String SELECT_WALLET_DETAIL_TOTALS = "SELECT \n" +
            "    cs.customer_id,\n" +
            "    SUM(cs.total_amount) AS total_amount,\n" +
            "    SUM(cs.debt) AS total_debt,\n" +
            "    SUM(cs.paid_amount) AS total_paid,\n" +
            "    SUM(cs.active_amount) AS total_active_amount,\n" +
            "    SUM(cs.days_0_7) AS total_days_0_7,\n" +
            "    SUM(cs.days_8_14) AS total_days_8_14,\n" +
            "    SUM(cs.days_15_21) AS total_days_15_21,\n" +
            "    SUM(cs.days_22_30) AS total_days_22_30,\n" +
            "    SUM(cs.previous_debt) AS total_previous_debt,\n" +
            "    cc.credit_limit,\n" +
            "    cc.credit_available,\n" +
            "    cc.credit_time_limit,\n" +
            "    CONCAT(c.first_name, ' ', c.last_name) AS customer_name,\n" +
            "    c.email as customer_email,\n" +
            "    c.phone as customer_phone\n" +
            "FROM combined_sums cs\n" +
            "LEFT JOIN customer_credits cc ON cs.customer_id = cc.customer_id\n" +
            "LEFT JOIN customer c ON cs.customer_id = c.id\n" +
            "GROUP BY cs.customer_id";

    private static final String QUERY_REPORT_LAST_CUSTOMERS = "SELECT \n" +
            "cu.id ,\n" +
            "CONCAT(cu.first_name ,' ', cu.last_name) as name,\n" +
            "cu.created_at as 'fecha',\n" +
            "cu.phone as 'phone' , \n" +
            "IFNULL(cu.birthday , 'No registrado') as birthday,\n" +
            "IFNULL(email , 'No tiene') as email ,\n" +
            "cu.created_by as created_by, \n" +
            "cu.gender \n "+
            "FROM customer cu \n" +
            "WHERE cu.created_at BETWEEN ? AND ? ";

    private static final String BOARDINGPASS_DEBT = "SELECT reservation_code, amount,total_amount,debt FROM boarding_pass WHERE customer_id = ? AND reservation_code = ?";
    private static final String PARCEL_DEBT = "SELECT\n" +
            "  p.id,\n" +
            "  CONVERT_TZ(p.created_at, '+00:00', '-07:00') AS 'date',\n" +
            "  CONVERT_TZ(i.created_at, '+00:00', '-07:00') AS 'invoice_date',\n" +
            "  i.uuid as 'UUID',\n" +
            "  p.parcel_tracking_code as code,\n" +
            "  p.parcel_tracking_code,\n" +
            "  'parcel' as 'service_type',\n" +
            "  p.debt as 'debt',\n" +
            "  CASE\n" +
            "    WHEN i.is_multiple = 1 THEN \n" +
            "      CONCAT(\n" +
            "        CASE \n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'cash' THEN 'GC-CO'\n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'credit' THEN 'GC-CR'\n" +
            "          ELSE 'PG'\n" +
            "        END,\n" +
            "        ' ',\n" +
            "        (SELECT id FROM parcels WHERE parcel_tracking_code = i.reference LIMIT 1)\n" +
            "      )\n" +
            "    WHEN i.is_multiple = 0 THEN \n" +
            "      CONCAT(\n" +
            "        CASE \n" +
            "          WHEN p.invoice_is_global = 1 THEN 'PG'\n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'cash' THEN 'GC-CO'\n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'credit' THEN 'GC-CR'\n" +
            "          ELSE 'PG'\n" +
            "        END, \n" +
            "        ' ', \n" +
            "        CASE \n" +
            "          WHEN p.invoice_is_global = 1 THEN p.invoice_id \n" +
            "          ELSE p.id \n" +
            "        END\n" +
            "      )\n" +
            "    ELSE ''\n" +
            "  END AS invoice_folio,\n" +
            " p.payment_condition,\n" +
            " p.pays_sender,\n" +
            " p.shipment_type,\n" +
            " p.invoice_id,\n" +
            " p.amount,\n" +
            " p.total_amount\n" +
            "FROM parcels p\n" +
            "LEFT JOIN customer AS c ON c.id = p.customer_id\n" +
            "LEFT JOIN invoice AS i ON i.id = p.invoice_id AND i.invoice_status = 'done'\n" +
            "WHERE customer_id = ? AND parcel_tracking_code = ?";
    private static final String RENTAL_DEBT = "select reservation_code , amount, debt from rental where customer_id = ? AND reservation_code = ?";
    private static final String PARCEL_PREPAID_DEBT = "SELECT\n" +
            "  p.id,\n" +
            "  CONVERT_TZ(p.created_at, '+00:00', '-07:00') AS 'date',\n" +
            "  CONVERT_TZ(i.created_at, '+00:00', '-07:00') AS 'invoice_date',\n" +
            "  i.uuid as 'UUID',\n" +
            "  p.tracking_code,\n" +
            "  p.tracking_code as 'code',\n" +
            "  'guia_pp' as 'service_type',\n" +
            "  p.debt as 'debt',\n" +
            "  CASE\n" +
            "    WHEN i.is_multiple = 1 THEN \n" +
            "      CONCAT(\n" +
            "        CASE \n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'cash' THEN 'GPP-CO'\n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'credit' THEN 'GPP-CR'\n" +
            "          ELSE 'PG'\n" +
            "        END,\n" +
            "        ' ',\n" +
            "        (SELECT id FROM parcels_prepaid WHERE tracking_code = i.reference LIMIT 1)\n" +
            "      )\n" +
            "    WHEN i.is_multiple = 0 THEN \n" +
            "      CONCAT(\n" +
            "        CASE \n" +
            "          WHEN p.invoice_is_global = 1 THEN 'PG'\n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'cash' THEN 'GPP-CO'\n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'credit' THEN 'GPP-CR'\n" +
            "          ELSE 'PG'\n" +
            "        END, \n" +
            "        ' ', \n" +
            "        CASE \n" +
            "          WHEN p.invoice_is_global = 1 THEN p.invoice_id \n" +
            "          ELSE p.id \n" +
            "        END\n" +
            "      )\n" +
            "    ELSE ''\n" +
            "  END AS invoice_folio,\n" +
            "  p.shipment_type,\n" +
            "  p.payment_condition,\n" +
            "  p.invoice_id,\n" +
            " p.amount,\n" +
            " p.total_amount\n" +
            "FROM\n" +
            "parcels_prepaid p\n" +
            "LEFT JOIN customer AS c ON c.id = p.customer_id\n" +
            "LEFT JOIN invoice AS i ON i.id = p.invoice_id AND i.invoice_status = 'done'\n" +
            "WHERE customer_id = ? AND tracking_code = ?";

    private static final String QUERY_CUSTOMER_WALLET_DETAIL_SERVICES_REPORT_DATE_OPTIONAL = "SELECT\n" +
            "\tinv.document_id,\n" +
            "\tbp.reservation_code AS service_code,\n" +
            "\tCOUNT(distinct(bpt.id)) AS quantity_products,\n" +
            "\tCONCAT(bo.prefix, ' - ', bd.prefix) AS route,\n" +
            "\t'boarding_pass' AS service,\n" +
            "\tbp.created_at,\n" +
            "\tbp.total_amount,\n" +
            "\tbp.debt,\n" +
            "\t(bp.total_amount - bp.debt) AS total_payments,\n" +
            "\tIF(bp.debt != 0 AND bp.created_at < DATE_SUB(NOW(), INTERVAL c.credit_time_limit DAY), TRUE, FALSE) AS is_expired\n" +
            "FROM boarding_pass bp\n" +
            "LEFT JOIN boarding_pass_passenger bpp ON bpp.boarding_pass_id = bp.id\n" +
            "LEFT JOIN boarding_pass_ticket bpt ON bpt.boarding_pass_passenger_id = bpp.id\n" +
            "LEFT JOIN boarding_pass_route bpr ON bpr.boarding_pass_id = bp.id\n" +
            "LEFT JOIN schedule_route_destination srd ON srd.id = bpr.schedule_route_destination_id\n" +
            "LEFT JOIN branchoffice bo ON IF(srd.terminal_origin_id IS NULL, bo.id = bp.terminal_origin_id, bo.id = srd.terminal_origin_id)\n" +
            "LEFT JOIN branchoffice bd ON IF(srd.terminal_destiny_id IS NULL, bd.id = bp.terminal_destiny_id, bd.id = srd.terminal_destiny_id)\n" +
            "LEFT JOIN customer c ON c.id = bp.customer_id\n" +
            "LEFT JOIN invoice inv ON inv.id = bp.invoice_id\n" +
            "WHERE c.id = ? AND bp.debt > 0 AND bp.boardingpass_status <> 0\n" +
            "AND bp.payment_condition = 'credit'  {boardingPassDateOptional} \n" +
            "GROUP BY bp.id\n" +
            "UNION\n" +
            "SELECT\n" +
            "\tinv.document_id,\n" +
            "\tp.parcel_tracking_code AS service_code,\n" +
            "\tp.total_packages AS quantity_products,\n" +
            "\tCONCAT(bo.prefix, ' - ', bd.prefix) AS route,\n" +
            "\t'parcel' AS service,\n" +
            "\tp.created_at,\n" +
            "\tp.total_amount,\n" +
            "\tp.debt,\n" +
            "\t(p.total_amount - p.debt) AS total_payments,\n" +
            "\tIF(p.debt != 0 AND p.created_at < DATE_SUB(NOW(), INTERVAL c.credit_time_limit DAY), TRUE, FALSE) AS is_expired\n" +
            "FROM parcels p\n" +
            "LEFT JOIN branchoffice bo ON bo.id = p.terminal_origin_id\n" +
            "LEFT JOIN branchoffice bd ON bd.id = p.terminal_destiny_id\n" +
            "LEFT JOIN customer c ON c.id = p.customer_id\n" +
            "LEFT JOIN invoice inv ON inv.id = p.invoice_id\n" +
            "WHERE c.id = ? AND p.debt > 0 AND p.parcel_status <> 4\n" +
            "AND p.payment_condition = 'credit' {parcelDateOptional}  \n" +
            "UNION " +
            "SELECT " +
            "inv.document_id, ppt.reservation_code AS service_code, ppt.total_tickets AS quantity_products, CONCAT(bo.prefix, ' - ', bbo.prefix) AS route," +
            "'prepaid' AS service, ppt.created_at, ppt.total_amount, ppt.debt, (ppt.total_amount - ppt.debt) AS total_payments," +
            "IF(ppt.debt != 0 AND ppt.created_at < DATE_SUB(NOW(), INTERVAL c.credit_time_limit DAY), TRUE, FALSE) AS is_expired \n" +
            "FROM prepaid_package_travel ppt \n" +
            "LEFT JOIN customer c ON c.id = ppt.customer_id \n" +
            "LEFT JOIN boarding_pass bpp ON bpp.prepaid_id = ppt.id \n" +
            "LEFT JOIN branchoffice bo ON bo.id = bpp.terminal_origin_id \n" +
            "LEFT JOIN branchoffice bbo ON bbo.id = bpp.terminal_destiny_id \n" +
            "LEFT JOIN invoice inv ON inv.id = bpp.invoice_id \n" +
            "WHERE c.id = ? AND ppt.debt > 0 AND ppt.payment_condition = 'credit' {packageTravelOptional} "+
            "ORDER BY created_at";

    private static final String QUERY_GET_CUSTOMER_BY_USER_ID = "SELECT * FROM customer where user_id = ? ";

    private static final String QUERY_GET_TIMES_EMAIL_REPEAT = "select count(id) as val from customer where email = ? ";

    private static final String QUERY_CHECK_RFC_BILLING_INFO = "select id from customer_billing_information where rfc = ? \n" +
            "and id != ? \n" +
            "and customer_id = ? ";

    private static final String QUERY_SELECT_GUIAPP_DEBTS = "SELECT \n" +
            "pp.id,\n" +
            "pp.debt,\n" +
            "'' waybill,\n" +
            "pp.tracking_code code,\n" +
            "i.document_id\n" +
            "FROM customer c\n" +
            "INNER JOIN parcels_prepaid pp ON pp.customer_id = c.id \n" +
            "LEFT JOIN invoice i on i.id = pp.invoice_id\n" +
            "WHERE c.id = ?\n" +
            "AND pp.debt > 0\n" +
            "AND pp.parcel_status NOT IN (4, 6);";

    private static final String QUERY_ACCOUNT_STATUS_DEBT_PARCELS = "SELECT\n" +
            "    p.created_at,\n" +
            "    NULL as payment_date,\n" +
            "    p.parcel_tracking_code AS detail,\n" +
            "    NULL AS invoice_folio,\n" +
            "    NULL AS invoice_id,\n" +
            "    NULL AS payment_complement_folio,\n" +
            "    NULL AS invoice_reference,\n" +
            "    p.total_amount,\n" +
            "    0 AS paid,\n" +
            "    NULL AS boarding_pass_id,\n" +
            "    p.id AS parcel_id,\n" +
            "    NULL AS rental_id,\n" +
            "    NULL AS parcel_prepaid_id,\n" +
            "    NULL AS payment_complement_id,\n" +
            "    NULL as ticket_id\n" +
            "FROM customer c\n" +
            "INNER JOIN parcels p ON p.customer_id = c.id\n" +
            "LEFT JOIN branchoffice origin ON origin.id = p.terminal_origin_id\n" +
            "LEFT JOIN city ON city.id = origin.city_id\n" +
            "LEFT JOIN branchoffice destiny ON destiny.id = p.terminal_destiny_id\n" +
            "LEFT JOIN parcels_prepaid_detail ppd ON ppd.parcel_id = p.id\n" +
            "WHERE c.id = %d\n" +
            "AND p.payment_condition = 'credit'\n" +
            "AND p.parcel_status NOT IN(4, 6)\n" +
            "AND (DATE(p.created_at) BETWEEN '%s' AND '%s')\n" +
            "%s\n" +
            "%s\n" +
            "AND ppd.id IS NULL\n" +
            "GROUP BY p.id";

    private static final String QUERY_ACCOUNT_STATUS_DEBT_PARCELS_PREPAID = "SELECT \n" +
            "   pp.created_at,\n" +
            "     NULL as payment_date,\n" +
            "     CONCAT(pp.tracking_code,\n" +
            "     ' ',\n" +
            "     COUNT(ppd.parcel_prepaid_id),\n" +
            "     ' ',\n" +
            "     pt.name,\n" +
            "     ' ',\n" +
            "     pprice.name_price,\n" +
            "     ' Hasta ',\n" +
            "     ppricek.max_km,\n" +
            "     ' km'" +
            "    ) AS detail,\n" +
            "    NULL AS invoice_folio,\n" +
            "    NULL AS invoice_id,\n" +
            "    NULL AS payment_complement_folio,\n" +
            "    NULL AS invoice_reference,\n" +
            "    pp.total_amount,\n" +
            "    0 AS paid,\n" +
            "    NULL AS boarding_pass_id,\n" +
            "    NULL AS parcel_id,\n" +
            "    NULL AS rental_id,\n" +
            "    pp.id AS parcel_prepaid_id,\n" +
            "    NULL AS payment_complement_id,\n" +
            "    NULL as ticket_id\n" +
            "FROM customer c\n" +
            "INNER JOIN parcels_prepaid pp ON pp.customer_id = c.id \n" +
            "INNER JOIN parcels_prepaid_detail ppd ON ppd.parcel_prepaid_id = pp.id\n" +
            "INNER JOIN package_types pt ON pt.id = ppd.package_type_id\n" +
            "INNER JOIN package_price pprice ON pprice.id = ppd.price_id\n" +
            "INNER JOIN package_price_km ppricek ON ppricek.id = ppd.price_km_id\n" +
            "LEFT JOIN branchoffice origin ON origin.id = pp.branchoffice_id \n" +
            "LEFT JOIN city ON city.id = origin.city_id\n" +
            "WHERE c.id = %d\n" +
            "AND pp.payment_condition = 'credit'\n" +
            "AND pp.parcel_status NOT IN(4, 6)\n" +
            "AND (DATE(pp.created_at) BETWEEN '%s' AND '%s')\n" +
            "%s\n" +
            "%s\n" +
            "GROUP BY pp.total_amount, ppd.parcel_prepaid_id, pt.name";

    private static final String QUERY_ACCOUNT_STATUS_DEBT_PAYMENTS = "SELECT\n" +
            "   dp.created_at,\n" +
            "   dp.payment_date,\n" +
            " CONCAT(\n" +
            "    CASE\n" +
            "        WHEN dp.parcel_id IS NOT NULL THEN p.parcel_tracking_code\n" +
            "        WHEN dp.parcel_prepaid_id IS NOT NULL THEN pp.tracking_code\n" +
            "    END,\n" +
            "    ' ',\n" +
            "    pm.name,\n" +
            "    ' (',\n" +
            "    payment.reference,\n" +
            "    ') '\n" +
            " ) AS detail,\n" +
            "    CASE\n" +
            "        WHEN dp.parcel_id IS NOT NULL THEN\n" +
            "            CONCAT(\n" +
            "                CASE\n" +
            "                    WHEN pi.is_multiple = 1 THEN \n" +
            "                        CONCAT(\n" +
            "                            CASE \n" +
            "                                WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'cash' THEN 'GC-CO'\n" +
            "                                WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'credit' THEN 'GC-CR'\n" +
            "                                ELSE 'PG'\n" +
            "                            END,\n" +
            "                            ' ',\n" +
            "                            (SELECT id FROM parcels WHERE parcel_tracking_code = pi.reference LIMIT 1)\n" +
            "                        )\n" +
            "                    WHEN pi.is_multiple = 0 THEN \n" +
            "                        CONCAT(\n" +
            "                            CASE \n" +
            "                                WHEN p.invoice_is_global = 1 THEN 'PG'\n" +
            "                                WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'cash' THEN 'GC-CO'\n" +
            "                                WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'credit' THEN 'GC-CR'\n" +
            "                                ELSE 'PG'\n" +
            "                            END,\n" +
            "                            ' ',\n" +
            "                            CASE \n" +
            "                                WHEN p.invoice_is_global = 1 THEN p.invoice_id \n" +
            "                                ELSE p.id \n" +
            "                            END\n" +
            "                        )\n" +
            "                    ELSE ''\n" +
            "                END\n" +
            "            )\n" +
            "        WHEN dp.parcel_prepaid_id IS NOT NULL THEN\n" +
            "            CONCAT(\n" +
            "                CASE\n" +
            "                    WHEN ppi.is_multiple = 1 THEN \n" +
            "                        CONCAT(\n" +
            "                            CASE \n" +
            "                                WHEN c2.parcel_type IS NOT NULL AND pp.payment_condition = 'cash' THEN 'GPP-CO'\n" +
            "                                WHEN c2.parcel_type IS NOT NULL AND pp.payment_condition = 'credit' THEN 'GPP-CR'\n" +
            "                                ELSE 'PG'\n" +
            "                            END,\n" +
            "                            ' ',\n" +
            "                            (SELECT id FROM parcels_prepaid WHERE tracking_code = ppi.reference LIMIT 1)\n" +
            "                        )\n" +
            "                    WHEN ppi.is_multiple = 0 THEN \n" +
            "                        CONCAT(\n" +
            "                            CASE \n" +
            "                                WHEN pp.invoice_is_global = 1 THEN 'PG'\n" +
            "                                WHEN c2.parcel_type IS NOT NULL AND pp.payment_condition = 'cash' THEN 'GPP-CO'\n" +
            "                                WHEN c2.parcel_type IS NOT NULL AND pp.payment_condition = 'credit' THEN 'GPP-CR'\n" +
            "                                ELSE 'PG'\n" +
            "                            END,\n" +
            "                            ' ',\n" +
            "                            CASE \n" +
            "                                WHEN pp.invoice_is_global = 1 THEN pp.invoice_id \n" +
            "                                ELSE pp.id \n" +
            "                            END\n" +
            "                        )\n" +
            "                    ELSE ''\n" +
            "                END\n" +
            "            )\n" +
            "    END as invoice_folio,\n" +
            "    CASE\n" +
            "     WHEN dp.parcel_id IS NOT NULL THEN pi.id\n" +
            "     WHEN dp.parcel_prepaid_id IS NOT NULL THEN ppi.id\n" +
            "     ELSE NULL\n" +
            "    END AS invoice_id,\n" +
            "    CONCAT(pc.serie, ' ', pc.folio) AS payment_complement_folio,\n" +
            "    CASE\n" +
            "     WHEN dp.parcel_id IS NOT NULL THEN pi.reference\n" +
            "     WHEN dp.parcel_prepaid_id IS NOT NULL THEN ppi.reference\n" +
            "     ELSE NULL\n" +
            "    END AS invoice_reference,\n" +
            "    0 AS total_amount,\n" +
            "    dp.amount AS paid,\n" +
            "    NULL AS boarding_pass_id,\n" +
            "    dp.parcel_id,\n" +
            "    NULL AS rental_id,\n" +
            "    dp.parcel_prepaid_id,\n" +
            "    dp.payment_complement_id,\n" +
            "    payment.ticket_id as ticket_id\n" +
            "FROM debt_payment dp\n" +
            "INNER JOIN payment ON payment.id = dp.payment_id\n" +
            "INNER JOIN payment_method pm ON pm.id = payment.payment_method_id\n" +
            "LEFT JOIN parcels p ON p.id = dp.parcel_id\n" +
            "LEFT JOIN branchoffice porigin ON porigin.id = p.terminal_origin_id\n" +
            "LEFT JOIN invoice pi ON pi.id = p.invoice_id\n" +
            "LEFT JOIN parcels_prepaid pp ON pp.id = dp.parcel_prepaid_id \n" +
            "LEFT JOIN branchoffice pporigin ON pporigin.id = pp.branchoffice_id\n" +
            "LEFT JOIN parcels_prepaid_detail ppd ON ppd.parcel_prepaid_id = pp.id\n" +
            "LEFT JOIN invoice ppi ON ppi.id = pp.invoice_id\n" +
            "LEFT JOIN customer c ON c.id = p.customer_id\n" +
            "LEFT JOIN customer c2 ON c2.id = pp.customer_id\n" +
            "LEFT JOIN payment_complement pc ON pc.id = dp.payment_complement_id\n" +
            "WHERE dp.customer_id = %d\n" +
            "AND (DATE(dp.created_at) BETWEEN '%s' AND '%s')\n" +
            "%s\n" +
            "%s\n" +
            "%s\n" +
            "GROUP BY payment.id, p.id, pp.id";

    private static final String QUERY_ACCOUNT_STATUS_DEBT_TO_DATE_PARCELS = "SELECT \n" +
            "    ROUND(MAX(total_amount), 2) AS total_amount, \n" +
            "    ROUND(MAX(total_payments), 2) AS total_payments\n" +
            "FROM (\n" +
            "    (SELECT \n" +
            "        0 AS total_amount, \n" +
            "        SUM(total_payments) AS total_payments \n" +
            "    FROM (\n" +
            "        SELECT\n" +
            "            SUM(dp.debt_ini) - SUM(dp.debt_end) AS total_payments\n" +
            "        FROM parcels p \n" +
            "        LEFT JOIN debt_payment dp ON dp.parcel_id = p.id AND dp.customer_id = p.customer_id\n" +
            "        LEFT JOIN branchoffice origin ON origin.id = p.terminal_origin_id\n" +
            "        WHERE p.customer_id = %d\n" +
            "        AND p.payment_condition = 'credit' \n" +
            "        AND dp.parcel_id IS NOT NULL\n" +
            "        AND DATE(dp.created_at) < '%s'\n" +
            "        AND DATE(p.created_at) < '%s'\n" +
            "        %s\n" +
            "        %s\n" +
            "        GROUP BY dp.parcel_id\n" +
            "    ) AS payments)\n" +
            "    UNION ALL \n" +
            "    (SELECT \n" +
            "        SUM(total_amount) AS total_amount, \n" +
            "        0 AS total_payments \n" +
            "    FROM (\n" +
            "        SELECT\n" +
            "            SUM(p.total_amount) AS total_amount\n" +
            "        FROM customer c\n" +
            "        LEFT JOIN parcels p ON p.customer_id = c.id\n" +
            "        LEFT JOIN branchoffice origin ON origin.id = p.terminal_origin_id\n" +
            "        WHERE c.id = %d\n" +
            "        AND p.payment_condition = 'credit' \n" +
            "        AND p.parcel_status NOT IN(4, 6)\n" +
            "        AND DATE(p.created_at) < '%s'\n" +
            "        %s\n" +
            "        %s\n" +
            "        GROUP BY c.id\n" +
            "    ) AS debt)\n" +
            ") AS parcels_info";

    private static final String QUERY_ACCOUNT_STATUS_DEBT_TO_DATE_PARCELS_PREPAID = "SELECT \n" +
            "    ROUND(MAX(total_amount), 2) AS total_amount, \n" +
            "    ROUND(MAX(total_payments), 2) AS total_payments\n" +
            "FROM (\n" +
            "    (SELECT \n" +
            "        0 AS total_amount, \n" +
            "        SUM(total_payments) AS total_payments \n" +
            "    FROM (\n" +
            "        SELECT\n" +
            "            SUM(dp.debt_ini) - SUM(dp.debt_end) AS total_payments\n" +
            "        FROM parcels_prepaid pp \n" +
            "        LEFT JOIN debt_payment dp ON dp.parcel_prepaid_id = pp.id AND dp.customer_id = pp.customer_id\n" +
            "        LEFT JOIN branchoffice origin ON origin.id = pp.branchoffice_id\n" +
            "        WHERE pp.customer_id = %d\n" +
            "        AND pp.payment_condition = 'credit' \n" +
            "        AND dp.parcel_prepaid_id IS NOT NULL\n" +
            "        AND DATE(dp.created_at) < '%s'\n" +
            "        AND DATE(pp.created_at) < '%s'\n" +
            "        %s\n" +
            "        %s\n" +
            "        GROUP BY dp.parcel_id\n" +
            "    ) AS payments)\n" +
            "    UNION ALL \n" +
            "    (SELECT \n" +
            "        SUM(total_amount) AS total_amount, \n" +
            "        0 AS total_payments \n" +
            "    FROM (\n" +
            "        SELECT\n" +
            "            SUM(pp.total_amount) AS total_amount\n" +
            "        FROM customer c\n" +
            "        LEFT JOIN parcels_prepaid pp ON pp.customer_id = c.id\n" +
            "        LEFT JOIN branchoffice origin ON origin.id = pp.branchoffice_id\n" +
            "        WHERE c.id = %d\n" +
            "        AND pp.payment_condition = 'credit' \n" +
            "        AND pp.parcel_status NOT IN(4, 6)\n" +
            "        AND DATE(pp.created_at) < '%s'\n" +
            "        %s\n" +
            "        %s\n" +
            "        GROUP BY c.id\n" +
            "    ) AS debt)\n" +
            ") AS parcels_prepaid_info";

    private static final String QUERY_ACCOUNT_STATUS_DEBT_CUSTOMER_DETAIL = "SELECT\n" +
            "    CONCAT(c.first_name, ' ', c.last_name) AS full_name,\n" +
            "    c.phone,\n" +
            "    c.email,\n" +
            "    c.credit_limit,\n" +
            "    c.credit_available,\n" +
            "    c.credit_balance,\n" +
            "    c.credit_time_limit\n" +
            "FROM customer c\n" +
            "WHERE c.id = ?;";

    private static final String QUERY_GET_CUSTOMER_DETAIL = "SELECT\n" +
            "c.*, b.name as branchoffice_name, u.name AS seller_name, seg.name as segment_name\n" +
            "FROM customer c\n" +
            "LEFT JOIN branchoffice b ON c.branchoffice_id = b.id\n" +
            "LEFT JOIN users u ON c.user_seller_id = u.id\n" +
            "LEFT JOIN segment seg ON c.segment_id = seg.id\n" +
            "WHERE c.id = ?;";

    private static final String QUERY_GET_CUSTOMER_DETAIL_PROMOS = "SELECT p.id, p.discount_code, p.name, p.description FROM promos p\n" +
            "INNER JOIN customers_promos cp ON cp.promo_id = p.id\n" +
            "WHERE cp.customer_id = ?\n" +
            "AND p.status = 1\n" +
            "AND cp.status = 1;";

    private static final String QUERY_GET_PASSENGERS_BY_CUSTOMER_ID = "SELECT\n" +
            "id, customer_id, first_name, last_name, birthday, gender, picture, need_preferential\n" +
            "FROM customer_passenger\n" +
            "WHERE customer_id = ?;";

    private static final String QUERY_GET_FCM_TOKENS_BY_CUSTOMER_ID = "SELECT\n" +
            "token\n" +
            "FROM fcm_token\n" +
            "WHERE customer_id = ?\n" +
            "AND is_valid = 1;";

    private static final String QUERY_GET_WALLET_ID_BY_USER_ID = "SELECT ew.id FROM e_wallet ew WHERE ew.user_id = ?";

    private static final String QUERY_GET_ID_BY_WALLET_CODE = "SELECT ew.* FROM e_wallet ew WHERE ew.code = ?";

    private static final String QUERY_SET_REFERRAL_ID_ON_WALLET = "UPDATE e_wallet SET referenced_by = ? WHERE id = ?";

    private static final String QUERY_GET_CUSTOMERS_BY_USER_SELLER_ID = "SELECT id FROM customer WHERE id > 0";

    private static final String QUERY_FIND_ALL_BILLING_INFORMATION_BY_CUSTOMER_ID = "SELECT cbi.*, " +
            "country.name AS country_name, state.name AS state_name, " +
            "street.name AS street_name," +
            "county.name AS county_name, city.name AS city_name " +
            "FROM customer_billing_information AS cbi " +
            "LEFT JOIN country ON cbi.country_id=country.id " +
            "LEFT JOIN state ON cbi.state_id=state.id " +
            "LEFT JOIN county ON cbi.county_id=county.id " +
            "LEFT JOIN city ON cbi.city_id=city.id " +
            "LEFT JOIN street ON cbi.street_id = street.id  " +
            "WHERE cbi.status != 3 AND cbi.customer_id = ?";

    private static final String QUERY_GET_PARCELS_WITH_DEBT = "SELECT \n" +
            "  p.id,\n" +
            "  CONVERT_TZ(p.created_at, '+00:00', '-07:00') AS 'date',\n" +
            "  CONVERT_TZ(i.created_at, '+00:00', '-07:00') AS 'invoice_date',\n" +
            "  i.uuid as 'UUID',\n" +
            "  p.parcel_tracking_code as 'code',\n" +
            "  'parcel' as 'service_type',\n" +
            "  p.debt as 'debt',\n" +
            "  CASE\n" +
            "    WHEN i.is_multiple = 1 THEN \n" +
            "      CONCAT(\n" +
            "        CASE \n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'cash' THEN 'GC-CO'\n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'credit' THEN 'GC-CR'\n" +
            "          ELSE 'PG'\n" +
            "        END,\n" +
            "        ' ',\n" +
            "        (SELECT id FROM parcels WHERE parcel_tracking_code = i.reference LIMIT 1)\n" +
            "      )\n" +
            "    WHEN i.is_multiple = 0 THEN \n" +
            "      CONCAT(\n" +
            "        CASE \n" +
            "          WHEN p.invoice_is_global = 1 THEN 'PG'\n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'cash' THEN 'GC-CO'\n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'credit' THEN 'GC-CR'\n" +
            "          ELSE 'PG'\n" +
            "        END, \n" +
            "        ' ', \n" +
            "        CASE \n" +
            "          WHEN p.invoice_is_global = 1 THEN p.invoice_id \n" +
            "          ELSE p.id \n" +
            "        END\n" +
            "      )\n" +
            "    ELSE ''\n" +
            "  END AS invoice_folio,\n" +
            " p.payment_condition,\n" +
            " p.pays_sender,\n" +
            " p.shipment_type,\n" +
            " p.invoice_id,\n" +
            " cbi.rfc\n" +
            "FROM parcels p\n" +
            "LEFT JOIN customer AS c ON c.id = p.customer_id\n" +
            "LEFT JOIN invoice AS i ON i.id = p.invoice_id AND i.invoice_status = 'done'\n" +
            "LEFT JOIN customer_billing_information AS cbi ON cbi.id = i.customer_billing_information_id\n" +
            "WHERE \n" +
            "  p.customer_id = ?\n" +
            "  AND {DATE_FILTER} BETWEEN ? AND ?\n" +
            "  AND p.debt > 0\n" +
            "  AND p.parcel_status != 4";

    private static final String QUERY_GET_PARCELS_PREPAID_WITH_DEBT = "SELECT \n" +
            "  p.id,\n" +
            "  CONVERT_TZ(p.created_at, '+00:00', '-07:00') AS 'date',\n" +
            "  CONVERT_TZ(i.created_at, '+00:00', '-07:00') AS 'invoice_date',\n" +
            "  i.uuid as 'UUID',\n" +
            "  p.tracking_code as 'code',\n" +
            "  'guia_pp' as 'service_type',\n" +
            "  p.debt as 'debt',\n" +
            "  CASE\n" +
            "    WHEN i.is_multiple = 1 THEN \n" +
            "      CONCAT(\n" +
            "        CASE \n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'cash' THEN 'GPP-CO'\n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'credit' THEN 'GPP-CR'\n" +
            "          ELSE 'PG'\n" +
            "        END,\n" +
            "        ' ',\n" +
            "        (SELECT id FROM parcels_prepaid WHERE tracking_code = i.reference LIMIT 1)\n" +
            "      )\n" +
            "    WHEN i.is_multiple = 0 THEN \n" +
            "      CONCAT(\n" +
            "        CASE \n" +
            "          WHEN p.invoice_is_global = 1 THEN 'PG'\n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'cash' THEN 'GPP-CO'\n" +
            "          WHEN c.parcel_type IS NOT NULL AND p.payment_condition = 'credit' THEN 'GPP-CR'\n" +
            "          ELSE 'PG'\n" +
            "        END, \n" +
            "        ' ', \n" +
            "        CASE \n" +
            "          WHEN p.invoice_is_global = 1 THEN p.invoice_id \n" +
            "          ELSE p.id \n" +
            "        END\n" +
            "      )\n" +
            "    ELSE ''\n" +
            "  END AS invoice_folio,\n" +
            "  p.shipment_type,\n" +
            "  p.payment_condition,\n" +
            "  p.invoice_id,\n" +
            "  cbi.rfc\n" +
            "FROM parcels_prepaid p\n" +
            "LEFT JOIN customer AS c ON c.id = p.customer_id\n" +
            "LEFT JOIN invoice AS i ON i.id = p.invoice_id AND i.invoice_status = 'done'\n" +
            "LEFT JOIN customer_billing_information AS cbi ON cbi.id = i.customer_billing_information_id\n" +
            "WHERE \n" +
            "  p.customer_id = ?\n" +
            "  AND {DATE_FILTER} BETWEEN ? AND ?\n" +
            "  AND p.debt > 0\n" +
            "  AND p.parcel_status != 4";

    private static final String QUERY_COUNT_GET_PARCELS_WITH_DEBT = "SELECT \n" +
            "COUNT(p.id) AS count, \n" +
            "COALESCE(SUM(p.debt), 0) AS total_debt\n" +
            "FROM {TABLE_NAME} p\n" +
            "LEFT JOIN invoice AS i ON i.id = p.invoice_id AND i.invoice_status = 'done'\n" +
            "WHERE \n" +
            " p.customer_id = ?\n" +
            " AND {DATE_FILTER} BETWEEN ? AND ?\n" +
            " AND p.debt > 0\n" +
            " AND p.parcel_status != 4";

    private static final String WHERE_BY_INVOICE_FOLIO = " AND (\n" +
            "    (i.is_multiple = 1 AND \n" +
            "      (SELECT id FROM parcels WHERE parcel_tracking_code = i.reference LIMIT 1) = ?)\n" +
            "    OR (i.is_multiple = 0 AND (p.id = ?))\n" +
            "  ) ";

    private static final String GET_FREQUENT_CUSOTMERS_BY_CUSTOMER_ID = "SELECT\n" +
            "   data.frequent_customer_id AS id,\n" +
            "    c.first_name,\n" +
            "    c.last_name,\n" +
            "    c.email,\n" +
            "    c.phone\n" +
            "FROM (\n" +
            "   SELECT 1 AS count, 1 AS frequent_customer_id\n" +
            "   UNION ALL\n" +
            "   (SELECT\n" +
            "      COUNT(p.id) AS count,\n" +
            "       CASE WHEN p.pays_sender IS TRUE\n" +
            "          THEN p.addressee_id ELSE p.sender_id\n" +
            "      END AS frequent_customer_id\n" +
            "   FROM parcels p\n" +
            "   INNER JOIN customer c ON c.id = p.customer_id\n" +
            "      AND c.status = 1\n" +
            "   WHERE p.customer_id = ?\n" +
            "   GROUP BY p.customer_id, frequent_customer_id\n" +
            "   ORDER BY count DESC LIMIT 3)) AS data\n" +
            "INNER JOIN customer c ON c.id = data.frequent_customer_id\n" +
            "GROUP BY c.id;";

    private static final String GET_CUSTOMER_CATALOGUE = "SELECT\n" +
        "  c.id AS id,\n" +
        "  CONCAT(c.first_name, ' ', c.last_name) AS customer_name,\n" +
        "  cbi.name AS business_name,\n" +
        "  cbi.rfc AS rfc,\n" +
        "  c.has_credit,\n" +
        "  c.credit_limit,\n" +
        "  us.email AS user_email,\n" +
        "  bo.prefix AS branchoffice,\n" +
        "  CASE\n" +
        "    WHEN c.parcel_type = 'agreement' THEN 'CONVENIO'\n" +
        "    WHEN c.parcel_type = 'guiapp' THEN 'GUIA PP'\n" +
        "    ELSE ''\n" +
        "  END AS parcel_type,\n" +
        "  asesor.name AS user_seller_name,\n" +
        "  c.company_nick_name,\n" +
        "  s.name AS segment,\n" +
        "  c.contact,\n" +
        "  c.phone,\n" +
        "  c.email,\n" +
        "  c.invoice_email,\n" +
        "  c.status\n" +
        "FROM customer c\n" +
        "LEFT JOIN customer_customer_billing_info ccbi\n" +
        "  ON ccbi.customer_id = c.id\n" +
        "LEFT JOIN customer_billing_information cbi\n" +
        "  ON cbi.id = ccbi.customer_billing_information_id\n" +
        "LEFT JOIN users us\n" +
        "  ON us.id = c.user_id\n" +
        "LEFT JOIN branchoffice bo\n" +
        "  ON bo.id = c.branchoffice_id\n" +
        "LEFT JOIN users asesor\n" +
        "  ON asesor.id = c.user_seller_id\n" +
        "LEFT JOIN segment s\n" +
        "  ON s.id = c.segment_id\n" +
        "WHERE c.status = 1\n" +
        "{WHERE_QUERY}" +
        "ORDER BY business_name";

    private static final String QUERY_CHECK_CUSTOMER_IS_ASSIGNED_TO_SELLER = "SELECT\n" +
            "   COUNT(c.id) > 0 AS is_assigned\n" +
            "FROM customer c\n" +
            "WHERE c.id = ?\n" +
            "   AND c.user_seller_id = ?";
}
