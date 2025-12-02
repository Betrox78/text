package database.invoicing.handlers.parcelInvoiceDBV;

import database.commons.DBHandler;
import database.invoicing.InvoiceDBV;
import database.invoicing.xml.CfdiXmlUpdater;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
import utils.UtilsGeneral;
import utils.UtilsID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static service.commons.Constants.CCP_PREFIX;
import static service.commons.Constants.CCP_VERSION;


public class ParcelInvoice extends DBHandler<InvoiceDBV> {
    private JsonObject config = new JsonObject();
    public static final String ACTION = "InvoiceDBV.RegisterParcelInvoice";
    public static final String ACTION_INGRESO = "InvoiceDBV.RegisterParcelIngreso";
    public static final String ACTION_CCP = "InvoiceDBV.RegisterCCP";
    public static final String  ACTION_INGRESO_GLOBAL = "InvoiceDBV.RegisterParcelGlobal";
    private static final String usuarioTimbrado = "ACA170911HY7";
    private static final String documentoTimbrado = "factura_traslado_3.xml";

    public ParcelInvoice(InvoiceDBV dbVerticle) {
        super(dbVerticle);
    }

    public ParcelInvoice(InvoiceDBV dbVerticle, JsonObject config) {
        super(dbVerticle);
        this.config = config;
    }

    private String createSello(String serviceType, String xmlFile) {
        String sello = "";
        try {
            String path = "files/facturacion/";
            String keyPem;
            if (serviceType.toUpperCase().equals("BOARDING_PASS") || serviceType.toUpperCase().equals("PREPAID_BOARDING_PASS") || serviceType.toUpperCase().equals("RENTAL")) {
                keyPem = new File(path + config.getString("boardingpass_keypem_path")).toString();
            } else {
                keyPem = new File(path + config.getString("parcel_keypem_path")).toString();
            }
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

    @Override
    public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        String dateNowCp = DateTime.now().toString();
        String cadenaOriginalName = "cadena_original".concat(dateNowCp);
        String uniqueId = UUID.nameUUIDFromBytes(cadenaOriginalName.getBytes()).toString();
        String cadenaOriginalPath = new File("")
                .getPath()
                .concat("./files/facturacion/")
                .concat("cadena_original.txt");
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
        String documentoTimbradoName = "factura_traslado".concat(dateNowCp);
        String documentUniqueId = UUID.nameUUIDFromBytes(documentoTimbradoName.getBytes()).toString();
        String fileFinalPathBase = new File("")
                .getPath()
                .concat("./files/facturacion/")
                .concat(body.getJsonObject("Complementos").isEmpty() ? "factura_traslado_ultima_milla.xml": documentoTimbrado);
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
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(fileFinalPathBase);
            os = new FileOutputStream(new File("")
                    .getPath()
                    .concat("./files/facturacion/procesarFacturas/")
                    .concat(documentUniqueId)
                    .concat(".xml"));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        actualizarXML(body, documentoTimbradoCopy).whenComplete( (result, err) -> {
           try{
               startTransaction(message, conn -> {
                   try {
                       generarSelloAsync(body,documentoTimbradoCopy, dateNowCp).whenComplete( (resultSello, errorSello) ->{
                          try{
                              if(errorSello!= null){
                                  throw errorSello;
                              }
                              byte[] archivoXml = Files.readAllBytes(Paths.get(documentoTimbradoCopy));
                              String xmlBase64 = Base64.getEncoder().encodeToString(archivoXml);
                              try {
                                  byte[] cadenaOriginal = Files.readAllBytes(Paths.get(cadenaOriginalCopy));
                                  String s = new String(cadenaOriginal, StandardCharsets.UTF_8);

                                  // Creacion del objeto Timbrado
                                  Timbrado tim = new Timbrado(usuarioTimbrado, config.getString("timbox_pwd"), xmlBase64, config);
                                  //Ejecucion del servicio
                                  //String facturaTimbrada = tim.Timbrar();
                                  this.getVertx().setTimer(2000, r -> {
                                      try {
                                          String resultadoFactura = tim.Timbrar();
                                          System.out.println("Comprobante timbrado: \n");
                                          System.out.print(resultadoFactura);
                                          String copyFac = resultadoFactura.replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                                                  .replaceAll("&apos;", "'").replaceAll("&quot;", "\"")
                                                  .replaceAll("&amp;", "&");
                                          //String replaceXmlTag = copyFac.replaceAll("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
                                          String newString = copyFac.replaceAll("(<\\?xml.*?\\?>)","");
                                          //commitTransaction(message, conn, new JsonObject().put("Factura Timbrada", facturaTimbrada));
                                          DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                                          DocumentBuilder db = dbf.newDocumentBuilder();
                                          org.w3c.dom.Document doc = db.parse(new InputSource(new StringReader(newString)));
                                          doc.normalizeDocument();
                                          Element itemNode = (Element) doc.getElementsByTagName("cfdi:Comprobante").item(0);

                                          if(itemNode != null){

                                              TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                              Transformer transformer = transformerFactory.newTransformer();
                                              DOMSource source = new DOMSource(itemNode);
                                              StreamResult xmlResult = new StreamResult(new StringWriter());
                                              transformer.transform(source, xmlResult);

                                              String strObject = xmlResult.getWriter().toString();
                                              removeFIles(dateNowCp).whenComplete( (resultRemove, errorRemove) ->{
                                                  try{
                                                      if(errorRemove != null){
                                                          throw errorRemove;
                                                      }
                                                      commitTransaction(message, conn, new JsonObject()
                                                              .put("factura_timbrada", Base64.getEncoder().encodeToString(strObject.getBytes(StandardCharsets.UTF_8)))
                                                              .put("cadena_original", s)
                                                      );
                                                  }catch (Throwable t) {
                                                      t.printStackTrace();
                                                      rollbackTransaction(message, conn, t);
                                                  }
                                              });

                                          }else{
                                              removeFIles(dateNowCp).whenComplete( (resultRemove, errorRemove) ->{
                                                  try{
                                                      if(errorRemove != null){
                                                          throw errorRemove;
                                                      }
                                                      commitTransaction(message, conn, new JsonObject()
                                                              .put("factura_timbrada", Base64.getEncoder().encodeToString(resultadoFactura.getBytes(StandardCharsets.UTF_8)))
                                                              .put("cadena_original", s)
                                                              .put("error", resultadoFactura)
                                                      );
                                                  }catch (Throwable t) {
                                                      t.printStackTrace();
                                                      rollbackTransaction(message, conn, t);
                                                  }
                                              });

                                          }

                                      } catch (Exception e) {
                                          e.printStackTrace();
                                          rollbackTransaction(message, conn, e);
                                      }
                                  });

                              } catch (Throwable t) {
                                  t.printStackTrace();
                                  rollbackTransaction(message, conn, t);
                              }
                          } catch (Throwable t) {
                              t.printStackTrace();
                              rollbackTransaction(message, conn, t);
                          }
                       });
                       //generar_sello(body,documentoTimbradoCopy);

                   } catch (Throwable t) {
                       t.printStackTrace();
                       rollbackTransaction(message, conn, t);
                   }
               });
           } catch (Exception ex){
               ex.printStackTrace();
           }
        });

        //---------------------------------------------------------------------------------------------

    }

    public void handleIngreso(Message<JsonObject> message) {
        JsonObject body = message.body();
        String documentoIngresoTimbrado = config.getString("invoice_income_parcel");
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

        CfdiXmlUpdater.updateXML(body, documentoTimbradoCopy, "ingreso").whenComplete( (result, err) -> {
            try{
                startTransaction(message, conn -> {
                    try {
                        generarSelloAsyncIngreso(body,documentoTimbradoCopy, dateNowCp).whenComplete( (resultSello, errorSello) ->{
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
                                    Timbrado tim = new Timbrado(usuarioTimbrado, config.getString("timbox_pwd"), xmlBase64, config);

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
                                            removeIngresoFIles(dateNowCp).whenComplete( (resultRemove, errorRemove) ->{
                                                try{
                                                    if(errorRemove != null){
                                                        throw errorRemove;
                                                    }
                                                    commitTransaction(message, conn, new JsonObject()
                                                            .put("factura_timbrada", Base64.getEncoder().encodeToString(strObject.getBytes(StandardCharsets.UTF_8)))
                                                            .put("cadena_original", s)
                                                            .put("uuid", uuid)
                                                            .put("serie", serie)
                                                            .put("folio", folio)
                                                    );
                                                }catch (Throwable t) {
                                                    t.printStackTrace();
                                                    rollbackTransaction(message, conn, t);
                                                }
                                            });

                                        }else{
                                            removeIngresoFIles(dateNowCp).whenComplete( (resultRemove, errorRemove) ->{
                                                try{
                                                    if(errorRemove != null){
                                                        throw errorRemove;
                                                    }
                                                    commitTransaction(message, conn, new JsonObject()
                                                            .put("factura_timbrada", Base64.getEncoder().encodeToString(resultadoFactura.getBytes(StandardCharsets.UTF_8)))
                                                            .put("cadena_original", s)
                                                            .put("error", resultadoFactura)
                                                    );
                                                }catch (Throwable t) {
                                                    t.printStackTrace();
                                                    rollbackTransaction(message, conn, t);
                                                }
                                            });

                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        rollbackTransaction(message, conn, e);
                                    }
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    rollbackTransaction(message, conn, t);
                                }
                            } catch (Throwable t) {
                                t.printStackTrace();
                                rollbackTransaction(message, conn, t);
                            }
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                        rollbackTransaction(message, conn, t);
                    }
                });
            } catch (Exception ex){
                ex.printStackTrace();
            }
        });
    }

