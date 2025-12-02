package database.invoicing;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.invoicing.handlers.parcelInvoiceDBV.Timbrado;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import database.invoicing.InvoiceDBV.ServiceTypes;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.joda.time.DateTime;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

import static database.boardingpass.BoardingPassDBV.VEHICLE_ID;
import static database.promos.CustomersPromosDBV.PROMO_ID;
import static service.commons.Constants.*;
import static service.commons.Constants.UPDATED_AT;

public class PaymentComplementDBV extends DBVerticle {
    public static final String ACTION_REGISTER_PC = "PaymentComplementDBV.registerPC";
    public static final String ACTION_HANDLE_STAMP = "PaymentComplementDBV.handleStamp";
    public static final String ACTION_UPDATE_PC_SUCCESS = "PaymentComplementDBV.updatePCSuccess";
    public static final String ACTION_UPDATE_PC_WITH_ERROR = "PaymentComplementDBV.updatePCWithError";
    public static final String ACTION_GET_PCI_XML = "PaymentComplementDBV.getPciXML";
    public static final String ACTION_GET_PC_BY_SERVICE_CODE = "PaymentComplementDBV.getPcByServiceCode";

    private static final String usuarioTimbrado = "ACA170911HY7";

    @Override
    public String getTableName() {
        return "payment_complement";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_REGISTER_PC:
                this.registerPC(message);
                break;
            case ACTION_HANDLE_STAMP:
                this.handleStamp(message);
                break;
            case ACTION_UPDATE_PC_SUCCESS:
                this.updatePCSuccess(message);
                break;
            case ACTION_UPDATE_PC_WITH_ERROR:
                this.updatePCWithError(message);
                break;
            case ACTION_GET_PCI_XML:
                this.getPciXML(message);
                break;
            case ACTION_GET_PC_BY_SERVICE_CODE:
                this.getPcByServiceCode(message);
                break;
        }
    }

    private void registerPC(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            JsonObject complement = body.getJsonObject("complement").put(CREATED_BY, body.getInteger(CREATED_BY));
            JsonArray details = body.getJsonArray("details");
            List<String> insertsDetails = new ArrayList<>();

            this.startTransaction(message, conn -> {

                conn.query(QUERY_GET_NEXT_PAYMENT_COMPLEMENT_FOLIO, (AsyncResult<ResultSet> replyFolio) -> {
                    try {
                        if(replyFolio.failed()) {
                            throw new Exception(replyFolio.cause());
                        }
                        ResultSet resultFolio = replyFolio.result();
                        JsonObject folioObj = resultFolio.getRows().get(0);
                        complement.put("folio", folioObj.getInteger("next_folio"));

                        String insertComplement = this.generateGenericCreate("payment_complement", complement);

                        conn.update(insertComplement, (AsyncResult<UpdateResult> replyComplement) -> {
                            try {
                                if(replyComplement.failed()) {
                                    throw new Exception(replyComplement.cause());
                                }

                                Integer complementID = replyComplement.result().getKeys().getInteger(0);
                                body.put("payment_complement_id", complementID);

                                for(int i = 0; i < details.size(); i++) {
                                    JsonObject detail = details.getJsonObject(i);
                                    detail.put("payment_complement_id", complementID);
                                    detail.put(CREATED_BY, body.getInteger(CREATED_BY));
                                    insertsDetails.add(this.generateGenericCreate("payment_complement_detail", detail));
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
                                this.rollback(conn,t,message);
                            }
                        });

                    }catch (Exception ex) {
                        ex.printStackTrace();
                        reportQueryError(message, ex);
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            reportQueryError(message, e);
        }
    }

    public void handleStamp(Message<JsonObject> message) {
        JsonObject body = message.body();
        String paymentComplementBase = config().getString("payment_complement_base");
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
        String documentoTimbradoName = "complemento_pago".concat(dateNowCp);
        String documentUniqueId = UUID.nameUUIDFromBytes(documentoTimbradoName.getBytes()).toString();
        String fileFinalPathBase = new File("")
                .getPath()
                .concat("./files/facturacion/")
                .concat(paymentComplementBase);
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

        actualizaXML(body, documentoTimbradoCopy).whenComplete((result, err) -> {
            try{
                startTransaction(message, conn -> {
                    try {
                        generateSello(body, documentoTimbradoCopy).whenComplete((resultSello, errorSello) ->{
                            try{
                                if(errorSello!= null){
                                    throw errorSello;
                                }
                                Path filePath = Paths.get(documentoTimbradoCopy);
                                String xmlContent = String.join("\n", Files.readAllLines(filePath, Charset.forName("UTF-8")));
                                String minifiedXmlContent = minifyXml(xmlContent);
                                String xmlBase64 = Base64.getEncoder().encodeToString(minifiedXmlContent.getBytes("UTF-8"));
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
                                        String newString = minifyXml(copyFac.replaceAll("(<\\?xml.*?\\?>)", ""));
                                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                                        DocumentBuilder db = dbf.newDocumentBuilder();
                                        InputSource is2 = new InputSource(new StringReader(newString));
                                        is2.setEncoding("UTF-8");
                                        org.w3c.dom.Document doc = db.parse(is2);
                                        doc.normalizeDocument();
                                        Element itemNode = (Element) doc.getElementsByTagName("cfdi:Comprobante").item(0);

                                        if(itemNode != null) {
                                            // Obtener el UUID
                                            NodeList timbreList = doc.getElementsByTagName("tfd:TimbreFiscalDigital");
                                            String uuid;
                                            if (timbreList != null && timbreList.getLength() > 0) {
                                                uuid = timbreList.item(0).getAttributes().getNamedItem("UUID").getNodeValue();
                                            } else {
                                                uuid = null;
                                            }

                                            TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                            Transformer transformer = transformerFactory.newTransformer();
                                            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                                            DOMSource source = new DOMSource(itemNode);
                                            StreamResult xmlResult = new StreamResult(new StringWriter());
                                            transformer.transform(source, xmlResult);

                                            String strObject = xmlResult.getWriter().toString();
                                            removeFiles(dateNowCp, documentoTimbradoName).whenComplete((resultRemove, errorRemove) ->{
                                                try{
                                                    if(errorRemove != null){
                                                        throw errorRemove;
                                                    }
                                                    this.commit(conn, message, new JsonObject()
                                                            .put("factura_timbrada", Base64.getEncoder().encodeToString(strObject.getBytes(StandardCharsets.UTF_8)))
                                                            .put("cadena_original", s)
                                                            .put("uuid", uuid)
                                                    );
                                                }catch (Throwable t) {
                                                    t.printStackTrace();
                                                    this.rollback(conn, t, message);
                                                }
                                            });
                                        } else {
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
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        this.rollback(conn, e, message);
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

    protected CompletableFuture<Boolean> actualizaXML(JsonObject body, String file){
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            FileInputStream fis = new FileInputStream(file);
            InputSource is = new InputSource(fis);
            is.setEncoding("UTF-8");
            org.w3c.dom.Document doc = db.parse(is);

            JsonObject paymentComplement = body.getJsonObject("complement");
            JsonObject complementCFDI = body.getJsonObject("complement_cfdi");
            JsonObject comprobante = complementCFDI.getJsonObject("Comprobante");
            JsonObject emisor = complementCFDI.getJsonObject("Emisor");
            JsonObject receptor = complementCFDI.getJsonObject("Receptor");
            JsonArray conceptos = complementCFDI.getJsonArray("Conceptos");
            JsonObject complemento = complementCFDI.getJsonObject("Complemento");

            //-------ACTUALIZAR NODO COMPROBANTE------
            NamedNodeMap attrComprobante = doc.getElementsByTagName("cfdi:Comprobante").item(0).getAttributes();
            Node lugarExpedicion = attrComprobante.getNamedItem("LugarExpedicion");
            lugarExpedicion.setTextContent(comprobante.getString("LugarExpedicion"));
            Node serie = attrComprobante.getNamedItem("Serie");
            serie.setTextContent(paymentComplement.getString("serie"));
            Node folio = attrComprobante.getNamedItem("Folio");
            folio.setTextContent(paymentComplement.getInteger("folio").toString());
            Node sello = attrComprobante.getNamedItem("Sello");
            sello.setTextContent("sello");
            Node fecha = attrComprobante.getNamedItem("Fecha");
            fecha.setTextContent(comprobante.getString("Fecha"));

            //-------ACTUALIZAR EMISOR------
            NamedNodeMap attrEmisor = doc.getElementsByTagName("cfdi:Emisor").item(0).getAttributes();
            Node emisorRFC = attrEmisor.getNamedItem("Rfc");
            emisorRFC.setTextContent(emisor.getString("Rfc").toUpperCase());
            Node emisorNombre = attrEmisor.getNamedItem("Nombre");
            emisorNombre.setTextContent(emisor.getString("Nombre").toUpperCase());
            Node emisorRegimenFiscal = attrEmisor.getNamedItem("RegimenFiscal");
            emisorRegimenFiscal.setTextContent(emisor.getString("RegimenFiscal"));

            //-------ACTUALIZAR RECEPTOR ----
            NamedNodeMap attrReceptor = doc.getElementsByTagName("cfdi:Receptor").item(0).getAttributes();
            Node receptorNombre = attrReceptor.getNamedItem("Nombre");
            receptorNombre.setTextContent(receptor.getString("Nombre").toUpperCase().trim());
            Node receptorRfc = attrReceptor.getNamedItem("Rfc");
            receptorRfc.setTextContent(receptor.getString("Rfc").toUpperCase());
            Node receptorUsoCFDI = attrReceptor.getNamedItem("UsoCFDI");
            receptorUsoCFDI.setTextContent(receptor.getString("UsoCFDI"));
            Node receptorRegimenFiscal = attrReceptor.getNamedItem("RegimenFiscalReceptor");
            receptorRegimenFiscal.setTextContent(receptor.getString("RegimenFiscalReceptor"));
            Node receptorDomicilio = attrReceptor.getNamedItem("DomicilioFiscalReceptor");
            receptorDomicilio.setTextContent(receptor.getString("DomicilioFiscalReceptor"));

            // ACTUALIZAR CONCEPTOS
            Element conceptosNode = (Element) doc.getElementsByTagName("cfdi:Conceptos").item(0);
            for(int i = 0; i < conceptos.size(); i++) {
                Element concepto = doc.createElement("cfdi:Concepto");
                JsonObject el = conceptos.getJsonObject(i);
                concepto.setAttribute("Cantidad", el.getString("Cantidad"));
                concepto.setAttribute("ClaveProdServ", el.getString("ClaveProdServ"));
                concepto.setAttribute("ClaveUnidad", el.getString("ClaveUnidad"));
                concepto.setAttribute("Descripcion", el.getString("Descripcion"));
                concepto.setAttribute("ValorUnitario", el.getString("ValorUnitario"));
                concepto.setAttribute("Importe", el.getString("Importe"));
                concepto.setAttribute("ObjetoImp", el.getString("ObjetoImp"));
                conceptosNode.appendChild(concepto);
            }

            // ACTUALIZAR NODO Complemento
            Element complementoNode = (Element) doc.getElementsByTagName("cfdi:Complemento").item(0);
            while (complementoNode.hasChildNodes()) {
                complementoNode.removeChild(complementoNode.getFirstChild());
            }

            if (complemento != null) {
                JsonObject pagosJson = complemento.getJsonObject("Pagos");
                if (pagosJson != null) {
                    Element pagosElement = doc.createElement("pago20:Pagos");
                    pagosElement.setAttribute("Version", pagosJson.getString("Version", ""));

                    // Nodo Totales
                    JsonObject totalesJson = pagosJson.getJsonObject("Totales");
                    if (totalesJson != null) {
                        Element totalesElement = doc.createElement("pago20:Totales");
                        totalesElement.setAttribute("TotalTrasladosBaseIVA16", totalesJson.getString("TotalTrasladosBaseIVA16", ""));
                        totalesElement.setAttribute("TotalTrasladosImpuestoIVA16", totalesJson.getString("TotalTrasladosImpuestoIVA16", ""));
                        totalesElement.setAttribute("MontoTotalPagos", totalesJson.getString("MontoTotalPagos", ""));
                        if (totalesJson.containsKey("TotalRetencionesIVA")) {
                            totalesElement.setAttribute("TotalRetencionesIVA", totalesJson.getString("TotalRetencionesIVA", ""));
                        }
                        pagosElement.appendChild(totalesElement);
                    }

                    // Nodo Pago
                    JsonObject pagoJson = pagosJson.getJsonObject("Pago");
                    if (pagoJson != null) {
                        Element pagoElement = doc.createElement("pago20:Pago");
                        pagoElement.setAttribute("FechaPago", pagoJson.getString("FechaPago", ""));
                        pagoElement.setAttribute("FormaDePagoP", pagoJson.getString("FormaDePagoP", ""));
                        pagoElement.setAttribute("MonedaP", pagoJson.getString("MonedaP", ""));
                        pagoElement.setAttribute("TipoCambioP", pagoJson.getString("TipoCambioP", ""));
                        pagoElement.setAttribute("Monto", pagoJson.getString("Monto", ""));

                        JsonArray doctoRelacionadosArray = pagoJson.getJsonArray("DoctoRelacionados");
                        if (doctoRelacionadosArray != null) {
                            for (int i = 0; i < doctoRelacionadosArray.size(); i++) {
                                JsonObject doctoJson = doctoRelacionadosArray.getJsonObject(i);
                                Element doctoElement = doc.createElement("pago20:DoctoRelacionado");
                                doctoElement.setAttribute("IdDocumento", doctoJson.getString("IdDocumento", ""));
                                doctoElement.setAttribute("MonedaDR", doctoJson.getString("MonedaDR", ""));
                                doctoElement.setAttribute("EquivalenciaDR", doctoJson.getString("EquivalenciaDR", ""));
                                doctoElement.setAttribute("NumParcialidad", doctoJson.getString("NumParcialidad", ""));
                                doctoElement.setAttribute("ImpSaldoAnt", doctoJson.getString("ImpSaldoAnt", ""));
                                doctoElement.setAttribute("ImpPagado", doctoJson.getString("ImpPagado", ""));
                                doctoElement.setAttribute("ImpSaldoInsoluto", doctoJson.getString("ImpSaldoInsoluto", ""));
                                doctoElement.setAttribute("ObjetoImpDR", doctoJson.getString("ObjetoImpDR", ""));

                                JsonObject impuestosDR = doctoJson.getJsonObject("ImpuestosDR");
                                if (impuestosDR != null) {
                                    Element impuestosDRElement = doc.createElement("pago20:ImpuestosDR");

                                    // RetencionesDR (si existe)
                                    JsonObject retencionesDR = impuestosDR.getJsonObject("RetencionesDR");
                                    if (retencionesDR != null) {
                                        Element retencionesDRElement = doc.createElement("pago20:RetencionesDR");
                                        Element retencionDRElement = doc.createElement("pago20:RetencionDR");
                                        retencionDRElement.setAttribute("BaseDR", retencionesDR.getString("BaseDR", ""));
                                        retencionDRElement.setAttribute("ImpuestoDR", retencionesDR.getString("ImpuestoDR", ""));
                                        retencionDRElement.setAttribute("TipoFactorDR", retencionesDR.getString("TipoFactorDR", ""));
                                        retencionDRElement.setAttribute("TasaOCuotaDR", retencionesDR.getString("TasaOCuotaDR", ""));
                                        retencionDRElement.setAttribute("ImporteDR", retencionesDR.getString("ImporteDR", ""));
                                        retencionesDRElement.appendChild(retencionDRElement);
                                        impuestosDRElement.appendChild(retencionesDRElement);
                                    }

                                    JsonObject trasladosDR = impuestosDR.getJsonObject("TrasladosDR");
                                    if (trasladosDR != null) {
                                        Element trasladosDRElement = doc.createElement("pago20:TrasladosDR");
                                        Element trasladoDRElement = doc.createElement("pago20:TrasladoDR");
                                        trasladoDRElement.setAttribute("BaseDR", trasladosDR.getString("BaseDR", ""));
                                        trasladoDRElement.setAttribute("ImpuestoDR", trasladosDR.getString("ImpuestoDR", ""));
                                        trasladoDRElement.setAttribute("TipoFactorDR", trasladosDR.getString("TipoFactorDR", ""));
                                        trasladoDRElement.setAttribute("TasaOCuotaDR", trasladosDR.getString("TasaOCuotaDR", ""));
                                        trasladoDRElement.setAttribute("ImporteDR", trasladosDR.getString("ImporteDR", ""));
                                        trasladosDRElement.appendChild(trasladoDRElement);
                                        impuestosDRElement.appendChild(trasladosDRElement);
                                    }
                                    doctoElement.appendChild(impuestosDRElement);
                                }
                                pagoElement.appendChild(doctoElement);
                            }
                        }

                        JsonObject impuestosP = pagoJson.getJsonObject("ImpuestosP");
                        if (impuestosP != null) {
                            Element impuestosPElement = doc.createElement("pago20:ImpuestosP");

                            // RetencionesP (si se incluye)
                            JsonObject retencionesP = impuestosP.getJsonObject("RetencionesP");
                            if (retencionesP != null) {
                                Element retencionesPElement = doc.createElement("pago20:RetencionesP");
                                Element retencionPElement = doc.createElement("pago20:RetencionP");
                                retencionPElement.setAttribute("ImpuestoP", retencionesP.getString("ImpuestoP", ""));
                                retencionPElement.setAttribute("ImporteP", retencionesP.getString("ImporteP", ""));
                                retencionesPElement.appendChild(retencionPElement);
                                impuestosPElement.appendChild(retencionesPElement);
                            }

                            JsonObject trasladosP = impuestosP.getJsonObject("TrasladosP");
                            if (trasladosP != null) {
                                Element trasladosPElement = doc.createElement("pago20:TrasladosP");
                                Element trasladoPElement = doc.createElement("pago20:TrasladoP");
                                trasladoPElement.setAttribute("BaseP", trasladosP.getString("BaseP", ""));
                                trasladoPElement.setAttribute("ImpuestoP", trasladosP.getString("ImpuestoP", ""));
                                trasladoPElement.setAttribute("TipoFactorP", trasladosP.getString("TipoFactorP", ""));
                                trasladoPElement.setAttribute("TasaOCuotaP", trasladosP.getString("TasaOCuotaP", ""));
                                trasladoPElement.setAttribute("ImporteP", trasladosP.getString("ImporteP", ""));
                                trasladosPElement.appendChild(trasladoPElement);
                                impuestosPElement.appendChild(trasladosPElement);
                            }
                            pagoElement.appendChild(impuestosPElement);
                        }

                        pagosElement.appendChild(pagoElement);
                    }
                    complementoNode.appendChild(pagosElement);
                }
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            DOMSource src = new DOMSource((Node) doc);
            StreamResult res = new StreamResult(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            transformer.transform(src, res);
            future.complete(true);
        } catch (Exception e){
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
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

    private void updatePCSuccess(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer paymentComplementId = body.getInteger("payment_complement_id");
            String xml = body.getString("xml");
            String xmlNoQuotes = xml.substring(1, xml.length() - 1);
            JsonArray invoices = body.getJsonArray("invoices");
            JsonArray paymentIds = body.getJsonArray("payment_ids");

            JsonArray paramsUpdatePC = new JsonArray()
                    .add(body.getString("uuid"))
                    .add(paymentComplementId);

            this.startTransaction(message, conn -> {
                conn.queryWithParams("UPDATE payment_complement SET invoice_status = 'done', uuid = ? WHERE id = ?", paramsUpdatePC, replyUpdatePC -> {
                    try {
                        if(replyUpdatePC.succeeded()) {
                            conn.queryWithParams("UPDATE payment_complement_detail SET status = 1 WHERE payment_complement_id = ?", new JsonArray().add(paymentComplementId), replyUpdateComplement -> {
                                try {
                                    if (replyUpdateComplement.failed()) {
                                        throw new Exception(replyUpdateComplement.cause());
                                    }

                                    JsonObject pci = new JsonObject()
                                            .put("payment_complement_id", paymentComplementId)
                                            .put("status", "done")
                                            .put("xml", xmlNoQuotes);
                                    String insertPCI = this.generateGenericCreate("payment_complement_invoice", pci);
                                    conn.update(insertPCI, (AsyncResult<UpdateResult> replyPCI) -> {
                                        try {
                                            if(replyPCI.failed()) {
                                                throw new Exception(replyPCI.cause());
                                            }

                                            JsonArray dpParams = new JsonArray();
                                            dpParams.add(paymentComplementId);
                                            StringBuilder placeholders = new StringBuilder();
                                            for (int i = 0; i < paymentIds.size(); i++) {
                                                placeholders.append("?");
                                                dpParams.add(paymentIds.getInteger(i));
                                                if (i < paymentIds.size() - 1) placeholders.append(",");
                                            }
                                            String sqlUpdateDP = "UPDATE debt_payment SET payment_complement_id = ? WHERE payment_id IN ("
                                                    + placeholders + ")";
                                            conn.queryWithParams(sqlUpdateDP, dpParams, replyUpdateDP -> {
                                                try {
                                                    if (replyUpdateDP.failed()) {
                                                        throw new Exception(replyUpdateDP.cause());
                                                    }

                                                    List<String> batchList = new ArrayList<>();
                                                    invoices.forEach(i -> {
                                                        JsonObject invoice = (JsonObject) i;
                                                        JsonObject invoiceUpdate = new JsonObject()
                                                                .put("id", invoice.getInteger("invoice_id"))
                                                                .put("available_subtotal_for_complement", invoice.getDouble("new_invoice_subtotal"))
                                                                .put("available_iva_for_complement", invoice.getDouble("new_invoice_iva"))
                                                                .put("available_iva_withheld_for_complement", invoice.getDouble("new_invoice_iva_withheld"))
                                                                .put("available_amount_for_complement", invoice.getDouble("new_debt_amount"))
                                                                .put(UPDATED_BY, body.getInteger("user_id"))
                                                                .put(UPDATED_AT, UtilsDate.sdfDataBase(new Date()));

                                                        batchList.add(this.generateGenericUpdateString("invoice", invoiceUpdate));
                                                    });

                                                    // CREAR LOS PAYMENTS COMPLEMENT PAYMENT CON LOS PAYMENT_ID
                                                    if (paymentIds != null) {
                                                        for (int i = 0; i < paymentIds.size(); i++) {
                                                            Integer pid = paymentIds.getInteger(i);
                                                            JsonObject pcp = new JsonObject()
                                                                    .put("payment_complement_id", paymentComplementId)
                                                                    .put("payment_id", pid);
                                                            batchList.add(this.generateGenericCreate("payment_complement_payment", pcp));
                                                        }
                                                    }

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
                                                    this.rollback(conn,t,message);
                                                }
                                            });
                                        } catch (Throwable t) {
                                            t.printStackTrace();
                                            this.rollback(conn,t,message);
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    this.rollback(conn, e, message);
                                }
                            });
                        } else {
                            throw new Exception(replyUpdatePC.cause());
                        }
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

    private void updatePCWithError(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer paymentComplementId = body.getInteger("payment_complement_id");

            startTransaction(message, conn -> {
                String updatePCQuery = "UPDATE payment_complement SET invoice_status = 'error' WHERE id = ?";
                conn.queryWithParams(updatePCQuery, new JsonArray().add(paymentComplementId), replyUpdatePC ->{
                    try{
                        if (replyUpdatePC.succeeded()) {
                            this.commit(conn, message, new JsonObject());
                        } else {
                            throw new Exception(replyUpdatePC.cause());
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

    private String minifyXml(String xml) {
        return xml.replaceAll(">\\s+<", "><").trim();
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

    private void getPciXML(Message<JsonObject> message) {
        try {
            Integer pcId = message.body().getInteger("payment_complement_id");
            dbClient.queryWithParams(QUERY_GET_PCI_BY_PC_ID, new JsonArray().add(pcId), reply -> {
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

    private void getPcByServiceCode(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String QUERY = QUERY_GET_PC_BY_SERVICE_CODE;
            switch(body.getString("service_type")) {
                case "parcel":
                    QUERY = QUERY.replace("{table_name}", body.getString("tabla_name"))
                            .replace("{tracking_code}", "parcel_tracking_code");
                    break;
                case "guia_pp":
                    QUERY = QUERY.replace("{table_name}", body.getString("tabla_name"))
                            .replace("{tracking_code}", "tracking_code");
                    break;
            }
            dbClient.queryWithParams(QUERY, new JsonArray().add(body.getString("service_code")), reply -> {
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

    private static final String QUERY_GET_PCI_BY_PC_ID = "SELECT * from payment_complement_invoice WHERE status = 'done' AND payment_complement_id = ? limit 1";
    private static final String QUERY_GET_NEXT_PAYMENT_COMPLEMENT_FOLIO = "SELECT COALESCE(MAX(folio), 0) + 1 AS next_folio FROM payment_complement where invoice_status = 'done'";

    private static final String QUERY_GET_PC_BY_SERVICE_CODE = "SELECT \n" +
            " pc.*,\n" +
            " pcd.invoice_id\n" +
            "FROM {table_name} p\n" +
            " INNER JOIN payment_complement_detail pcd ON p.invoice_id = pcd.invoice_id AND pcd.status = 1\n" +
            " INNER JOIN payment_complement pc ON pc.id = pcd.payment_complement_id\n" +
            "WHERE p.{tracking_code} = ?";
}
