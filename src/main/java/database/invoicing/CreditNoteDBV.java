package database.invoicing;

import database.commons.DBVerticle;
import database.invoicing.handlers.parcelInvoiceDBV.Timbrado;
import database.invoicing.xml.CfdiXmlUpdater;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTime;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import utils.UtilsDate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import static service.commons.Constants.*;

public class CreditNoteDBV extends DBVerticle {
    public static final String ACTION_REGISTER_CN = "CreditNoteDBV.registerCN";
    public static final String ACTION_HANDLE_STAMP = "CreditNoteDBV.handleStamp";
    public static final String ACTION_UPDATE_CN_SUCCESS = "CreditNoteDBV.updateCNSuccess";
    public static final String ACTION_UPDATE_CN_WITH_ERROR = "CreditNoteDBV.updateCNWithError";
    public static final String ACTION_GET_CNI_XML = "CreditNoteDBV.getCniXML";
    private static final String usuarioTimbrado = "ACA170911HY7";

    @Override
    public String getTableName() {
        return "credit_note";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
    }

    @Override
    public void onMessage(Message<JsonObject> message) {
        String action = message.headers().get(ACTION);
        try {
            switch (action) {
                case ACTION_REGISTER_CN:
                    this.registerCN(message);
                    break;
                case ACTION_HANDLE_STAMP:
                    this.handleStamp(message);
                    break;
                case ACTION_UPDATE_CN_SUCCESS:
                    this.updateCNSuccess(message);
                    break;
                case ACTION_UPDATE_CN_WITH_ERROR:
                    this.updateCNWithError(message);
                    break;
                case ACTION_GET_CNI_XML:
                    this.getCniXML(message);
                    break;
                default:
                    message.reply(new JsonObject().put("error", "Acción no soportada: " + action));
            }
        } catch (Throwable t) {
            reportQueryError(message, t);
        }
    }