    public void handleCCP(Message<JsonObject> message) {
        try {
             JsonObject params = message.body();
             JsonObject body = null;
             switch(params.getString("type")) {
                 case "travel_log":
                     body = this.generateComplementBody(params);
                     break;
                 case "EAD/RAD":
                     body = this.generateComplementEadBody(params);
                     break;
             }
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
            String documentoTimbradoName = "factura_traslado".concat(dateNowCp);
            String documentUniqueId = UUID.nameUUIDFromBytes(documentoTimbradoName.getBytes()).toString();
            String fileFinalPathBase = new File("")
                    .getPath()
                    .concat("./files/facturacion/")
                    .concat(body.getJsonObject("Complementos").isEmpty() ? config.getString("invoice_last_mile_base") : config.getString("invoice_ccp_base"));
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

            JsonObject finalBody = body;
            actualizarXML(body, documentoTimbradoCopy).whenComplete( (result, err) -> {
                try{
                    startTransaction(message, conn -> {
                        try {
                            generarSelloAsync(finalBody,documentoTimbradoCopy, dateNowCp).whenComplete( (resultSello, errorSello) ->{
                                try{
                                    if(errorSello!= null){
                                        throw errorSello;
                                    }
                                    Path filePath = Paths.get(documentoTimbradoCopy);
                                    String xmlContent = String.join("\n", Files.readAllLines(filePath, Charset.forName("UTF-8")));
                                    String xmlBase64 = Base64.getEncoder().encodeToString(xmlContent.getBytes("UTF-8"));
                                    try {
                                        String cadenaOriginal40txt = new File("files/facturacion/cadena_original40.txt").getCanonicalPath();
                                        byte[] cadenaOriginal = Files.readAllBytes(Paths.get(cadenaOriginal40txt));
                                        String s = new String(cadenaOriginal, StandardCharsets.UTF_8);
                                        Timbrado tim = new Timbrado(usuarioTimbrado, config.getString("timbox_pwd"), xmlBase64, config);

                                        try {
                                            String resultadoFactura = tim.Timbrar();
                                            System.out.println("Comprobante timbrado: \n");
                                            System.out.print(resultadoFactura);
                                            String copyFac = resultadoFactura.replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                                                    .replaceAll("&apos;", "'").replaceAll("&quot;", "\"")
                                                    .replaceAll("&amp;", "&");
                                            String newString = copyFac.replaceAll("(<\\?xml.*?\\?>)","");
                                            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                                            DocumentBuilder db = dbf.newDocumentBuilder();
                                            org.w3c.dom.Document doc = db.parse(new InputSource(new StringReader(newString)));
                                            doc.normalizeDocument();
                                            Element itemNode = (Element) doc.getElementsByTagName("cfdi:Comprobante").item(0);

                                            if(itemNode != null){
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
                                                removeFIles(dateNowCp).whenComplete( (resultRemove, errorRemove) ->{
                                                    try{
                                                        if(errorRemove != null){
                                                            throw errorRemove;
                                                        }
                                                        commitTransaction(message, conn, new JsonObject()
                                                                .put("factura_timbrada", Base64.getEncoder().encodeToString(strObject.getBytes(StandardCharsets.UTF_8)))
                                                                .put("cadena_original", s)
                                                                .put("uuid", uuid)
                                                        );
                                                    }catch (Throwable t) {
                                                        t.printStackTrace();
                                                        rollbackTransaction(message, conn, t);
                                                    }
                                                });

                                            }else{
                                                removeFIles(dateNowCp).whenComplete( (resultRemove, errorRemove) ->{
                                                    try{
                                                        if(errorRemove != null){
                                                            throw errorRemove;
                                                        }
                                                        commitTransaction(message, conn, new JsonObject()
                                                                .put("factura_timbrada", Base64.getEncoder().encodeToString(resultadoFactura.getBytes(StandardCharsets.UTF_8)))
                                                                .put("cadena_original", s)
                                                                .put("error", resultadoFactura)
                                                                .put("body_stamp", finalBody)
                                                                .put("xml_to_stamp", xmlBase64)
                                                        );
                                                    }catch (Throwable t) {
                                                        t.printStackTrace();
                                                        rollbackTransaction(message, conn, t);
                                                    }
                                                });

                                            }

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            rollbackTransaction(message, conn, e);
                                        }
                                    } catch (Throwable t) {
                                        t.printStackTrace();
                                        rollbackTransaction(message, conn, t);
                                    }
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    rollbackTransaction(message, conn, t);
                                }
                            });
                        } catch (Throwable t) {
                            t.printStackTrace();
                            rollbackTransaction(message, conn, t);
                        }
                    });
                } catch (Exception ex){
                    ex.printStackTrace();
                }
            });
        } catch(Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private String minifyXml(String xml) {
        return xml.replaceAll(">\\s+<", "><").trim();
    }

    protected CompletableFuture<Boolean> generarSelloAsync(JsonObject body, String xmlFile, String dateNowCp){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            File inputFile = new File(xmlFile);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);
            String lugarExpedicion = body.getString("LugarExpedicion");
            String postalCode = searchCsvLine(1, lugarExpedicion);
            Calendar calendar = new GregorianCalendar();
            TimeZone tz = TimeZone.getTimeZone(postalCode.split(",")[6]);
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            //Obtener zona horaria de Lugar Expedicion
            formatter.setTimeZone(TimeZone.getTimeZone(tz.getID()));
            // Restar 1 hora al tiempo actual
            calendar.add(Calendar.HOUR_OF_DAY, -1);

            //Obtener la fecha actual y remplazarla en el atributo fecha
            String dateNow = formatter.format(calendar.getTime());
            String timeStamp = dateNow.replace('_', 'T');
            document.getRootElement().setAttribute("Fecha", timeStamp);
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            xmlOutputter.output(document, new FileOutputStream(xmlFile));

            //Actualizar sello en el comprobante(xml)
            String sello = createSelloCCP(xmlFile);
            document.getRootElement().setAttribute("Sello", sello);
            xmlOutputter.output(document, new FileOutputStream(xmlFile));
            future.complete(true);
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }


    public static String searchCsvLine(int searchColumnIndex, String searchString) throws IOException, FileNotFoundException {
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

    protected CompletableFuture<Boolean> actualizarXML(JsonObject body, String file){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        JsonObject emisor = body.getJsonObject("Emisor");
        JsonObject receptor = body.getJsonObject("Receptor");
        JsonArray conceptos = body.getJsonArray("Conceptos");
        JsonObject complemento = body.getJsonObject("Complementos");
        JsonObject cartaPorte = !complemento.isEmpty() ? complemento.getJsonObject("CartaPorte") : null;
        JsonArray ubicaciones = !complemento.isEmpty() ? cartaPorte.getJsonArray("Ubicaciones") : null;
        JsonObject mercancias = !complemento.isEmpty()  ? cartaPorte.getJsonObject("Mercancias") : null;
        JsonObject autotransporte = !complemento.isEmpty()  ? cartaPorte.getJsonObject("Autotransporte") : null;
        JsonArray figuras = !complemento.isEmpty() ? cartaPorte.getJsonArray("FiguraTransporte") : null;
        JsonObject cfdiRelacionados = body.getJsonObject("CfdiRelacionados");
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            FileInputStream fis = new FileInputStream(file);
            InputSource is = new InputSource(fis);
            is.setEncoding("UTF-8");
            org.w3c.dom.Document doc = db.parse(is);

            //-------ACTUALIZAR BASE--------
            Node comprobante = doc.getElementsByTagName("cfdi:Comprobante").item(0);
            Node emisorNode = doc.getElementsByTagName("cfdi:Emisor").item(0);
            Node receptorNode = doc.getElementsByTagName("cfdi:Receptor").item(0);

            // ACTUALIZAR CONCEPTOS
            Element itemNode = (Element) doc.getElementsByTagName("cfdi:Conceptos").item(0);
            for(int i = 0; i<conceptos.size(); i++) {
                Element concepto = doc.createElement("cfdi:Concepto");
                JsonObject el = conceptos.getJsonObject(i);
                concepto.setAttribute("Cantidad", el.getString("Cantidad"));
                concepto.setAttribute("ClaveProdServ", el.getString("ClaveProdServ"));
                concepto.setAttribute("ClaveUnidad", el.getString("ClaveUnidad"));
                concepto.setAttribute("Descripcion", el.getString("Descripcion"));
                concepto.setAttribute("Importe", el.getString("Importe"));
                concepto.setAttribute("NoIdentificacion", el.getString("NoIdentificacion"));
                concepto.setAttribute("Unidad", el.getString("Unidad"));
                concepto.setAttribute("ValorUnitario", el.getString("ValorUnitario"));
                concepto.setAttribute("ObjetoImp", "01");
                itemNode.appendChild(concepto);
            }

            NamedNodeMap attrEmisor = emisorNode.getAttributes();
            NamedNodeMap attrReceptor = receptorNode.getAttributes();
            NamedNodeMap attr = comprobante.getAttributes();

            //-------ACTUALIZAR DATOS GENERALES------
            Node lugarExpedicion = attr.getNamedItem("LugarExpedicion");
            lugarExpedicion.setTextContent(body.getString("LugarExpedicion"));
            Node total = attr.getNamedItem("Total");
            total.setTextContent(body.getString("Total"));
            Node moneda = attr.getNamedItem("Moneda");
            moneda.setTextContent(body.getString("Moneda"));
            Node subTotal = attr.getNamedItem("SubTotal");
            subTotal.setTextContent(body.getString("Subtotal"));
            Node tipoDeComprobante = attr.getNamedItem("TipoDeComprobante");
            tipoDeComprobante.setTextContent(body.getString("TipoDeComprobante"));

            //----- CFDI RELACIONADOS
            if(cfdiRelacionados != null && !cfdiRelacionados.isEmpty()) {
                JsonArray cfdiRelacionadosArray = cfdiRelacionados.getJsonArray("relacionados");
                if(!cfdiRelacionadosArray.isEmpty()) {
                    Element cfdiRelacionadosE = doc.createElement("cfdi:CfdiRelacionados");
                    cfdiRelacionadosE.setAttribute("TipoRelacion", "05");
                    for(int i = 0; i < cfdiRelacionadosArray.size(); i++) {
                        Element cfdiRelacionado = doc.createElement("cfdi:CfdiRelacionado");
                        JsonObject el = cfdiRelacionadosArray.getJsonObject(i);
                        cfdiRelacionado.setAttribute("UUID", el.getString("UUID"));
                        cfdiRelacionadosE.appendChild(cfdiRelacionado);
                    }
                    comprobante.insertBefore(cfdiRelacionadosE, emisorNode);
                }
            }

            //-------ACTUALIZAR EMISOR------
            Node emisorRFC = attrEmisor.getNamedItem("Rfc");
            emisorRFC.setTextContent(emisor.getString("Rfc").toUpperCase());
            Node emisorNombre = attrEmisor.getNamedItem("Nombre");
            emisorNombre.setTextContent(emisor.getString("Nombre").toUpperCase());
            Node emisorRegimenFiscal = attrEmisor.getNamedItem("RegimenFiscal");
            emisorRegimenFiscal.setTextContent(emisor.getString("RegimenFiscal"));

            //-------ACTUALIZAR RECEPTOR ----
            Node receptorNombre = attrReceptor.getNamedItem("Nombre");
            receptorNombre.setTextContent(receptor.getString("Nombre").toUpperCase());
            Node receptorRfc = attrReceptor.getNamedItem("Rfc");
            receptorRfc.setTextContent(receptor.getString("Rfc").toUpperCase());
            Node receptorUsoCFDI = attrReceptor.getNamedItem("UsoCFDI");
            receptorUsoCFDI.setTextContent(receptor.getString("UsoCFDI"));
            Node receptorRegimenFiscal = attrReceptor.getNamedItem("RegimenFiscalReceptor");
            receptorRegimenFiscal.setTextContent(receptor.getString("RegimenFiscalReceptor"));
            Node receptorDomicilio = attrReceptor.getNamedItem("DomicilioFiscalReceptor");
            receptorDomicilio.setTextContent(body.getString("LugarExpedicion"));

            //----EL SELLO SE GENERA DE FORMA DINAMICA DESDE EL PROCESO ----
            Node sello = attr.getNamedItem("Sello");
            sello.setTextContent("sello");

            if(!complemento.isEmpty()){
                //-----ACTUALIZAR CARTA PORTE----------------
                Element itemCartaPorte = (Element) doc.getElementsByTagName(CCP_PREFIX + ":CartaPorte").item(0);
                itemCartaPorte.setAttribute("Version", CCP_VERSION);
                itemCartaPorte.setAttribute("IdCCP", UtilsID.generateIdCCP());
                itemCartaPorte.setAttribute("TranspInternac", "No");
                itemCartaPorte.setAttribute("TotalDistRec", cartaPorte.getString("TotalDistRec"));

                // ACTUALIZAR UBICACIONES
                Element itemUbicaciones = (Element) doc.getElementsByTagName(CCP_PREFIX + ":Ubicaciones").item(0);

                for(int i = 0; i < ubicaciones.size(); i++){
                    Element ubicacion = doc.createElement(CCP_PREFIX + ":Ubicacion");
                    JsonObject el = ubicaciones.getJsonObject(i);
                    if(el.getString("DistanciaRecorrida") != null){
                        ubicacion.setAttribute("DistanciaRecorrida", el.getString("DistanciaRecorrida"));
                    }
                    ubicacion.setAttribute("FechaHoraSalidaLlegada", el.getString("FechaHoraSalidaLlegada"));
                    ubicacion.setAttribute("IDUbicacion", el.getString("IDUbicacion"));
                    ubicacion.setAttribute("RFCRemitenteDestinatario", el.getString("RFCRemitenteDestinatario"));
                    ubicacion.setAttribute("TipoUbicacion", el.getString("TipoUbicacion"));
                    // DOMICILIO
                    Element domicilio = doc.createElement(CCP_PREFIX + ":Domicilio");
                    domicilio.setAttribute("CodigoPostal", el.getJsonObject("Domicilio").getString("CodigoPostal"));
                    //domicilio.setAttribute("Colonia", el.getJsonObject("Domicilio").getString("Colonia"));
                    domicilio.setAttribute("Estado", el.getJsonObject("Domicilio").getString("Estado"));
                    //domicilio.setAttribute("Localidad", el.getJsonObject("Domicilio").getString("Localidad"));
                    //domicilio.setAttribute("Municipio", el.getJsonObject("Domicilio").getString("Municipio"));
                    domicilio.setAttribute("Pais", el.getJsonObject("Domicilio").getString("Pais"));
                    ubicacion.appendChild(domicilio);
                    itemUbicaciones.appendChild(ubicacion);
                }

                //-------------------------ACTUALIZAR MERCANCIAS------------------
                Element itemMercancias = (Element) doc.getElementsByTagName(CCP_PREFIX + ":Mercancias").item(0);
                itemMercancias.setAttribute("NumTotalMercancias", mercancias.getString("NumTotalMercancias"));
                itemMercancias.setAttribute("PesoBrutoTotal", mercancias.getString("PesoBrutoTotal"));
                itemMercancias.setAttribute("UnidadPeso", mercancias.getString("UnidadPeso"));
                JsonArray mercanciaArray = mercancias.getJsonArray("Mercancia");
                for(int i = 0; i<mercanciaArray.size();i++) {
                    Element mercancia = doc.createElement(CCP_PREFIX + ":Mercancia");
                    JsonObject el = mercanciaArray.getJsonObject(i);
                    mercancia.setAttribute("BienesTransp", el.getString("BienesTransp"));
                    mercancia.setAttribute("Cantidad", el.getString("Cantidad"));
                    if(el.getString("MaterialPeligroso") != null) {
                        mercancia.setAttribute("MaterialPeligroso", el.getString("MaterialPeligroso"));
                    }
                    mercancia.setAttribute("ClaveUnidad", el.getString("ClaveUnidad"));
                    mercancia.setAttribute("Descripcion", el.getString("Descripcion"));
                    mercancia.setAttribute("PesoEnKg", el.getString("PesoEnKg"));
                    // mercancia.setAttribute("Unidad", el.getString("Unidad")); // es necesario?
                    // GUIAS IDENTIFICACION
                    Element guiasIdentificacion = doc.createElement(CCP_PREFIX + ":GuiasIdentificacion");
                    guiasIdentificacion.setAttribute("NumeroGuiaIdentificacion", el.getJsonObject("GuiasIdentificacion").getString("NumeroGuiaIdentificacion"));
                    guiasIdentificacion.setAttribute("DescripGuiaIdentificacion", el.getJsonObject("GuiasIdentificacion").getString("DescripGuiaIdentificacion"));
                    guiasIdentificacion.setAttribute("PesoGuiaIdentificacion", el.getJsonObject("GuiasIdentificacion").getString("PesoGuiaIdentificacion"));
                    mercancia.appendChild(guiasIdentificacion);
                    // CANTIDAD TRANSPORTA
                    Element cantidadTransporta = doc.createElement(CCP_PREFIX + ":CantidadTransporta");
                    cantidadTransporta.setAttribute("Cantidad", el.getJsonObject("CantidadTransporta").getString("Cantidad"));
                    cantidadTransporta.setAttribute("IDDestino", el.getJsonObject("CantidadTransporta").getString("IDDestino"));
                    cantidadTransporta.setAttribute("IDOrigen", el.getJsonObject("CantidadTransporta").getString("IDOrigen"));
                    mercancia.appendChild(cantidadTransporta);
                    itemMercancias.appendChild(mercancia);
                }

                //-------------ACTUALIZAR AUTOTRANSPORTE------------------
                JsonObject identificacionVehicularJson = cartaPorte.getJsonObject("Autotransporte").getJsonObject("IdentificacionVehicular");
                JsonObject seguros = cartaPorte.getJsonObject("Autotransporte").getJsonObject("Seguros");
                JsonArray remolquesArray = cartaPorte.getJsonObject("Autotransporte").getJsonArray("Remolques");

                Element autotransporteItem = doc.createElement(CCP_PREFIX + ":Autotransporte");
                autotransporteItem.setAttribute("NumPermisoSCT", autotransporte.getString("NumPermisoSCT"));
                autotransporteItem.setAttribute("PermSCT", autotransporte.getString("PermSCT"));

                // IDENTIFICACION VEHICULAR
                Element identificacionVehicular = doc.createElement(CCP_PREFIX + ":IdentificacionVehicular");
                identificacionVehicular.setAttribute("AnioModeloVM", identificacionVehicularJson.getString("AnioModeloVM"));
                identificacionVehicular.setAttribute("ConfigVehicular", identificacionVehicularJson.getString("ConfigVehicular"));
                identificacionVehicular.setAttribute("PlacaVM", identificacionVehicularJson.getString("PlacaVM"));
                identificacionVehicular.setAttribute("PesoBrutoVehicular", identificacionVehicularJson.getString("PesoBrutoVehicular"));

                Element seguro = doc.createElement(CCP_PREFIX + ":Seguros");
                seguro.setAttribute("AseguraRespCivil", seguros.getString("AseguraRespCivil"));
                seguro.setAttribute("PolizaRespCivil", seguros.getString("PolizaRespCivil"));
                autotransporteItem.appendChild(identificacionVehicular);
                autotransporteItem.appendChild(seguro);

                // REMOLQUES
                if(remolquesArray != null) {
                    Element remolques = doc.createElement(CCP_PREFIX + ":Remolques");
                    for(int i = 0; i < remolquesArray.size(); i++) {
                        Element remolque = doc.createElement(CCP_PREFIX + ":Remolque");
                        JsonObject el = remolquesArray.getJsonObject(i);
                        remolque.setAttribute("Placa", el.getString("Placa"));
                        remolque.setAttribute("SubTipoRem", el.getString("SubTipoRem"));
                        remolques.appendChild(remolque);
                    }
                    autotransporteItem.appendChild(remolques);
                }

                itemMercancias.appendChild(autotransporteItem);

                //--------------- FIGURA TRANSPORTE-----------------------
                Element figuraTransporte = (Element) doc.getElementsByTagName(CCP_PREFIX + ":FiguraTransporte").item(0);
                for(int i = 0; i < figuras.size(); i++){
                    Element tiposFigura = doc.createElement(CCP_PREFIX + ":TiposFigura");
                    JsonObject el = figuras.getJsonObject(i);
                    tiposFigura.setAttribute("NombreFigura", el.getString("NombreFigura"));
                    tiposFigura.setAttribute("NumLicencia", el.getString("NumLicencia"));
                    tiposFigura.setAttribute("RFCFigura", el.getString("RFCFigura"));
                    tiposFigura.setAttribute("TipoFigura", el.getString("TipoFigura"));

                    // DOMICILIO
                    /*
                    Element domicilio = doc.createElement(CCP_PREFIX + ":Domicilio");
                    domicilio.setAttribute("Localidad", el.getJsonObject("Domicilio").getString("Localidad"));
                    domicilio.setAttribute("Municipio", el.getJsonObject("Domicilio").getString("Municipio"));
                    domicilio.setAttribute("Estado", el.getJsonObject("Domicilio").getString("Estado"));
                    domicilio.setAttribute("Pais", el.getJsonObject("Domicilio").getString("Pais"));
                    domicilio.setAttribute("CodigoPostal", el.getJsonObject("Domicilio").getString("CodigoPostal"));
                    tiposFigura.appendChild(domicilio);
                     */
                    figuraTransporte.appendChild(tiposFigura);
                }
            }

            // ---- escribir en el archivo
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            DOMSource src = new DOMSource((Node) doc);
            StreamResult res = new StreamResult(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            transformer.transform(src, res);
            future.complete(true);
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    private CompletableFuture<Boolean> removeFIles(String dateNowCp) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try{
            String documentoTimbradoName = "factura_traslado".concat(dateNowCp);
            String documentUniqueId = UUID.nameUUIDFromBytes(documentoTimbradoName.getBytes()).toString();

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
                    //future.completeExceptionally(e);
                }
            }
            future.complete(true);
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<Boolean> removeIngresoFIles(String dateNowCp) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try{
            String documentoTimbradoName = "factura_ingreso".concat(dateNowCp);
            String documentUniqueId = UUID.nameUUIDFromBytes(documentoTimbradoName.getBytes()).toString();

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
                    //future.completeExceptionally(e);
                }
            }
            future.complete(true);
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    protected CompletableFuture<Boolean> generarSelloAsyncIngreso(JsonObject cfdi, String xmlFile, String dateNowCp){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String type = cfdi.getJsonObject("cfdi_body").getJsonObject("invoice").getString("type");

            File inputFile = new File(xmlFile);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);
            JsonObject body = cfdi.getJsonObject("cfdi_body");
            String lugarExpedicion = body.getString("LugarExpedicion");
            String postalCode = searchCsvLine(1, lugarExpedicion);

            OffsetDateTime odt = OffsetDateTime.parse(body.getString("Fecha"));
            ZoneId expeditionZone = ZoneId.of(postalCode.split(",")[6]);
            // ZonedDateTime expeditionDateTime = odt.atZoneSameInstant(expeditionZone).minusMinutes(1);
            ZonedDateTime expeditionDateTime = odt.atZoneSameInstant(expeditionZone).minusHours(1);
            String timeStamp = expeditionDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

            document.getRootElement().setAttribute("Fecha", timeStamp);
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            xmlOutputter.output(document, new FileOutputStream(xmlFile));

            //Actualizar sello en el comprobante(xml)
            String sello = createSello(type, xmlFile);
            document.getRootElement().setAttribute("Sello", sello);
            xmlOutputter.output(document, new FileOutputStream(xmlFile));
            future.complete(true);
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }

    protected CompletableFuture<Boolean> generarSelloAsyncIngresoGlobal(JsonObject cfdi, String xmlFile, String dateNowCp){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            String type = cfdi.getJsonObject("cfdi_body").getJsonObject("invoice").getString("service_type");
            File inputFile = new File(xmlFile);
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(inputFile);
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

            JsonObject body = cfdi.getJsonObject("cfdi_body");
            LocalDateTime invoiceDate = LocalDateTime.parse(body.getString("Fecha"));
            String lugarExpedicion = body.getString("LugarExpedicion");
            String postalCode = searchCsvLine(1, lugarExpedicion);
            Calendar calendar = new GregorianCalendar();
            calendar.set(Calendar.DAY_OF_MONTH, invoiceDate.getDayOfMonth());
            calendar.set(Calendar.MONTH, invoiceDate.getMonthValue() - 1);
            calendar.set(Calendar.YEAR, invoiceDate.getYear());
            TimeZone tz = TimeZone.getTimeZone(postalCode.split(",")[6]);
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            //Obtener zona horaria de Lugar Expedicion
            formatter.setTimeZone(TimeZone.getTimeZone(tz.getID()));

            // Restar 1 hora antes de formatear
            calendar.add(Calendar.HOUR_OF_DAY, -1);

            //Obtener la fecha actual y remplazarla en el atributo fecha
            String dateNow = formatter.format(calendar.getTime());
            String timeStamp = dateNow.replace('_', 'T');
            document.getRootElement().setAttribute("Fecha", timeStamp);
            xmlOutputter.output(document, new FileOutputStream(xmlFile));


            //Actualizar sello en el comprobante(xml)
            String sello = createSello(type, xmlFile);
            document.getRootElement().setAttribute("Sello", sello);
            xmlOutputter.output(document, new FileOutputStream(xmlFile));
            future.complete(true);
        } catch (Exception e){
            future.completeExceptionally(e);
        }
        return future;
    }
    public void handleIngresoGlobal(Message<JsonObject> message) {
        JsonObject body = message.body();
        String typeService = body.getJsonObject("cfdi_body").getJsonObject("invoice").getString("service_type");
        String documentoIngresoTimbrado = config.getString("invoice_income_parcel");
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
        CfdiXmlUpdater.updateXML(body, documentoTimbradoCopy, "global").whenComplete( (result, err) -> {
            try{
                startTransaction(message, conn -> {
                    try {
                        generarSelloAsyncIngresoGlobal(body,documentoTimbradoCopy, dateNowCp).whenComplete( (resultSello, errorSello) ->{
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
                                    Timbrado tim = new Timbrado(usuarioTimbrado, config.getString("timbox_pwd"), xmlBase64, config);

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
                                            removeIngresoFIles(dateNowCp).whenComplete( (resultRemove, errorRemove) ->{
                                                try{
                                                    if(errorRemove != null){
                                                        throw errorRemove;
                                                    }
                                                    commitTransaction(message, conn, new JsonObject()
                                                            .put("factura_timbrada", Base64.getEncoder().encodeToString(strObject.getBytes(StandardCharsets.UTF_8)))
                                                            .put("cadena_original40", s)
                                                            .put("uuid", uuid)
                                                            .put("serie", serie)
                                                            .put("folio", folio)
                                                    );
                                                }catch (Throwable t) {
                                                    t.printStackTrace();
                                                    rollbackTransaction(message, conn, t);
                                                }
                                            });

                                        } else {
                                            removeIngresoFIles(dateNowCp).whenComplete( (resultRemove, errorRemove) ->{
                                                try{
                                                    if(errorRemove != null){
                                                        throw errorRemove;
                                                    }
                                                    commitTransaction(message, conn, new JsonObject()
                                                            .put("factura_timbrada", Base64.getEncoder().encodeToString(resultadoFactura.getBytes(StandardCharsets.UTF_8)))
                                                            .put("cadena_original40", s)
                                                            .put("error", resultadoFactura)
                                                    );
                                                } catch (Throwable t) {
                                                    t.printStackTrace();
                                                    rollbackTransaction(message, conn, t);
                                                }
                                            });

                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        rollbackTransaction(message, conn, e);
                                    }
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    rollbackTransaction(message, conn, t);
                                }
                            } catch (Throwable t) {
                                t.printStackTrace();
                                rollbackTransaction(message, conn, t);
                            }
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                        rollbackTransaction(message, conn, t);
                    }
                });
            } catch (Exception ex){
                ex.printStackTrace();
            }
        });

