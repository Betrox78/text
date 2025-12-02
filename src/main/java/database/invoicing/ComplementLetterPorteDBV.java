package database.invoicing;

import database.commons.DBVerticle;
import database.invoicing.handlers.parcelInvoiceDBV.ComplementDetail;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import utils.UtilsGeneral;
import utils.UtilsID;

import javax.swing.text.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import database.invoicing.InvoiceDBV.ServiceTypes;
import java.util.concurrent.atomic.AtomicReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

import static database.invoicing.InvoiceDBV.COURIER_RANGES;
import static service.commons.Constants.ACTION;

/**
 *
 * @author daliacarlon
 */
public class ComplementLetterPorteDBV extends DBVerticle {
    public static final String ACTION_SEARCH_CLAVE_PRDUCT = "ComplementLetterPorteDBV.searchClaveProduct";
    public static final String ACTION_GET_CONFIG_AUTOTRANSPORTE = "ComplementLetterPorteDBV.getConfigAutotransporte";
    public static final String ACTION_CLAVEPRODUCTO_REGISTER = "ComplementLetterPorteDBV.registerClaveProducto";
    public static final String ACTION_CLAVEPRODUCTO_UPDATE = "ComplementLetterPorteDBV.updateClaveProducto";
    public static final String ACTION_CFDI_COMPLEMENT_CP = "ComplementLetterPorteDBV.insertCfdiComplementCp";
    public static final String ACTION_GET_CFDI_BODY = "ComplementLetterPorteDBV.getCfdiBody";
    public static final String ACTION_REGISTER_MANIFEST_CCP = "ComplementLetterPorteDBV.registerManifestccp";
    public static final String ACTION_GET_CFDI_STATUS = "ComplementLetterPorteDBV.getCfdiStatus";
    public static final String ACTION_INSERT_XML_TIMBRADO = "ComplementLetterPorteDBV.insertXMl";
    public static final String ACTION_PRINT_MANIFEST_CP = "ComplementLetterPorteDBV.updatePrintManifestCp";
    public static final String ACTION_GET_NO_PRINT = "ComplementLetterPorteDBV.getManifestNoPrint";
    public static final String ACTION_GET_CFDI_BODY_SITE = "ComplementLetterPorteDBV.getCfdiBodySite";
    public static final String ACTION_REGISTER_MANIFEST_CCP_EAD = "ComplementLetterPorteDBV.registerManifestccpEad";
    public static final String ACTION_GET_CFDI_GLOBAL_BODY = "ComplementLetterPorteDBV.getCfdiGlobalBody";

    public static final String ACTION_GET_MANIFEST_CFDI_EAD = "ComplementLetterPorteDBV.getManifestCfdiEad";
    public static final String REGISTER_TRAVEL_LOG_CCP = "ComplementLetterPorteDBV.registertravelLogCCP";
    public static final String ACTION_GET_CCP_XML_INFO = "ComplementLetterPorteDBV.getCCPXMLInfo";
    public static final String ACTION_GET_TRAVEL_LOG_CCPS = "ComplementLetterPorteDBV.getTravelLogCCPs";
    public static final String ACTION_UPDATE_TL_STAMP_STATUS = "ComplementLetterPorteDBV.updateTLStampStatus";
    public static final String ACTION_GET_RELATED_UUID = "ComplementLetterPorteDBV.getRelatedUUID";
    public static final String ACTION_GET_MANIFEST_CCP_XML_INFO = "ComplementLetterPorteDBV.getManifestCCPXMLInfo";
    public static final String GET_COMPLEMENT_DETAIL = "ComplementLetterPorteDBV.getComplementDetail";
    public static final String ACTION_REGISTER_EAD_RAD_CCP = "ComplementLetterPorteDBV.registerEadRadCCP";
    ComplementDetail complementDetail;
    @Override
    public String getTableName() {
        return "package_types";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
        complementDetail = new ComplementDetail(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_SEARCH_CLAVE_PRDUCT:
                this.searchClaveProduct(message);
                break;
            case ACTION_GET_CONFIG_AUTOTRANSPORTE:
                this.getConfigAutotransporte(message);
                break;
            case ACTION_CLAVEPRODUCTO_REGISTER:
                this.registerClaveProducto(message);
                break;
            case ACTION_CLAVEPRODUCTO_UPDATE:
                this.updateClaveProducto(message);
                break;
            case ACTION_CFDI_COMPLEMENT_CP:
                this.insertCfdiComplementCp(message);
                break;
            case ACTION_GET_CFDI_BODY:
                this.getCfdiBody(message);
                break;
            case ACTION_REGISTER_MANIFEST_CCP:
                this.registerManifestccp(message);
                break;
            case ACTION_GET_CFDI_STATUS:
                this.getCfdiStatus(message);
                break;
            case ACTION_INSERT_XML_TIMBRADO:
                this.insertXMl(message);
                break;
            case ACTION_PRINT_MANIFEST_CP:
                this.updatePrintManifestCp(message);
                break;
            case ACTION_GET_NO_PRINT:
                this.getManifestNoPrint(message);
                break;
            case ACTION_GET_CFDI_BODY_SITE:
                this.getCfdiBodySite(message);
                break;
            case ACTION_REGISTER_MANIFEST_CCP_EAD:
                this.registerManifestccpEad(message);
                break;
            case ACTION_GET_CFDI_GLOBAL_BODY:
                this.getCfdiGlobalBody(message);
                break;
            case ACTION_GET_MANIFEST_CFDI_EAD:
                this.getManifestCfdiEad(message);
                break;
            case REGISTER_TRAVEL_LOG_CCP:
                this.registertravelLogCCP(message);
                break;
            case GET_COMPLEMENT_DETAIL:
                complementDetail.handle(message);
                break;
            case ACTION_GET_CCP_XML_INFO:
                this.getCCPXMLInfo(message);
                break;
            case ACTION_GET_MANIFEST_CCP_XML_INFO:
                this.getManifestCCPXMLInfo(message);
                break;
            case ACTION_REGISTER_EAD_RAD_CCP:
                this.registerEadRadCCP(message);
                break;
            case ACTION_GET_TRAVEL_LOG_CCPS:
                this.getTravelLogCCPs(message);
                break;
            case ACTION_UPDATE_TL_STAMP_STATUS:
                this.updateTLStampStatus(message);
                break;
            case ACTION_GET_RELATED_UUID:
                this.getRelatedUUID(message);
                break;
        }
    }