    private void registerCN(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonObject cn = body.getJsonObject("credit_note").put(CREATED_BY, body.getInteger(CREATED_BY));
            JsonArray invoices = body.getJsonArray("invoices");
            List<String> insertsDetails = new ArrayList<>();

            this.startTransaction(message, conn -> {
                conn.query(QUERY_GET_NEXT_CREDIT_NOTE_FOLIO, (AsyncResult<ResultSet> folioReply) -> {
                    try {
                        if (folioReply.failed()) throw folioReply.cause();

                        Integer nextFolio = folioReply.result().getRows().get(0).getInteger("next_folio");
                        cn.put("folio", nextFolio);
                        cn.put("invoice_status", "pending");
                        cn.put("issue_date", UtilsDate.sdfDataBase(new Date()));

                        String insertCN = this.generateGenericCreate("credit_note", cn);

                        conn.update(insertCN, (AsyncResult<UpdateResult> insReply) -> {
                            try {
                                if (insReply.failed()) {
                                    throw new Exception(insReply.cause());
                                }

                                Integer creditNoteId = insReply.result().getKeys().getInteger(0);
                                body.put("credit_note_id", creditNoteId);
                                body.put("folio", nextFolio);

                                for(int i = 0; i < invoices.size(); i++) {
                                    JsonObject detail = invoices.getJsonObject(i);
                                    detail.put("credit_note_id", creditNoteId);
                                    detail.put(CREATED_BY, body.getInteger(CREATED_BY));
                                    insertsDetails.add(this.generateGenericCreate("credit_note_detail", detail));
                                }

                                conn.batch(insertsDetails, (AsyncResult<List<Integer>> replyComplementDetails) -> {
                                    try {
                                        if (replyComplementDetails.failed()){
                                            throw new Exception(replyComplementDetails.cause());
                                        }

                                        this.commit(conn,message, body);
                                    } catch (Exception t) {
                                        t.printStackTrace();
                                        this.rollback(conn,t,message);
                                    }
                                });

                            } catch (Throwable t) {
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                        reportQueryError(message, t);
                    }
                });
            });
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    public void handleStamp(Message<JsonObject> message) {
        JsonObject body = message.body();
        String documentoIngresoTimbrado = config().getString("invoice_income_parcel");
        String dateNowCp = DateTime.now().toString();
        String cadenaOriginalName = "cadena_original40".concat(dateNowCp);
        String uniqueId = UUID.nameUUIDFromBytes(cadenaOriginalName.getBytes()).toString();
        String cadenaOriginalPath = new File("")
                .getPath()
                .concat("./files/facturacion/")
                .concat("cadena_original40.txt");
        String cadenaOriginalCopy = new File("")
                .getPath()
                .concat("./files/facturacion/procesarFacturas/")
                .concat(uniqueId)
                .concat(".txt");
        try {
            Files.copy((new File(cadenaOriginalPath)).toPath(), (new File(cadenaOriginalCopy)).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String documentoTimbradoName = "factura_ingreso".concat(dateNowCp);
        String documentUniqueId = UUID.nameUUIDFromBytes(documentoTimbradoName.getBytes()).toString();
        String fileFinalPathBase = new File("")
                .getPath()
                .concat("./files/facturacion/")
                .concat(documentoIngresoTimbrado);
        String documentoTimbradoCopy = new File("")
                .getPath()
                .concat("./files/facturacion/procesarFacturas/")
                .concat(documentUniqueId)
                .concat(".xml");
        try {
            Files.copy((new File(fileFinalPathBase)).toPath(), (new File(documentoTimbradoCopy)).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(fileFinalPathBase), StandardCharsets.UTF_8);
             OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(new File("")
                     .getPath()
                     .concat("./files/facturacion/procesarFacturas/")
                     .concat(documentUniqueId)
                     .concat(".xml")), StandardCharsets.UTF_8)) {
            char[] buffer = new char[1024];
            int length;
            while ((length = isr.read(buffer)) > 0) {
                osw.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        CfdiXmlUpdater.updateXML(body, documentoTimbradoCopy, "egreso").whenComplete( (result, err) -> {
            try{
                startTransaction(message, conn -> {
                    try {
                        generarSello(body,documentoTimbradoCopy, dateNowCp).whenComplete( (resultSello, errorSello) ->{
                            try{
                                if(errorSello!= null){
                                    throw errorSello;
                                }
                                Path filePath = Paths.get(documentoTimbradoCopy);
                                String xmlContent = String.join("\n", Files.readAllLines(filePath, Charset.forName("UTF-8")));
                                // String minifiedXmlContent = minifyXml(xmlContent); // MINIFIED XML
                                // String xmlBase64 = Base64.getEncoder().encodeToString(minifiedXmlContent.getBytes("UTF-8")); // MINIFIED XML
                                String xmlBase64 = Base64.getEncoder().encodeToString(xmlContent.getBytes(StandardCharsets.UTF_8));
                                try {
                                    String cadenaOriginal40txt = new File("files/facturacion/cadena_original40.txt").getCanonicalPath();
                                    byte[] cadenaOriginal = Files.readAllBytes(Paths.get(cadenaOriginal40txt));
                                    String s = new String(cadenaOriginal, StandardCharsets.UTF_8);
                                    // Creacion del objeto Timbrado
                                    Timbrado tim = new Timbrado(usuarioTimbrado, config().getString("timbox_pwd"), xmlBase64, config());

                                    try {
                                        String resultadoFactura = tim.Timbrar();
                                        System.out.print(resultadoFactura);
                                        String copyFac = resultadoFactura.replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                                                .replaceAll("&apos;", "'").replaceAll("&quot;", "\"")
                                                .replaceAll("&amp;", "&");
                                        //String newString = minifyXml(copyFac.replaceAll("(<\\?xml.*?\\?>)", "")); // MINIFIED XML
                                        String newString = copyFac.replaceAll("(<\\?xml.*?\\?>)", "");
                                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                                        DocumentBuilder db = dbf.newDocumentBuilder();
                                        InputSource is2 = new InputSource(new StringReader(newString));
                                        is2.setEncoding("UTF-8");
                                        org.w3c.dom.Document doc = db.parse(is2);
                                        doc.normalizeDocument();
                                        Element itemNode = (Element) doc.getElementsByTagName("cfdi:Comprobante").item(0);

                                        if(itemNode != null){
                                            String uuid = CfdiXmlUpdater.extractAttributeFromXML(doc, "tfd:TimbreFiscalDigital", "UUID");
                                            String serie = CfdiXmlUpdater.extractAttributeFromXML(doc, "cfdi:Comprobante", "Serie");
                                            String folio = CfdiXmlUpdater.extractAttributeFromXML(doc, "cfdi:Comprobante", "Folio");

                                            TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                            Transformer transformer = transformerFactory.newTransformer();
                                            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                                            DOMSource source = new DOMSource(itemNode);
                                            StreamResult xmlResult = new StreamResult(new StringWriter());
                                            transformer.transform(source, xmlResult);

                                            String strObject = xmlResult.getWriter().toString();
                                            removeFiles(dateNowCp, documentoTimbradoName).whenComplete( (resultRemove, errorRemove) ->{
                                                try{
                                                    if(errorRemove != null){
                                                        throw errorRemove;
                                                    }
                                                    this.commit(conn, message, new JsonObject()
                                                            .put("factura_timbrada", Base64.getEncoder().encodeToString(strObject.getBytes(StandardCharsets.UTF_8)))
                                                            .put("cadena_original", s)
                                                            .put("uuid", uuid)
                                                            .put("serie", serie)
                                                            .put("folio", folio)
                                                    );
                                                }catch (Throwable t) {
                                                    t.printStackTrace();
                                                    this.rollback(conn, t, message);
                                                }
                                            });

                                        }else{
                                            removeFiles(dateNowCp, documentoTimbradoName).whenComplete( (resultRemove, errorRemove) ->{
                                                try{
                                                    if(errorRemove != null){
                                                        throw errorRemove;
                                                    }
                                                    this.commit(conn, message, new JsonObject()
                                                            .put("factura_timbrada", Base64.getEncoder().encodeToString(resultadoFactura.getBytes(StandardCharsets.UTF_8)))
                                                            .put("cadena_original", s)
                                                            .put("error", resultadoFactura)
                                                    );
                                                }catch (Throwable t) {
                                                    t.printStackTrace();
                                                    this.rollback(conn, t, message);
                                                }
                                            });

                                        }
                                    } catch (Throwable t) {
                                        t.printStackTrace();
                                        this.rollback(conn, t, message);
                                    }
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    this.rollback(conn, t, message);
                                }
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
            } catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }

    protected CompletableFuture<Boolean> generarSello(JsonObject cfdi, String xmlFile, String dateNowCp){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            File inputFile = new File(xmlFile);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);
            JsonObject body = cfdi.getJsonObject("cfdi_body");
            String lugarExpedicion = body.getString("LugarExpedicion");
            String postalCode = searchCsvLine(1, lugarExpedicion);

            Calendar calendar = new GregorianCalendar();
            TimeZone tz = TimeZone.getTimeZone(postalCode.split(",")[6]);
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            //Obtener zona horaria de Lugar Expedicion
            formatter.setTimeZone(TimeZone.getTimeZone(tz.getID()));
            //Obtener la fecha actual y remplazarla en el atributo fecha
            String dateNow = formatter.format(calendar.getTime());
            String timeStamp = dateNow.replace('_', 'T');

//            OffsetDateTime odt = OffsetDateTime.parse(body.getString("Fecha"));
//            ZoneId expeditionZone = ZoneId.of(postalCode.split(",")[6]);
//            ZonedDateTime expeditionDateTime = odt.atZoneSameInstant(expeditionZone).minusMinutes(1);
//            String timeStamp = expeditionDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

            document.getRootElement().setAttribute("Fecha", timeStamp);
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            xmlOutputter.output(document, new FileOutputStream(xmlFile));

            //Actualizar sello en el comprobante(xml)
            String sello = createSello(xmlFile);
            document.getRootElement().setAttribute("Sello", sello);
            xmlOutputter.output(document, new FileOutputStream(xmlFile));
            future.complete(true);
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private void updateCNSuccess(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer creditNoteId = body.getInteger("credit_note_id");
            String xml = body.getString("xml");
            JsonArray invoices = body.getJsonArray("invoices");
            JsonArray services = body.getJsonArray("services");
            String xmlNoQuotes = xml != null && xml.length() > 1 ? xml.substring(1, xml.length() - 1) : xml;

            this.startTransaction(message, conn -> {

                JsonArray paramsUpdate = new JsonArray()
                        .add(body.getString("uuid"))
                        .add(creditNoteId);

                conn.queryWithParams("UPDATE credit_note SET invoice_status = 'done', uuid = ? WHERE id = ?", paramsUpdate, replyUpdateCN -> {
                    try {
                        if(replyUpdateCN.succeeded()) {
                            conn.queryWithParams("UPDATE credit_note_detail SET status = 1 WHERE credit_note_id = ?",
                                    new JsonArray().add(creditNoteId), replyUpdateDetails -> {
                                        JsonObject cni = new JsonObject()
                                                .put("credit_note_id", creditNoteId)
                                                .put("status", "done")
                                                .put("xml", xmlNoQuotes);
                                        String insertInvoice = this.generateGenericCreate("credit_note_invoice", cni);

                                        conn.update(insertInvoice, (AsyncResult<UpdateResult> replyInsertInvoice) -> {
                                            try {
                                                if(replyInsertInvoice.failed()) {
                                                    throw new Exception(replyInsertInvoice.cause());
                                                }

                                                List<String> batchList = new ArrayList<>();
                                                invoices.forEach(i -> {
                                                    JsonObject invoice = (JsonObject) i;
                                                    JsonObject invoiceUpdate = new JsonObject()
                                                            .put("id", invoice.getInteger("invoice_id"))
                                                            .put("available_subtotal_for_complement", invoice.getDouble("new_invoice_subtotal"))
                                                            .put("available_iva_for_complement", invoice.getDouble("new_invoice_iva"))
                                                            .put("available_iva_withheld_for_complement", invoice.getDouble("new_invoice_iva_withheld"))
                                                            .put("available_amount_for_complement", invoice.getDouble("new_invoice_total_amount"))
                                                            .put(UPDATED_BY, body.getInteger("user_id"))
                                                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

                                                    batchList.add(this.generateGenericUpdateString("invoice", invoiceUpdate));
                                                });

                                                // UPDATE SERVICE DEBTS
                                                services.forEach(s -> {
                                                    JsonObject service = (JsonObject) s;
                                                    InvoiceDBV.ServiceTypes serviceType = InvoiceDBV.ServiceTypes.getTypeByServiceName(service.getString("service_type"));
                                                    Double prevDebt = service.getDouble("p_debt");
                                                    Double usedAmount = service.getJsonObject("__tax").getDouble("total");
                                                    Double newDebt = prevDebt - usedAmount;
                                                    JsonObject serviceUpdate = new JsonObject()
                                                            .put("id", service.getInteger("p_id"))
                                                            .put("debt", newDebt)
                                                            .put(UPDATED_BY, body.getInteger("user_id"))
                                                            .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));
                                                    batchList.add(this.generateGenericUpdateString(serviceType.getTable(), serviceUpdate));

                                                    // agregar servicios abonados en la NC
                                                    JsonObject cnService = new JsonObject()
                                                            .put("credit_note_id", creditNoteId)
                                                            .put("prev_debt", prevDebt)
                                                            .put("new_debt", newDebt)
                                                            .put("created_by", body.getInteger("user_id"))
                                                            .put("created_at", UtilsDate.sdfDataBase(new Date()));

                                                    switch(serviceType.getType()) {
                                                        case "parcel":
                                                            cnService.put("parcel_id", service.getInteger("p_id"));
                                                            break;
                                                        case "guia_pp":
                                                            cnService.put("parcel_prepaid_id", service.getInteger("p_id"));
                                                            break;
                                                    }
                                                    batchList.add(this.generateGenericCreate("credit_note_services", cnService));
                                                });

                                                conn.batch(batchList, replyUpdateInvoices -> {
                                                    try {
                                                        if(replyUpdateInvoices.failed()){
                                                            throw replyUpdateInvoices.cause();
                                                        }
                                                        this.commit(conn,message, body);
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
                            });
                        } else {
                            throw new Exception(replyUpdateCN.cause());
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            });
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void updateCNWithError(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer creditNoteId = body.getInteger("credit_note_id");

            startTransaction(message, conn -> {
                String update = "UPDATE credit_note SET invoice_status = 'error' WHERE id = ?";
                conn.queryWithParams(update, new JsonArray().add(creditNoteId), rep -> {
                    try {
                        if (rep.failed()) throw rep.cause();
                        this.commit(conn, message, new JsonObject());
                    } catch (Throwable t) {
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });
            });
        } catch (Throwable t) {
            t.printStackTrace();
            reportQueryError(message, t);
        }
    }

    private void getCniXML(Message<JsonObject> message) {
        try {
            Integer cnId = message.body().getInteger("credit_note_id");
            dbClient.queryWithParams(QUERY_GET_CNI_BY_CN_ID, new JsonArray().add(cnId), reply -> {
                try {
                    if (reply.failed()) throw reply.cause();
                    List<JsonObject> rows = reply.result().getRows();
                    message.reply(new JsonArray(rows));
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

    private String minifyXml(String xml) {
        return xml.replaceAll(">\\s+<", "><").trim();
    }

    protected CompletableFuture<Boolean> generateSello(JsonObject body, String xmlFile) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            File inputFile = new File(xmlFile);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);
            String lugarExpedicion = body.getJsonObject("complement_cfdi").getJsonObject("Comprobante").getString("LugarExpedicion");
            String postalCode = searchCsvLine(1, lugarExpedicion);
            Calendar calendar = new GregorianCalendar();
            TimeZone tz = TimeZone.getTimeZone(postalCode.split(",")[6]);
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            //Obtener zona horaria de Lugar Expedicion
            formatter.setTimeZone(TimeZone.getTimeZone(tz.getID()));
            //Obtener la fecha actual y remplazarla en el atributo fecha
            String dateNow = formatter.format(calendar.getTime());
            String timeStamp = dateNow.replace('_', 'T');
            document.getRootElement().setAttribute("Fecha", timeStamp);
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            xmlOutputter.output(document, new FileOutputStream(xmlFile));

            //Actualizar sello en el comprobante(xml)
            String sello = createSello(xmlFile);
            document.getRootElement().setAttribute("Sello", sello);
            xmlOutputter.output(document, new FileOutputStream(xmlFile));
            future.complete(true);
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private static String searchCsvLine(int searchColumnIndex, String searchString) throws IOException {
        String resultRow = null;
        String catPostal = new File("")
                .getPath()
                .concat("./files/facturacion/")
                .concat("cat_postal_code.csv");
        BufferedReader br = new BufferedReader(new FileReader(catPostal));
        String line;
        while ( (line = br.readLine()) != null ) {
            String[] values = line.split(",");
            if(values[searchColumnIndex].equals(searchString)) {
                resultRow = line;
                break;
            }
        }
        br.close();
        return resultRow;
    }

    private CompletableFuture<Boolean> removeFiles(String dateNowCp, String documentName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try{
            String documentUniqueId = UUID.nameUUIDFromBytes(documentName.getBytes()).toString();

            String documentoTimbradoCopy = new File("")
                    .getPath()
                    .concat("./files/facturacion/procesarFacturas/")
                    .concat(documentUniqueId)
                    .concat(".xml");
            String cadenaOriginalName = "cadena_original".concat(dateNowCp);
            String uniqueId = UUID.nameUUIDFromBytes(cadenaOriginalName.getBytes()).toString();

            String cadenaOriginalCopy = new File("")
                    .getPath()
                    .concat("./files/facturacion/procesarFacturas/")
                    .concat(uniqueId)
                    .concat(".txt");

            String cadenaOriginalCp = "cadena_original_xslt".concat(dateNowCp);
            String cademaOriginalUnique = UUID.nameUUIDFromBytes(cadenaOriginalCp.getBytes()).toString();

            String cadenaOriginalNewFile = new File("")
                    .getPath()
                    .concat("./files/facturacion/procesarFacturas/")
                    .concat(cademaOriginalUnique)
                    .concat(".xslt");
            String cadenaOriginalTxtCp = "cadena_original_txt".concat(dateNowCp);
            String cademaOriginalTxtUnique = UUID.nameUUIDFromBytes(cadenaOriginalTxtCp.getBytes()).toString();

            String cadenaOriginalTxtNewFile = new File("")
                    .getPath()
                    .concat("./files/facturacion/procesarFacturas/")
                    .concat(cademaOriginalTxtUnique)
                    .concat(".txt");
            String digestCp = "digest".concat(dateNowCp);
            String digestUnique = UUID.nameUUIDFromBytes(digestCp.getBytes()).toString();

            String digestNewFile = new File("")
                    .getPath()
                    .concat("./files/facturacion/procesarFacturas/")
                    .concat(digestUnique)
                    .concat(".txt");
            String selloCp = "sello".concat(dateNowCp);
            String selloUnique = UUID.nameUUIDFromBytes(selloCp.getBytes()).toString();

            String selloNewFile = new File("")
                    .getPath()
                    .concat("./files/facturacion/procesarFacturas/")
                    .concat(selloUnique)
                    .concat(".txt");

            List<File> filesRemove = new ArrayList();
            filesRemove.add((new File(cadenaOriginalCopy)));
            filesRemove.add((new File(documentoTimbradoCopy)));
            filesRemove.add((new File(cadenaOriginalNewFile)));
            filesRemove.add((new File(cadenaOriginalTxtNewFile)));
            filesRemove.add((new File(digestNewFile)));
            filesRemove.add((new File(selloNewFile)));
            for(File file : filesRemove) {
                try {
                    Files.deleteIfExists(file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            future.complete(true);
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private String createSello(String xmlFile) {
        String sello = "";
        try {
            String path = "files/facturacion/";
            String keyPem = new File(path + config().getString("parcel_keypem_path")).toString();

            // archivos necesarios para sellado
            String digest40 = new File(path + "digest40.txt").toString();
            String cadenaOriginal40 = new File(path + "cadena_original40.txt").toString();
            String sello40 = new File(path + "sello40.txt").toString();

            //Crear la cadena original
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(new File(path + "cadenaoriginal_40.xslt"));
            Transformer transformer = factory.newTransformer(xslt);
            Source text = new StreamSource(new File(xmlFile));
            transformer.transform(text, new StreamResult(new File(path +"cadena_original40.txt")));

            //Crear digestion
            String cmd = "openssl dgst -sha256 -sign " + keyPem +" -out "+ digest40 + " " + cadenaOriginal40;
            Process p;
            Runtime r = Runtime.getRuntime();
            p = r.exec(cmd);

            p.waitFor();
            //Generar Sello
            cmd = "openssl enc -in " + digest40 + " -out " + sello40 + " -base64 -A -K " + keyPem;
            p = r.exec(cmd);
            p.waitFor();

            sello = new Scanner(new File(path + "sello40.txt")).useDelimiter("\\Z").next();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return sello;
    }

    private static final String QUERY_GET_CNI_BY_CN_ID =
            "SELECT * FROM credit_note_invoice WHERE status = 'done' AND credit_note_id = ? LIMIT 1";

    private static final String QUERY_GET_NEXT_CREDIT_NOTE_FOLIO =
            "SELECT COALESCE(MAX(folio), 0) + 1 AS next_folio FROM credit_note";
}