        //---------------------------------------------------------------------------------------------

    }

    private String createSelloCCP(String xmlFile) {
        String sello = "";
        try {
            String path = "files/facturacion/";
            String keyPem = new File(path + config.getString("parcel_keypem_path")).toString();

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

    public JsonObject generateComplementEadBody(JsonObject params) {
        JsonObject body = new JsonObject();
        // Datos por defecto
        // Emisor
        JsonObject emisor = new JsonObject()
                .put("Rfc", config.getString("rfc_parcel"))
                .put("Nombre", config.getString("rfc_name_parcel"))
                .put("RegimenFiscal", "601");
        // Datos por defecto
        // Receptor
        JsonObject receptor = new JsonObject()
                .put("Nombre", config.getString("parcel_destination_name"))
                .put("Rfc", config.getString("parcel_destination_rfc"))
                .put("UsoCFDI", "S01")
                .put("RegimenFiscalReceptor", "601");

        // Concepto
        JsonArray conceptos = new JsonArray();

        params.getJsonArray("packages").forEach(e -> {
            JsonObject pkg = (JsonObject) e;
            JsonObject concepto = new JsonObject()
                    .put("ClaveProdServ", "31181701")
                    .put("NoIdentificacion", pkg.getString("parcel_tracking_code"))
                    .put("Cantidad", pkg.getInteger("total_packages").toString())
                    .put("ClaveUnidad", "XPK")
                    .put("Unidad", "Paquete")
                    .put("Descripcion", "EMPAQUES")
                    .put("ValorUnitario", "0")
                    .put("Importe", "0")
                    .put("ObjetoImp", "01");
            conceptos.add(concepto);
        });

        // Agregar al objeto principal
        body.put("Emisor", emisor);
        body.put("Receptor", receptor);
        body.put("Conceptos", conceptos);
        body.put("Complementos", new JsonObject());

        // Otros atributos del comprobante
        body.put("LugarExpedicion", "81200")
                .put("Total", "0")
                .put("Moneda", "XXX")
                .put("Subtotal", "0")
                .put("TipoDeComprobante", "T");

        return body;
    }

    public JsonObject generateComplementBody(JsonObject dailyLogBody) {
        JsonObject body = new JsonObject();
        JsonObject ccpElement = dailyLogBody.getJsonObject("ccp_element");
        try {
            // Emisor
            JsonObject emisor = new JsonObject()
                    .put("Rfc", config.getString("rfc_parcel"))
                    .put("Nombre", config.getString("rfc_name_parcel"))
                    .put("RegimenFiscal", "601");
            // Receptor
            JsonObject receptor = new JsonObject()
                    .put("Rfc", config.getString("rfc_parcel"))
                    .put("Nombre", config.getString("rfc_name_parcel"))
                    .put("UsoCFDI", "S01")
                    .put("RegimenFiscalReceptor", "601");

            // CFDI relacionados
            JsonArray uuids = dailyLogBody.getJsonArray("uuids");
            if(!uuids.isEmpty()) {
                JsonObject cfdiRelacionados = new JsonObject()
                        .put("TipoRelacion", "05")
                        .put("relacionados", new JsonArray());
                uuids.forEach(uuid -> {
                    cfdiRelacionados.getJsonArray("relacionados").add(new JsonObject().put("UUID", uuid));
                });
                body.put("CfdiRelacionados", cfdiRelacionados);
            }

            // Carta Porte
            JsonObject cartaPorte = new JsonObject()
                    .put("TotalDistRec", dailyLogBody.getDouble("distance").toString())
                    .put("Version", "1")
                    .put("IdCCP", "")
                    .put("TranspInternac", "NO");

            // Ubicaciones
            JsonArray ubicaciones = new JsonArray();
            JsonObject ubicacionOrigen = new JsonObject()
                    .put("TipoUbicacion", "Origen")
                    .put("IDUbicacion", "OR000001") // Este tiene que salir del catalogo
                    .put("RFCRemitenteDestinatario", config.getString("parcel_destination_rfc"))
                    .put("FechaHoraSalidaLlegada", removeLastCharOptional(dailyLogBody.getString("departed_at")))
                    .put("Domicilio", new JsonObject()
                            //.put("Colonia", dailyLogBody.getString("colOrigin"))
                            //.put("Localidad", dailyLogBody.getString("localidadOrigin"))
                            //.put("Municipio", dailyLogBody.getString("municipioOrigin"))
                            .put("Estado", dailyLogBody.getString("estadoOrigin"))
                            .put("Pais", "MEX")
                            .put("CodigoPostal", dailyLogBody.getInteger("zipCodeOrigin").toString()));
            JsonObject ubicacionDestino = new JsonObject()
                    .put("TipoUbicacion", "Destino")
                    .put("IDUbicacion", "DE000002") // Este sale del catalogo
                    .put("RFCRemitenteDestinatario", config.getString("parcel_destination_rfc"))
                    .put("FechaHoraSalidaLlegada", removeLastCharOptional(dailyLogBody.getString("departed_at")))
                    .put("DistanciaRecorrida", dailyLogBody.getDouble("distance").toString())
                    .put("Domicilio", new JsonObject()
                            //.put("Colonia", dailyLogBody.getString("colDestiny"))
                            //.put("Localidad", dailyLogBody.getString("localidadDestiny"))
                            //.put("Municipio", dailyLogBody.getString("munDestiny"))
                            .put("Estado", dailyLogBody.getString("estadoDestiny"))
                            .put("Pais", "MEX")
                            .put("CodigoPostal", dailyLogBody.getInteger("zipCodeDestiny").toString()));
            ubicaciones.add(ubicacionOrigen).add(ubicacionDestino);
            cartaPorte.put("Ubicaciones", ubicaciones);

            // Conceptos
            JsonArray conceptos = new JsonArray();
            JsonArray allParcels = ccpElement.getJsonArray("parcels");
            JsonArray allPackages = ccpElement.getJsonArray("packages");

            AtomicReference<Double> totalWeight = new AtomicReference<>(0.0);
            AtomicReference<Integer> packagesCount = new AtomicReference<>(0);

            allParcels.forEach(p -> {
                JsonObject parcel = (JsonObject) p;
                JsonObject concepto = getConceptoForCCP(ccpElement.getString("ccp_type"));
                concepto.put("NoIdentificacion", parcel.getString("parcel_tracking_code"))
                        .put("Cantidad", parcel.getInteger("total_packages").toString());
                conceptos.add(concepto);
                packagesCount.set(packagesCount.get() + parcel.getInteger("total_packages"));
            });

            // Mercancias
            JsonArray arrayMercancia = new JsonArray();
            Map<String, JsonObject> remolquesMap = new HashMap<>();

            allPackages.forEach(e -> {
                JsonObject pkg = (JsonObject) e;
                if(pkg.getDouble("weight") == null || pkg.getDouble("weight") == 0.0) {
                    pkg.put("weight", 1.00);
                }

                String trailerPlate = pkg.getString("trailer_plate");
                String trailerTypeCode = pkg.getString("trailer_type_code");
                if (trailerPlate != null && trailerTypeCode != null) {
                    String trailerPlateCleaned = trailerPlate.replaceAll("[-\\s]", "");
                    JsonObject trailerInfo = new JsonObject()
                            .put("Placa", trailerPlateCleaned)
                            .put("SubTipoRem", trailerTypeCode)
                            .put("trailer_name", pkg.getString("trailer_name"));
                    remolquesMap.put(trailerPlate, trailerInfo);
                }

                String[] parts = ((JsonObject) e).getString("contains").split("-");
                JsonObject mercancia = new JsonObject()
                        .put("BienesTransp", parts[0])
                        .put("Descripcion", parts[1])
                        .put("MaterialPeligroso", "No")
                        .put("Cantidad", pkg.getInteger("total_packages").toString())
                        .put("ClaveUnidad", getClaveUnidadForCCP(ccpElement.getString("ccp_type")))
                        .put("PesoEnKg", pkg.getDouble("weight").toString());
                // GuiasIdentificacion
                JsonObject guiaIdentificacion = new JsonObject()
                        .put("NumeroGuiaIdentificacion", pkg.getString("parcel_tracking_code"))
                        .put("DescripGuiaIdentificacion", parts[1])
                        .put("PesoGuiaIdentificacion", pkg.getDouble("weight").toString());
                mercancia.put("GuiasIdentificacion", guiaIdentificacion);
                // CantidadTransporta es opcional este atributo
                JsonObject cantTransporta = new JsonObject()
                        .put("Cantidad", pkg.getInteger("total_packages").toString())
                        .put("IDOrigen", "OR000001") // Este es igual al id ubicacion TODO
                        .put("IDDestino", "DE000002"); //Igual al id destino TODO ;

                mercancia.put("CantidadTransporta", cantTransporta);
                arrayMercancia.add(mercancia);
                totalWeight.set(totalWeight.get() + pkg.getDouble("weight"));
            });

            JsonArray remolquesArray = new JsonArray(new ArrayList<>(remolquesMap.values()));

            // Mercancias
            BigDecimal bdWeight = new BigDecimal(totalWeight.get());
            bdWeight = bdWeight.setScale(2, RoundingMode.HALF_UP);
            String roundedWeight = bdWeight.toPlainString();

            JsonObject mercancias = new JsonObject()
                    .put("PesoBrutoTotal", roundedWeight)
                    .put("UnidadPeso", "KGM")
                    .put("NumTotalMercancias", packagesCount.toString());

            mercancias.put("Mercancia", arrayMercancia);
            cartaPorte.put("Mercancias", mercancias);

            // Autotransporte
            JsonObject autotransporte = new JsonObject()
                    .put("PermSCT", dailyLogBody.getString("tipoPermiso")) // tipoPermiso
                    .put("NumPermisoSCT", dailyLogBody.getString("NumPermisoSCT"))
                    .put("IdentificacionVehicular", new JsonObject()
                            .put("ConfigVehicular", dailyLogBody.getString("ConfigVehicular"))
                            .put("PesoBrutoVehicular", "2.00") //Dato pendiente TODO
                            .put("PlacaVM", dailyLogBody.getString("PlacaVM").replace("-", ""))
                            .put("AnioModeloVM", dailyLogBody.getInteger("AnioModeloVM").toString()))
                    .put("Seguros", new JsonObject()
                            .put("AseguraRespCivil", dailyLogBody.getString("AseguraRespCivil")) //
                            .put("PolizaRespCivil", dailyLogBody.getString("PolizaRespCivil")));
            if(!remolquesArray.isEmpty()) {
                autotransporte.put("Remolques", remolquesArray);
            }
            cartaPorte.put("Autotransporte", autotransporte);

            // FiguraTransporte
            JsonArray figuraTransporte = new JsonArray();
            JsonObject tiposFigura = new JsonObject()
                    .put("TipoFigura", "01") //Este va por defecto
                    .put("RFCFigura", dailyLogBody.getString("RFCFiguraDriver"))
                    .put("NumLicencia", dailyLogBody.getString("NumLicenciaDriver"))
                    .put("NombreFigura", dailyLogBody.getString("NombreFiguraDriver"));
                    /*
                    .put("Domicilio", new JsonObject()
                            .put("Localidad", dailyLogBody.getString("LocalidadDriver"))
                            .put("Municipio", dailyLogBody.getString("MunicipioDriver"))
                            .put("Estado", dailyLogBody.getString("EstadoDriver"))
                            .put("Pais", "MEX")
                            .put("CodigoPostal", dailyLogBody.getInteger("CodigoPostalDriver").toString()));
                     */
            figuraTransporte.add(tiposFigura);
            cartaPorte.put("FiguraTransporte", figuraTransporte);

            // Complemento
            JsonObject complemento = new JsonObject()
                    .put("CartaPorte", cartaPorte);

            body.put("Emisor", emisor);
            body.put("Receptor", receptor);
            body.put("Conceptos", conceptos);
            body.put("Complementos", complemento);
            body.put("LugarExpedicion", "81200")
                    .put("Total", "0")
                    .put("Moneda", "XXX")
                    .put("Subtotal", "0")
                    .put("TipoDeComprobante", "T");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return body;
    }

    public JsonObject getConceptoForCCP(String type) {
        switch(type) {
            case "courier":
                return new JsonObject()
                        .put("ClaveProdServ", "31181701")
                        .put("ClaveUnidad", getClaveUnidadForCCP(type))
                        .put("Unidad", "E48")
                        .put("Descripcion", "PAQUETES")
                        .put("ValorUnitario", "0.0")
                        .put("Importe", "0.0")
                        .put("ObjetoImp", "01");
            case "freight":
                return new JsonObject()
                        .put("ClaveProdServ", "24112700")
                        .put("ClaveUnidad", getClaveUnidadForCCP(type))
                        .put("Unidad", "E48")
                        .put("Descripcion", "CARGA")
                        .put("ValorUnitario", "0.0")
                        .put("Importe", "0.0")
                        .put("ObjetoImp", "01");
            default: return new JsonObject();
        }
    }

    public String getClaveUnidadForCCP(String type) {
        switch(type) {
            case "courier":
                return "XPK";
            case "freight":
                return "NL";
            default: return "";
        }
    }

    public static String removeLastCharOptional(String s) {
        return Optional.ofNullable(s)
                .filter(str -> str.length() != 0)
                .map(str -> str.substring(0, str.length() - 1))
                .orElse(s);
    }

}