    private void searchClaveProduct(Message<JsonObject> message) {
        try {
            String QUERY = QUERY_SEARCH_ADVANCED + SEARCH_TERM_FILTER + SEARCH_ORDER_BY;
            JsonArray param = new JsonArray();
            String searchTerm = "%" + message.body().getString("searchTerm") + "%";
            param.add(searchTerm);
            Integer limit = message.body().getInteger("limit");
            if (limit > 0) QUERY += "LIMIT " + limit;
            dbClient.queryWithParams(QUERY, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()) {
                        dbClient.query(QUERY_SEARCH_ADVANCED + SEARCH_TERM_FILTER_NOT_EXISTS, replyNE -> {
                            try {
                                if (replyNE.failed()) {
                                    throw replyNE.cause();
                                }
                                List<JsonObject> resultNE = replyNE.result().getRows();
                                message.reply(new JsonArray(resultNE));
                            } catch (Throwable ex) {
                                ex.printStackTrace();
                                reportQueryError(message, ex);
                            }
                        });
                    } else {
                        message.reply(new JsonArray(result));
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

    private void getConfigAutotransporte(Message<JsonObject> message) {
        try {
            String QUERY = GET_CONFIG_AUTOTRANSPORTE;
            dbClient.queryWithParams(QUERY, null, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(result));
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

    private void  registerClaveProducto(Message<JsonObject> message) {

        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject data = message.body();



                this.registerCProducto(conn,data).whenComplete((res, error) -> {

                    try {
                        if(error != null){
                            this.rollback(conn, error, message);
                        } else {
                            this.commit(conn, message, res);
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                        this.rollback(conn, error, message);

                    }

                });

            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });

    }
    private CompletableFuture<JsonObject> registerCProducto(SQLConnection conn, JsonObject  params ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();


        JsonObject paramRAD = new JsonObject()
                .put("clave",     params.getString("clave"))
                .put("description",     params.getString("description"))
                .put("similar_words",     params.getString("similar_words"))
                .put("init_date",     params.getString("init_date"))
                .put("end_date",     params.getString("end_date"))
                .put("dangerous_material", Integer.parseInt(params.getString("dangerous_material")));
        String insert = this.generateGenericCreate("c_ClaveProdServCP",paramRAD);

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

    private void updateClaveProducto(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer status=body.getInteger("status");
        JsonArray params = new JsonArray()
                .add(status)
                .add(body.getInteger("updated_by")).add(body.getInteger("id"));

        this.dbClient.queryWithParams(UPDATE_STATUS_CLAVE_PRODUCTO, params, reply -> {
            if (reply.succeeded()) {

                message.reply(reply.succeeded());

            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void  insertCfdiComplementCp(Message<JsonObject> message) {

        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject data = message.body();



                this.registerCfdiCp(conn,data).whenComplete((res, error) -> {

                    try {
                        if(error != null){
                            this.rollback(conn, error, message);
                        } else {
                            this.commit(conn, message, res);
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                        this.rollback(conn, error, message);

                    }

                });

            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });

    }
    private CompletableFuture<JsonObject> registerCfdiCp(SQLConnection conn, JsonObject  params ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();


        String cfdi= params.getJsonObject("cartaporte").getJsonArray("jsons").toString();
        cfdi = cfdi.replaceAll("'","" ) ;
        JsonObject paramRAD = new JsonObject()
                .put("id_parcel",     params.getJsonObject("result").getInteger("id"))
                .put("tipo_cfdi",    2)
                .put("cfdi_body",     params.getJsonObject("cartaporte").getJsonArray("jsons").toString())
                .put("status_cfdi",    1)
                .put("created_by",     params.getInteger("id_user"));

        String insert = this.generateGenericCreate("parcel_invoice_complement",paramRAD);

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

    private void getCfdiBody(Message<JsonObject> message) {
        try {
            String QUERY = QUERY_GET_MANIFEST_DETAIL_CFDI;
            JsonArray param = new JsonArray();

            String tracking_code = "";
            tracking_code = message.body().getString("tracking_code");
            param.add(tracking_code);

            dbClient.queryWithParams(QUERY, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(result));
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

    private void getCfdiBodySite(Message<JsonObject> message) {
        try {
            String QUERY = "";
            JsonArray param = new JsonArray();

            String tracking_code = message.body().getString("tracking_code");

            ServiceTypes serviceName = ServiceTypes.getTypeByReservationCode(tracking_code);

            if(ServiceTypes.PARCEL.equals(serviceName)) {
                QUERY = QUERY_GET_MANIFEST_DETAIL_CFDI_SITE_PARCEL;
            } else if (ServiceTypes.BOARDING_PASS.equals(serviceName)) {
                QUERY = QUERY_GET_MANIFEST_DETAIL_CFDI_SITE_BOARDINGPASS;
            } else if(ServiceTypes.GUIA_PP.equals(serviceName)) {
                QUERY = QUERY_GET_MANIFEST_DETAIL_CFDI_SITE_GUIA_PP;
            }  else if(ServiceTypes.PREPAID_BOARDING_PASS.equals(serviceName)) {
                QUERY = QUERY_GET_MANIFEST_DETAIL_CFDI_SITE_PREPAID_BOARDINGPASS;
            }

            param.add(tracking_code);
            dbClient.queryWithParams(QUERY, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(result));
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

    private void  registerManifestccp(Message<JsonObject> message) {

        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject data = message.body();
                if(data.getJsonObject("data").getInteger("register")==1){
                    this.registerManifestMaster(conn,data).whenComplete((res, error) -> {

                        try {
                            if(error != null){
                                this.rollback(conn, error, message);
                            } else {
                                 this.commit(conn, message, res);

                            }


                        } catch (Exception e) {
                            e.printStackTrace();
                            this.rollback(conn, error, message);

                        }

                    });
                }else{
                    this.registerManifestDetail(conn,data).whenComplete((res2, error2) -> {

                        try {
                            if(error2 != null){
                                this.rollback(conn, error2, message);
                            } else {
                                this.registerCcp(conn,data).whenComplete((res3, error3) -> {

                                    try {
                                        if(error3 != null){
                                            this.rollback(conn, error3, message);
                                        } else {
                                            this.commit(conn, message, res3);
                                        }


                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        this.rollback(conn, error2, message);

                                    }

                                });
                            }


                        } catch (Exception e) {
                            e.printStackTrace();
                            this.rollback(conn, error2, message);

                        }

                    });
                }

            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });

    }
    private CompletableFuture<JsonObject> registerManifestDetail(SQLConnection conn, JsonObject  params ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

                    JsonObject paramsMInsert = new JsonObject()
                            .put("id_parcel_invoice_manifest" ,  params.getJsonObject("data").getInteger("id_parcel_invoice_manifest"))
                            .put("id_parcel_invoice_complement",    params.getJsonObject("data").getJsonObject("cartaPorte").getInteger("parcel_invoice_complement_id"))
                            .put("status",    1)
                            .put("created_by",    params.getJsonObject("data").getInteger("idUser"));
                    String insertD = this.generateGenericCreate("parcel_invoice_manifest_details",paramsMInsert);

                    conn.update(insertD,(AsyncResult<UpdateResult> reply2) -> {
                        try{
                            if(reply2.succeeded()){
                                future.complete(reply2.result().toJson());

                            } else {
                                future.completeExceptionally(reply2.cause());
                            }
                        } catch (Exception e){
                            future.completeExceptionally(e);
                        }
                    });

        return future;
    }
    private CompletableFuture<JsonObject> registerManifestMaster(SQLConnection conn, JsonObject  params ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String tracking_code=UtilsID.generateID("CCP"+params.getJsonObject("data").getInteger("tipo_servicio").toString());
        JsonObject paramsInsert = new JsonObject()
                .put("tracking_code" ,tracking_code )
                .put("status",    1)
                .put("id_branchOffice",  params.getJsonObject("data").getInteger("id_branchoffice"))
                .put("tipo_cfdi",  params.getJsonObject("data").getInteger("tipo_cfdi"))
                .put("tipo_servicio",  params.getJsonObject("data").getInteger("tipo_servicio"))
                .put("created_by",    params.getJsonObject("data").getInteger("idUser"));
        String insertM = this.generateGenericCreate("parcel_invoice_manifest",paramsInsert);
        conn.update(insertM,(AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.succeeded()){
                    future.complete(new JsonObject().put("tracking_code",tracking_code).put("id_parcel_invoice_manifest",reply.result().toJson().getJsonArray("keys").getInteger(0)));
                } else {
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });




        return future;
    }

    private CompletableFuture<JsonObject> registerCcp(SQLConnection conn, JsonObject  params ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {

            JsonObject body = params.getJsonObject("data");
            params.remove("DatosTransporte");
            params.remove("Seguros");
            params.remove("Seguros");


            String body_cfdi = params.getJsonObject("data").getJsonObject("cartaPorte").getString("cfdi_body");
            JsonArray cfdi_body_array = new JsonArray(body_cfdi);
            JsonObject cfdi_body = new JsonObject(cfdi_body_array.getJsonObject(0).toString());
            cfdi_body.getJsonObject("Complementos").getJsonObject("CartaPorte").remove("FiguraTransporte");
            cfdi_body.getJsonObject("Complementos").getJsonObject("CartaPorte").put("FiguraTransporte", body.getJsonArray("FiguraTransporte"));
            cfdi_body.getJsonObject("Complementos").getJsonObject("CartaPorte").put("IdentificacionVehicular", body.getJsonObject("IdentificacionVehicular"));
            //   cfdi_body.remove("DatosTransporte");
            body.remove("Emisor");
            body.remove("Receptor");
            cfdi_body.put("Emisor",new JsonObject()
                    .put("Rfc","PPA190515V16")
                    .put("Nombre","PTX PAQUETERIA SA DE CV")
                    .put("RegimenFiscal","601"));
            cfdi_body.put("Receptor",new JsonObject()
                    .put("Rfc","PPA190515V16")
                    .put("Nombre","PTX PAQUETERIA SA DE CV")
                    .put("ResidenciaFiscal","null")
                    .put("NumRegIdTrib","null")
                    .put("UsoCFDI","P01"));
            //cfdi_body.remove("Seguros");
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

            JsonObject paramPic = new JsonObject()
                    .put("status_cfdi", 2)
                    .put("updated_at", format1.format(Calendar.getInstance().getTime()))
                    .put("updated_by", body.getInteger("idUser"))
                    .put("cfdi_body", new JsonArray().add(cfdi_body).toString())
                    .put("id", body.getJsonObject("cartaPorte").getInteger("parcel_invoice_complement_id"));

            String update = this.generateGenericUpdateString("parcel_invoice_complement", paramPic);

            conn.update(update, (AsyncResult<UpdateResult> reply) -> {
                try {
                    if (reply.succeeded()) {
                        future.complete(new JsonObject().put("body_cfdi", cfdi_body.toString()).put("result", reply.result().toJson()));
                    } else {
                        future.completeExceptionally(reply.cause());
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<JsonObject> registerCcpEAD(SQLConnection conn, JsonObject  params ){ //ultima milla
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {

            JsonObject body = params.getJsonObject("data");
            params.remove("DatosTransporte");
            params.remove("Seguros");
            params.remove("Seguros");


            String body_cfdi = params.getJsonObject("data").getJsonObject("cartaPorte").getString("cfdi_body");
            JsonArray cfdi_body_array = new JsonArray(body_cfdi);
            JsonObject cfdi_body = new JsonObject(cfdi_body_array.getJsonObject(0).toString());
            cfdi_body.getJsonObject("Complementos").getJsonObject("CartaPorte").remove("FiguraTransporte");
            cfdi_body.getJsonObject("Complementos").getJsonObject("CartaPorte").put("FiguraTransporte", body.getJsonArray("FiguraTransporte"));
            cfdi_body.getJsonObject("Complementos").getJsonObject("CartaPorte").put("IdentificacionVehicular", body.getJsonObject("IdentificacionVehicular"));
            //   cfdi_body.remove("DatosTransporte");
            body.remove("Emisor");
            body.remove("Receptor");
            cfdi_body.put("Emisor",new JsonObject()
                    .put("Rfc","PPA190515V16")
                    .put("Nombre","PTX PAQUETERIA SA DE CV")
                    .put("RegimenFiscal","601"));
            cfdi_body.put("Receptor",new JsonObject()
                    .put("Rfc","PPA190515V16")
                    .put("Nombre","PTX PAQUETERIA SA DE CV")
                    .put("ResidenciaFiscal","null")
                    .put("NumRegIdTrib","null")
                    .put("UsoCFDI","P01"));
            //cfdi_body.remove("Seguros");
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            //Removiendo complemento carta porte y preparando para ultima milla
            String tracking_code=  body.getJsonObject("cartaPorte").getString("parcel_tracking_code");
            cfdi_body.getJsonObject("Complementos").remove("CartaPorte");
            for(int i = 0 ; i <   cfdi_body.getJsonArray("Conceptos").size() ; i++ ) {
                cfdi_body.getJsonArray("Conceptos").getJsonObject(i).remove("Unidad");
                cfdi_body.getJsonArray("Conceptos").getJsonObject(i).put("Unidad","XPK");
                cfdi_body.getJsonArray("Conceptos").getJsonObject(i).put("ObjetoImp","01");
                cfdi_body.getJsonArray("Conceptos").getJsonObject(i).remove("ClaveProdServ");
                cfdi_body.getJsonArray("Conceptos").getJsonObject(i).put("ClaveProdServ","31181701");
                cfdi_body.getJsonArray("Conceptos").getJsonObject(i).remove("Descripcion");
                cfdi_body.getJsonArray("Conceptos").getJsonObject(i).put("Descripcion","Paquetes");
                cfdi_body.getJsonArray("Conceptos").getJsonObject(i).remove("NoIdentificacion");
                cfdi_body.getJsonArray("Conceptos").getJsonObject(i).put("NoIdentificacion",tracking_code);
            }


            JsonObject paramPic = new JsonObject()
                    .put("status_cfdi", 1)
                    .put("updated_at", format1.format(Calendar.getInstance().getTime()))
                    .put("updated_by", body.getInteger("idUser"))
                    .put("cfdi_body", new JsonArray().add(cfdi_body).toString())
                    .put("id", body.getJsonObject("cartaPorte").getInteger("parcel_invoice_complement_id"));

            String update = this.generateGenericUpdateString("parcel_invoice_complement", paramPic);

            conn.update(update, (AsyncResult<UpdateResult> reply) -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    future.complete(new JsonObject().put("body_cfdi", cfdi_body.toString()).put("result", reply.result().toJson()));

                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (Throwable e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private void getCfdiStatus(Message<JsonObject> message) {
        JsonObject body=message.body();
        try {
            String QUERY ="";
            JsonArray param = new JsonArray();

            String tracking_code = "";
            Boolean isEad= body.getBoolean("isead");
            tracking_code = body.getString("tracking_code");
            param.add(tracking_code);
            if(!isEad){
                QUERY = QUERY_GET_MANIFEST_DETAIL_CFDI_STATUS;
            }else{
                QUERY=  QUERY_GET_MANIFEST_DETAIL_CFDI_STATUS_EAD;
            }
            dbClient.queryWithParams(QUERY, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(result));
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

    private void insertXMl(Message<JsonObject> message) {
            try{


                JsonObject body = message.body();
                SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

                String xmlBase64 = body.getString("XML");
                byte[] decodedXML = Base64.getDecoder().decode(xmlBase64);
                String decodedXMLString=new String(decodedXML);
                JsonArray params = new JsonArray()
                        .add(body.getString("XML"))
                        .add(1)
                        .add(decodedXMLString.contains("TimbreFiscalDigital")?4:6)
                        .add(format1.format(Calendar.getInstance().getTime()))
                        .add(body.getInteger("updated_by"))
                    .add(body.getInteger("parcel_invoice_complement_id"));

                this.dbClient.queryWithParams(UPDATE_PARCEL_INVOICE_COMPLEMENT_CADENA_ORIGINAL_XML, params, reply -> {
                    if (reply.succeeded()) {

                        message.reply(reply.succeeded());

                    } else {
                        reportQueryError(message, reply.cause());
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                reportQueryError(message, ex);
            }

    }
    private void updatePrintManifestCp(Message<JsonObject> message) {
        JsonObject body = message.body();
        JsonArray params = new JsonArray()
                .add(body.getJsonObject("data").getString("tracking_code"));
           //    .add(body.getInteger("updated_by")).add(body.getInteger("id"));
        String query="SELECT print_count FROM parcel_invoice_manifest WHERE tracking_code=?";
        this.dbClient.queryWithParams(query, params, reply -> {
            if (reply.succeeded()) {
                SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                JsonObject count=reply.result().toJson();
                int numC=count.getJsonArray("rows").getJsonObject(0).getInteger("print_count")==null?0:count.getJsonArray("rows").getJsonObject(0).getInteger("print_count");
                int countPrint=0;
                if(numC>0){
                    countPrint=numC+1;
                }else{
                    countPrint=1;
                }
                JsonArray param = new JsonArray()
                        .add(format1.format(Calendar.getInstance().getTime()))
                        .add(body.getJsonObject("data").getInteger("user_id"))
                        .add(countPrint)
                        .add(body.getJsonObject("data").getInteger("id_branchoffice"))
                        .add(format1.format(Calendar.getInstance().getTime()))
                        .add(body.getJsonObject("data").getString("tracking_code"));
                this.dbClient.queryWithParams(UPDATE_PRINT_COUNT_MANIFEST_CCP, param, reply2 -> {
                    if (reply2.succeeded()) {
                        message.reply(reply2.succeeded());

                    } else {
                        reportQueryError(message, reply2.cause());
                    }
                });
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void getManifestNoPrint(Message<JsonObject> message) {
        JsonObject body=message.body();
        try {
            String QUERY = GET_MANIFEST_NO_PRINT;
            JsonArray param = new JsonArray();

            int idBranch = 0;
            idBranch =  Integer.parseInt(body.getString("id_branchoffice"));
            param.add(idBranch);
            dbClient.queryWithParams(QUERY, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(result));
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
    private void  registerManifestccpEad(Message<JsonObject> message) {

        this.startTransaction(message, (SQLConnection conn) -> {
            try {
                JsonObject data = message.body();
                if(data.getJsonObject("data").getInteger("register")==1){
                    this.registerManifestMaster(conn,data).whenComplete((res, error) -> {

                        try {
                            if(error != null){
                                this.rollback(conn, error, message);
                            } else {
                                this.commit(conn, message, res);

                            }


                        } catch (Throwable e) {
                            e.printStackTrace();
                            this.rollback(conn, error, message);

                        }

                    });
                }else{
                    this.registerCfdiCpEAD(conn,data).whenComplete((res, error) -> {

                        try {
                            if(error != null){

                                this.rollback(conn, error, message);

                            } else {
                             //   this.commit(conn, message, res);
                                int parcel_invoice_complement_id=res.getJsonArray("keys").getInteger(0);
                                data.getJsonObject("data").getJsonObject("cartaPorte").put("parcel_invoice_complement_id",parcel_invoice_complement_id);
                                this.registerManifestDetail(conn,data).whenComplete((res2, error2) -> {

                                    try {
                                        if(error2 != null){
                                            this.rollback(conn, error2, message);
                                        } else {
                                            this.registerCcpEAD(conn,data).whenComplete((res3, error3) -> {

                                                try {
                                                    if(error3 != null){
                                                        this.rollback(conn, error3, message);
                                                    } else {
                                                        res3.put("parcel_invoice_complement_id",parcel_invoice_complement_id);
                                                        this.commit(conn, message, res3);
                                                    }


                                                } catch (Throwable e) {
                                                    e.printStackTrace();
                                                    this.rollback(conn, error2, message);

                                                }

                                            });
                                        }


                                    } catch (Throwable e) {
                                        e.printStackTrace();
                                        this.rollback(conn, error2, message);

                                    }

                                });

                            }


                        } catch (Throwable e) {
                            e.printStackTrace();
                            this.rollback(conn, error, message);

                        }

                    });

                }

            } catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });

    }

    private CompletableFuture<JsonObject> registerCfdiCpEAD(SQLConnection conn, JsonObject  params ){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();


        JsonObject paramRAD = new JsonObject()
                .put("id_parcel",     params.getJsonObject("data").getInteger("id_parcel"))
                .put("tipo_cfdi",    1)
                .put("cfdi_body",      params.getJsonObject("data").getJsonObject("cartaPorte").getString("cfdi_body"))
                .put("status_cfdi",    1)
                .put("created_by",     params.getJsonObject("data").getInteger("idUser"));

        String insert = this.generateGenericCreate("parcel_invoice_complement",paramRAD);

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
    private void getManifestCfdiEad(Message<JsonObject> message) {
        JsonObject body=message.body();
        try {
            String QUERY = GET_MANIFEST_CDFI_EAD;
            String date_init=body.getString("date_init");
            String date_end=body.getString("date_end");
            //     JsonArray param = new JsonArray();

            int idBranch = 0;
            idBranch =  Integer.parseInt(body.getString("terminal_origin_id"));
            //  param.add(idBranch);

            if(date_init.equals("null") && date_end.equals("null")){
                QUERY = GET_MANIFEST_CDFI_EAD+" and id_branchOffice="+idBranch;

            }
            else{
                QUERY = GET_MANIFEST_CDFI_EAD+" and id_branchOffice="+idBranch+" AND (created_at>='"+date_init+" 00:00:00' AND created_at<='"+date_end+" 23:59:59')" ;

            }
            dbClient.queryWithParams(QUERY, null, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(result));
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


private void getCfdiGlobalBody(Message<JsonObject> message) {
        try {
            String QUERY = "";
            JsonArray param = new JsonArray();

            String tracking_code = message.body().getString("tracking_code");

            ServiceTypes serviceName = ServiceTypes.getTypeByReservationCode(tracking_code);

            if(ServiceTypes.PARCEL.equals(serviceName)) {
                QUERY = QUERY_GET_CFDI_GLOBAL_PARCEL;
            } else if(ServiceTypes.GUIA_PP.equals(serviceName)) {
                QUERY = QUERY_GET_CFDI_GLOBAL_PARCEL_PREPAID;
            }

            param.add(tracking_code);
            dbClient.queryWithParams(QUERY, param, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(result));
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

    private void registertravelLogCCP(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonObject ccpElement =body.getJsonObject("ccp_element");

            this.startTransaction(message, conn -> {
                JsonObject tl_ccp = new JsonObject()
                        .put("travel_log_id", body.getInteger("travel_logs_id"))
                        .put("invoice_status", "en proceso")
                        .put("ccp_type", ccpElement.getString("ccp_type"))
                        .put("created_by", body.getInteger("created_by"));

                if (ccpElement.getInteger("customer_id") != null) {
                    tl_ccp.put("customer_id", ccpElement.getInteger("customer_id"));
                }

                if (ccpElement.getInteger("customer_counter") != null) {
                    tl_ccp.put("customer_counter", ccpElement.getInteger("customer_counter"));
                }

                if (ccpElement.getInteger("specific_parcel_id") != null) {
                    tl_ccp.put("specific_parcel_id", ccpElement.getInteger("specific_parcel_id"));
                }

                String insertCCP = this.generateGenericCreate("travel_logs_ccp", tl_ccp);

                conn.update(insertCCP, (AsyncResult<UpdateResult> resp) -> {
                    try {
                        if(resp.failed()) {
                            throw new Exception(resp.cause());
                        }
                        Integer ccpID = resp.result().getKeys().getInteger(0);
                        this.commit(conn,message, new JsonObject().put("travel_logs_ccp_id", ccpID));
                    } catch (Throwable t) {
                        t.printStackTrace();
                        this.rollback(conn,t,message);
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private void getCCPXMLInfo(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer id = body.getInteger("id");
            String QUERY = "";
            JsonArray params = new JsonArray();
            params.add(id);
            switch(body.getString("ccp_type")) {
                case "freight":
                case "courier":
                    QUERY = QUERY_GET_CCP_BY_ID;
                break;
                case "mixed":
                    // C.P con PyM y carga, como parametro recibimos el id del ccp de carga del cliente
                    QUERY = QUERY_GET_MIXED_CCP_BY_FREIGHT_ID;
                    params.add(id);
                    break;
                case "all":
                    QUERY = QUERY_GET_CCP_BY_TL_ID;
                    break;
            }
            dbClient.queryWithParams(QUERY, params, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(result));
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

    private void getManifestCCPXMLInfo(Message<JsonObject> message) {
        try {
            Integer pmId = message.body().getInteger("parcels_manifest_id");
            dbClient.queryWithParams(QUERY_GET_CCP_BY_PM_ID, new JsonArray().add(pmId), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if (result.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(result));
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

    private void registerEadRadCCP(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            this.startTransaction(message, conn -> {
                JsonObject pm_ccp = new JsonObject()
                        .put("parcels_manifest_id", body.getInteger("id"))
                        .put("invoice_status", "en proceso")
                        .put("created_by", body.getInteger("created_by"));

                String insertCCP = this.generateGenericCreate("parcels_manifest_ccp", pm_ccp);

                conn.update(insertCCP, (AsyncResult<UpdateResult> resp) -> {
                    try {
                        if(resp.failed()) {
                            throw new Exception(resp.cause());
                        }
                        Integer ccpID = resp.result().getKeys().getInteger(0);
                        String updateParcel = "UPDATE parcels_manifest SET parcels_manifest_ccp_id = ? WHERE id = ? ";
                        JsonArray updateServiceParams = new JsonArray().add(ccpID).add(body.getInteger("id"));

                        conn.queryWithParams(updateParcel, updateServiceParams, replyUpdateParcel -> {
                            try {
                                if(replyUpdateParcel.failed()) {
                                    throw new Exception(replyUpdateParcel.cause());
                                }
                                this.commit(conn,message, new JsonObject().put("parcels_manifest_ccp_id", ccpID));
                            } catch (Exception e) {
                                e.printStackTrace();
                                this.rollback(conn, e, message);
                            }
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                        this.rollback(conn,t,message);
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }
    private void getTravelLogCCPs(Message<JsonObject> message) {
        try {
            JsonObject dailyLogBody = message.body();
            JsonObject load = dailyLogBody.getJsonObject("load");

            JsonArray detailParcels = new JsonArray();
            load.getJsonArray("detail").forEach(detail -> detailParcels.addAll(((JsonObject) detail).getJsonArray("parcels")));
            JsonArray allParcels = UtilsGeneral.mergeAndSortJsonArrays(detailParcels, load.getJsonArray("parcels_transhipments"), "created_at");

            JsonArray detailPackages = new JsonArray();
            load.getJsonArray("detail").forEach(detail -> detailPackages.addAll(((JsonObject) detail).getJsonArray("packages")));
            JsonArray allPackages = UtilsGeneral.mergeAndSortJsonArrays(detailPackages, load.getJsonArray("packages_transhipments"), "created_at");

            JsonObject courier = new JsonObject().put("parcels", new JsonArray()).put("packages", new JsonArray());
            Map<String, JsonObject> freight = new HashMap<>();
            Map<Integer, Integer> courierParcelPackageCounts = new HashMap<>();
            Map<String, Map<String, Integer>> freightParcelPackageCounts = new HashMap<>();
            Map<String, Integer> huicholCounter = new HashMap<>();

            for (Object packageObj : allPackages) {
                JsonObject pkg = (JsonObject) packageObj;
                int parcelId = pkg.getInteger("parcel_id");
                boolean isOld = pkg.getBoolean("is_old", false);
                boolean invoicedAsFreight = false;

                if (pkg.getDouble("weight") == null || pkg.getDouble("weight") == 0.0) {
                    pkg.put("weight", 1.00);
                }

                JsonObject parcel = findParcelById(allParcels, parcelId);
                if (parcel == null) {
                    continue;
                }

                if(pkg.getString("pp_package_price_name") != null && !COURIER_RANGES.contains(pkg.getString("pp_package_price_name")) && !isOld) {
                    invoicedAsFreight = true;
                }

                if ((pkg.getDouble("weight") <= 31.5 || isOld) && !invoicedAsFreight) {
                    courier.put("ccp_type", "courier");
                    if (!containsParcel(courier.getJsonArray("parcels"), parcel)) {
                        courier.getJsonArray("parcels").add(parcel.copy());
                        courierParcelPackageCounts.put(parcelId, 0);
                    }
                    courierParcelPackageCounts.put(parcelId, courierParcelPackageCounts.getOrDefault(parcelId, 0) + 1);
                    courier.getJsonArray("packages").add(pkg);
                } else {
                    int customerId = parcel.getInteger("customer_id");
                    String customerName = parcel.getString("customer_name");
                    String customerNickName = parcel.getString("customer_company_nick_name");

                    JsonObject freightCustomer;
                    String keyForMaps = String.valueOf(customerId);
                    if ("HUICHOL".equals(customerNickName)) {
                        // check if there is another entry with same parcel_id, to avoid creating another
                        // if exist, use it, else, create another one
                        String existingKey = null;
                        for (Map.Entry<String, JsonObject> entry : freight.entrySet()) {
                            JsonObject existingCustomer = entry.getValue();
                            if (existingCustomer.getJsonArray("parcels").stream().anyMatch(p -> ((JsonObject) p).getInteger("parcel_id") == parcelId)) {
                                existingKey = entry.getKey();
                                break;
                            }
                        }

                        if (existingKey != null) {
                            freightCustomer = freight.get(existingKey);
                            keyForMaps = existingKey;
                        } else {
                            int count = huicholCounter.getOrDefault(keyForMaps, 0) + 1;
                            huicholCounter.put(keyForMaps, count);
                            keyForMaps += count > 1 ? "_" + count : "";
                            String modifiedName = customerName + (count > 1 ? " (" + count + ")" : "");
                            freightCustomer = freight.computeIfAbsent(keyForMaps, k -> createClienteObject(modifiedName, customerId));
                            freightCustomer.put("customer_counter", count > 1 ? count : null);
                            freightCustomer.put("specific_parcel_id", parcelId);
                        }
                    } else {
                        freightCustomer = freight.computeIfAbsent(keyForMaps, k -> createClienteObject(customerName, customerId));
                    }
                    freightCustomer.put("ccp_type", "freight");
                    if (!containsParcel(freightCustomer.getJsonArray("parcels"), parcel)) {
                        freightCustomer.getJsonArray("parcels").add(parcel.copy());
                        freightParcelPackageCounts.computeIfAbsent(keyForMaps, k -> new HashMap<>()).put(String.valueOf(parcelId), 0);
                    }
                    Map<String, Integer> customerParcelCounts = freightParcelPackageCounts.get(keyForMaps);
                    customerParcelCounts.put(String.valueOf(parcelId), customerParcelCounts.getOrDefault(String.valueOf(parcelId), 0) + 1);
                    freightCustomer.getJsonArray("packages").add(pkg);
                }
            }

            for (Object parcelObj : courier.getJsonArray("parcels")) {
                JsonObject parcel = (JsonObject) parcelObj;
                int parcelId = parcel.getInteger("parcel_id");
                parcel.put("total_packages", courierParcelPackageCounts.getOrDefault(parcelId, 0));
            }

            for (Map.Entry<String, JsonObject> entry : freight.entrySet()) {
                JsonObject freightCustomer = entry.getValue();
                Map<String, Integer> customerParcelCounts = freightParcelPackageCounts.get(entry.getKey());
                for (Object parcelObj : freightCustomer.getJsonArray("parcels")) {
                    JsonObject parcel = (JsonObject) parcelObj;
                    String parcelIdStr = String.valueOf(parcel.getInteger("parcel_id"));
                    parcel.put("total_packages", customerParcelCounts.getOrDefault(parcelIdStr, 0));
                }
            }

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            futures.add(verifyifCCPExistence(dailyLogBody.getInteger("travel_logs_id"), courier, "courier"));

            for (Map.Entry<String, JsonObject> entry : freight.entrySet()) {
                futures.add(verifyifCCPExistence(dailyLogBody.getInteger("travel_logs_id"), entry.getValue(), "freight"));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((result, error) -> {
                JsonObject data = new JsonObject();
                data.put("courier", courier);
                data.put("freight", new JsonArray(new ArrayList<>(freight.values())));
                message.reply(data);
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void updateTLStampStatus(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String QUERY = "UPDATE travel_logs SET has_stamp = 1";

            if (body.getBoolean("is_stamped") != null) {
                QUERY += ", is_stamped = 1";
            }
            QUERY += " WHERE id = ?";

            String finalQUERY = QUERY;
            this.startTransaction(message, conn -> {
                JsonArray updateServiceParams = new JsonArray().add(body.getInteger("travel_log_id"));
                conn.queryWithParams(finalQUERY, updateServiceParams, replyUpdateParcel -> {
                    try {
                        if(replyUpdateParcel.failed()) {
                            throw new Exception(replyUpdateParcel.cause());
                        }
                        this.commit(conn,message, new JsonObject().put("success", true));
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

    private CompletableFuture<Void> verifyifCCPExistence(int travelLogsId, JsonObject obj, String ccpType) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        String QUERY = QUERY_GET_STAMPED_CCP;
        JsonArray params = new JsonArray().add(travelLogsId).add(ccpType);

        if(obj.getInteger("customer_id") != null) {
            QUERY += " AND customer_id = ?";
            params.add(obj.getInteger("customer_id"));
        }

        if(obj.getInteger("customer_counter") != null) {
            QUERY += " AND customer_counter = ?";
            params.add(obj.getInteger("customer_counter"));
        } else {
            QUERY += " AND customer_counter IS NULL";
        }

        dbClient.queryWithParams(QUERY, params, res -> {
            if (res.succeeded() && !res.result().getRows().isEmpty()) {
                JsonObject row = res.result().getRows().get(0);
                obj.put("stamped", true);
                obj.put("travel_logs_ccp_id", row.getInteger("id"));
            } else {
                obj.put("stamped", false);
            }
            future.complete(null);
        });
        return future;
    }

    private static JsonObject createClienteObject(String customerName, Integer customerId) {
        JsonObject clienteObject = new JsonObject();
        clienteObject.put("customer_name", customerName);
        clienteObject.put("customer_id", customerId);
        clienteObject.put("parcels", new JsonArray());
        clienteObject.put("packages", new JsonArray());
        return clienteObject;
    }

    private JsonObject findParcelById(JsonArray parcels, int parcelId) {
        for (Object parcelObj : parcels) {
            JsonObject parcel = (JsonObject) parcelObj;
            if (parcel.getInteger("parcel_id") == parcelId) {
                return parcel;
            }
        }
        return null;
    }

    private boolean containsParcel(JsonArray parcels, JsonObject targetParcel) {
        int targetParcelId = targetParcel.getInteger("parcel_id");
        for (Object parcelObj : parcels) {
            JsonObject parcel = (JsonObject) parcelObj;
            if (parcel.getInteger("parcel_id") == targetParcelId) {
                return true;
            }
        }
        return false;
    }

    private void getRelatedUUID(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();

            if (Objects.equals(body.getJsonObject("ccp_element").getString("ccp_type"), "courier")) {
                // cuando es courier, no se necesitan revisar UUID relacionados
                message.reply(new JsonObject().put("uuids", new JsonArray()));
            } else {
                this.startTransaction(message, conn -> {
                    doGetRelatedUUID(conn, body).whenComplete((uuids, ex) -> {
                        if (ex != null) {
                            ex.printStackTrace();
                            this.rollback(conn, ex, message);
                        } else {
                            Set<String> validUUIDs = uuids.stream()
                                    .filter(uuid -> uuid != null && !uuid.isEmpty())
                                    .collect(Collectors.toSet());
                            JsonArray uuidsArray = new JsonArray(new ArrayList<>(validUUIDs));
                            this.commit(conn, message, new JsonObject().put("uuids", uuidsArray));
                        }
                    });
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    private CompletableFuture<Set<String>> doGetRelatedUUID(SQLConnection conn, JsonObject body) {
        List<CompletableFuture<Set<String>>> futures = new ArrayList<>();
        JsonArray parcels = body.getJsonObject("ccp_element").getJsonArray("parcels");

        for (Object parcelObj : parcels) {
            JsonObject parcel = (JsonObject) parcelObj;
            String trackingCode = parcel.getString("parcel_tracking_code");

            if (trackingCode != null && !trackingCode.isEmpty()) {
                if (trackingCode.startsWith("GPP")) {
                    futures.add(getUUIDForPrepaid(conn, parcel.getInteger("parcel_id")));
                } else {
                    futures.add(getUUIDForDirect(conn, parcel.getInteger("parcel_id")).thenApply(uuid -> {
                        Set<String> set = new HashSet<>();
                        if (uuid != null) {
                            set.add(uuid);
                        }
                        return set;
                    }));
                }
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .flatMap(future -> future.join().stream())
                        .collect(Collectors.toSet()));
    }


    private CompletableFuture<Set<String>> getUUIDForPrepaid(SQLConnection conn, Integer parcelId) {
        String queryParcelPrepaid = "SELECT parcel_prepaid_id FROM parcels_prepaid_detail WHERE parcel_id = ?";
        String queryTrackingCodes = "SELECT tracking_code FROM parcels_prepaid WHERE id IN (SELECT parcel_prepaid_id FROM parcels_prepaid_detail WHERE parcel_id = ?)";
        String queryUUIDs = "SELECT uuid FROM invoice WHERE uuid IS NOT NULL AND uuid != '' AND service_type = 'guia_pp' AND invoice_status = 'done' AND reference IN ";

        return query(conn, queryParcelPrepaid, new JsonArray().add(parcelId))
                .thenCompose(result -> {
                    if (result.isEmpty() || result.getJsonObject(0) == null) {
                        return CompletableFuture.completedFuture(Collections.emptySet());
                    }
                    return query(conn, queryTrackingCodes, new JsonArray().add(parcelId))
                            .thenCompose(resultTracking -> {
                                if (resultTracking.isEmpty() || resultTracking.getJsonObject(0) == null) {
                                    return CompletableFuture.completedFuture(Collections.emptySet());
                                }
                                List<String> trackingCodes = resultTracking.stream()
                                        .map(obj -> (JsonObject) obj)
                                        .map(json -> json.getString("tracking_code"))
                                        .collect(Collectors.toList());
                                if (trackingCodes.isEmpty()) {
                                    return CompletableFuture.completedFuture(Collections.emptySet());
                                }
                                String inClause = trackingCodes.stream()
                                        .map(code -> "'" + code + "'")
                                        .collect(Collectors.joining(", "));
                                String fullQueryUUIDs = queryUUIDs + "(" + inClause + ")";
                                return query(conn, fullQueryUUIDs, new JsonArray())
                                        .thenApply(resultUUIDs -> {
                                            if (resultUUIDs.isEmpty() || resultUUIDs.getJsonObject(0) == null) {
                                                return Collections.emptySet();
                                            }
                                            return resultUUIDs.stream()
                                                    .map(obj -> (JsonObject) obj)
                                                    .map(json -> json.getString("uuid"))
                                                    .filter(Objects::nonNull)
                                                    .collect(Collectors.toSet());
                                        });
                            });
                });
    }

    private CompletableFuture<String> getUUIDForDirect(SQLConnection conn, Integer parcelId) {
        String queryParcel = "SELECT invoice_id FROM parcels WHERE id = ?";
        String queryUUID = "SELECT uuid FROM invoice WHERE id = ? AND uuid IS NOT NULL AND uuid != '' AND invoice_status = 'done'";

        return query(conn, queryParcel, new JsonArray().add(parcelId))
                .thenCompose(result -> {
                    if (result == null || result.isEmpty() || result.getJsonObject(0) == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    JsonObject invoiceResult = result.getJsonObject(0);
                    Integer invoiceId = invoiceResult.getInteger("invoice_id");
                    if (invoiceId == null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return query(conn, queryUUID, new JsonArray().add(invoiceId));
                })
                .thenApply(result -> {
                    if (result == null || result.isEmpty() || result.getJsonObject(0) == null) {
                        return null;
                    }
                    JsonObject uuidResult = result.getJsonObject(0);
                    String uuid = uuidResult.getString("uuid");
                    if (uuid == null || uuid.isEmpty()) {
                        return null;
                    }
                    return uuid;
                })
                .exceptionally(ex -> {
                    return null;
                });
    }

    private CompletableFuture<JsonArray> query(SQLConnection conn, String query, JsonArray params) {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        conn.queryWithParams(query, params, res -> {
            if (res.succeeded()) {
                List<JsonObject> rows = res.result().getRows();
                if (rows == null || rows.isEmpty() || rows.stream().allMatch(JsonObject::isEmpty)) {
                    future.complete(new JsonArray());
                } else {
                    JsonArray jsonArray = new JsonArray(rows);
                    future.complete(jsonArray);
                }
            } else {
                future.completeExceptionally(res.cause());
            }
        });
        return future;
    }

    private static final String UPDATE_STATUS_CLAVE_PRODUCTO = "UPDATE c_ClaveProdServCP \n" +
            " SET  status = ? ,\n" +
            "updated_by = ?\n" +
            " WHERE Id = ?;";
    private static final String QUERY_SEARCH_ADVANCED = "SELECT \n" +
            "    clave,\n" +
            "    description,\n" +
            "    dangerous_material,\n" +
            "    similar_words\n" +
            "FROM c_ClaveProdServCP\n" +
            "WHERE status != 3 \n";

    private static final String SEARCH_TERM_FILTER = "AND CONCAT_WS(' ', similar_words, description, clave) LIKE ? \n";
    private static final String SEARCH_TERM_FILTER_NOT_EXISTS = "AND clave = '01010101' \n";
    private static final String SEARCH_ID_FILTER = "AND id = ? \n";
    private static final String SEARCH_ORDER_BY = "ORDER BY description, similar_words \n";
    private static final String GET_CONFIG_AUTOTRANSPORTE="SELECT clave,description,num_ejes,num_llantas FROM c_ConfigAutotransporte";
    private static  final String QUERY_GET_MANIFEST_DETAIL_CFDI="SELECT  p.parcel_tracking_code,pic.*,p.payment_condition,pim.tipo_cfdi  FROM \n" +
            "parcel_invoice_complement AS pic\n" +
            "INNER JOIN  parcel_invoice_manifest_details as pimd on pic.id=pimd.id_parcel_invoice_complement\n" +
            "INNER JOIN  parcel_invoice_manifest AS pim on pim.id=pimd.id_parcel_invoice_manifest\n" +
            "INNER JOIN\tparcels as p on p.id=pic.id_parcel\n" +
            "where pim.status!=4 and pic.status_cfdi=\"timbrado\"  and pim.tracking_code =?";
    private static  final String QUERY_GET_MANIFEST_DETAIL_CFDI_STATUS="SELECT pic.*,p.payment_condition,p.parcel_tracking_code FROM \n" +
            "            parcel_invoice_complement AS pic\n" +
            "            INNER JOIN  parcel_invoice_manifest_details as pimd on pic.id=pimd.id_parcel_invoice_complement\n" +
            "            INNER JOIN  parcel_invoice_manifest AS pim on pim.id=pimd.id_parcel_invoice_manifest\n" +
            "            INNER JOIN parcels as p on p.id=pic.id_parcel\n" +
            "            where  (pic.status_cfdi=4 or pic.status_cfdi=6 ) AND pim.tracking_code =? order by p.created_at desc";
    private static  final String QUERY_GET_MANIFEST_DETAIL_CFDI_STATUS_EAD="SELECT pic.*,p.payment_condition,p.parcel_tracking_code,pre.id as \"id_parcels_rad_ead\" FROM \n" +
            "            parcel_invoice_complement AS pic\n" +
            "            INNER JOIN  parcel_invoice_manifest_details as pimd on pic.id=pimd.id_parcel_invoice_complement\n" +
            "            INNER JOIN  parcel_invoice_manifest AS pim on pim.id=pimd.id_parcel_invoice_manifest\n" +
            "            INNER JOIN parcels as p on p.id=pic.id_parcel\n" +
            "            INNER JOIN parcels_rad_ead as pre on pre.parcel_id=pic.id_parcel\n" +
            "            where  (pic.status_cfdi=4 or pic.status_cfdi=6 ) AND pim.tracking_code =? order by p.created_at desc";
    private static final String UPDATE_PARCEL_INVOICE_COMPLEMENT_CADENA_ORIGINAL_XML = "UPDATE parcel_invoice_complement SET xml = ?" +
            ",system_origin=?,status_cfdi=?,updated_at=?,updated_by=? WHERE id = ?";
    private static final String UPDATE_PRINT_COUNT_MANIFEST_CCP = "UPDATE parcel_invoice_manifest SET  updated_at = ?,updated_by=?, print_count = ?,id_branchOffice=?,update_at_print=? WHERE tracking_code= ?";
    private static  final String GET_MANIFEST_NO_PRINT="select pim.tracking_code,pim.created_at \n" +
            "from parcel_invoice_manifest as pim\n" +
            "where pim.print_count=0 and pim.status=1 and pim.tipo_cfdi=\"traslado con complemento carta porte\" \n" +
            "and (SELECT COUNT(id_parcel_invoice_manifest) from parcel_invoice_manifest_details where id_parcel_invoice_manifest=pim.id )>0\n" +
            "and  pim.id_branchOffice=?  order by pim.created_at desc";

    private static final String QUERY_GET_MANIFEST_DETAIL_CFDI_SITE_PARCEL =
            "SELECT pic.*,\n" +
            "p.payment_condition,\n" +
            "p.notes,\n" +
            "p.notes_invoice,\n" +
            "c.company_nick_name as customer_company_nick_name,\n" +
            "c.parcel_notes_on_invoice,\n" +
            "cbi.address as customer_billing_address,\n" +
            "acbi.name AS addressee_name,\n" +
            "acbi.rfc AS addressee_rfc,\n" +
            "acbi.address AS addressee_address,\n" +
            "acbi.zip_code as addressee_zip_code\n" +
            "FROM parcel_invoice_complement AS pic\n" +
            "INNER JOIN parcels AS p ON p.id = pic.id_parcel\n" +
            "LEFT JOIN customer AS c ON p.customer_id = c.id\n" +
            "LEFT JOIN customer_billing_information AS cbi ON p.customer_billing_information_id = cbi.id\n" +
            "LEFT JOIN customer_billing_information AS acbi ON p.addressee_customer_billing_information_id = acbi.id\n" +
            "WHERE pic.status_cfdi = 'timbrado'\n" +
            "AND pic.tipo_cfdi = 'ingreso'\n" +
            "AND p.parcel_tracking_code = ? LIMIT 1";
    private static  final String QUERY_GET_MANIFEST_DETAIL_CFDI_SITE_BOARDINGPASS="SELECT pic.*,p.payment_condition FROM \n" +
            "            boardingpass_invoice_complement AS pic\n" +
            "            INNER JOIN boarding_pass as p on p.id=pic.id_boardingpass\n" +
            "            where pic.status_cfdi= 'timbrado' and pic.tipo_cfdi = 'ingreso'  and p.reservation_code = ?";

    private static  final String QUERY_GET_MANIFEST_DETAIL_CFDI_SITE_GUIA_PP="SELECT gic.*, p.payment_condition FROM \n" +
            "            guiapp_invoice_complement AS gic\n" +
            "            INNER JOIN parcels_prepaid as p on p.id = gic.id_prepaid\n" +
            "            where gic.status_cfdi= 'timbrado' and gic.tipo_cfdi = 'ingreso' and p.tracking_code = ? LIMIT 1";

    private static  final String QUERY_GET_MANIFEST_DETAIL_CFDI_SITE_PREPAID_BOARDINGPASS="SELECT pic.*,p.payment_condition FROM \n" +
            "            prepaid_travel_invoice_complement AS pic\n" +
            "            INNER JOIN prepaid_package_travel as p on p.id=pic.id_prepaid\n" +
            "            where pic.status_cfdi= 'timbrado' and pic.tipo_cfdi = 'ingreso'  and p.reservation_code = ?";

    private static  final String QUERY_GET_CFDI_GLOBAL_PARCEL="SELECT pic.*,p.payment_condition FROM \n" +
            "            parcel_invoice_complement AS pic\n" +
            "            INNER JOIN parcels as p on p.id=pic.id_parcel\n" +
            "            where pic.status_cfdi= \"timbrado\" and pic.tipo_cfdi = 'factura global' and p.parcel_tracking_code = ? LIMIT 1";

    private static  final String QUERY_GET_CFDI_GLOBAL_PARCEL_PREPAID = "SELECT gic.*,p.payment_condition FROM \n" +
            "            guiapp_invoice_complement AS gic\n" +
            "            INNER JOIN parcels_prepaid as p on p.id = gic.id_prepaid\n" +
            "            where gic.status_cfdi= 'timbrado' and gic.tipo_cfdi = 'factura global' and p.tracking_code = ? LIMIT 1";

    private static  final String GET_MANIFEST_CDFI_EAD="select tracking_code,created_at from parcel_invoice_manifest  where tipo_servicio=\"EAD\" and print_count=0 and status=1";

    private static final String QUERY_GET_CCP_BY_TL_ID = "SELECT * from travel_logs_ccp WHERE invoice_status = 'timbrado' AND travel_log_id = ? ORDER BY ccp_type ASC";
    private static final String QUERY_GET_CCP_BY_ID = "SELECT * from travel_logs_ccp WHERE id = ?";

    private static final String QUERY_GET_MIXED_CCP_BY_FREIGHT_ID = "SELECT * \n" +
            "FROM travel_logs_ccp \n" +
            "WHERE (id = ?\n" +
            "       OR (ccp_type = 'courier' AND travel_log_id = (\n" +
            "           SELECT travel_log_id \n" +
            "           FROM travel_logs_ccp \n" +
            "           WHERE id = ?\n" +
            "       ))\n" +
            "     )\n" +
            "AND invoice_status = 'timbrado'";

    private static final String QUERY_GET_CCP_BY_PM_ID = "SELECT * from parcels_manifest_ccp WHERE invoice_status = 'timbrado' AND parcels_manifest_id = ?";

    private static final String QUERY_GET_STAMPED_CCP = "SELECT id \n" +
            "FROM travel_logs_ccp \n" +
            "WHERE travel_log_id = ? \n" +
            "AND ccp_type = ? AND invoice_status = 'timbrado' AND xml IS NOT NULL";

}
