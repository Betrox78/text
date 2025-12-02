package service.invoicing;

import database.commons.ErrorCodes;
import database.ead_rad.EadRadDBV;
import database.invoicing.ComplementLetterPorteDBV;
import database.invoicing.InvoiceDBV;
import database.invoicing.handlers.parcelInvoiceDBV.ParcelInvoice;
import database.shipments.ShipmentsDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.UtilsMoney;
import utils.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import utils.UtilsValidation;
import java.util.*;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsValidation.isGraterAndNotNull;

public class ComplementLetterPorteSV extends ServiceVerticle {
    @Override
    protected String getDBAddress() {
        return ComplementLetterPorteDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/complementLetterPorte";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/v2", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::findAllV2);
        this.addHandler(HttpMethod.GET, "/claveProducto/:search_term", AuthMiddleware.getInstance(), this::getClaveProdServCP);
        this.addHandler(HttpMethod.GET, "/getConfigAutotransporte",  AuthMiddleware.getInstance(), this::getConfigAutotransporte);
        this.addHandler(HttpMethod.POST, "/claveProducto/register",  AuthMiddleware.getInstance(), this::registerProducto);
        this.addHandler(HttpMethod.PUT, "/claveProducto/update",  AuthMiddleware.getInstance(), this::updateClaveProducto);
        this.addHandler(HttpMethod.GET, "/getcfdiletterporte/:tracking_code", AuthMiddleware.getInstance(), this::getCfdiLetterPorte);
        this.addHandler(HttpMethod.POST, "/registerManifestCcp",  AuthMiddleware.getInstance(), this::registerManifestCcp);
        this.addHandler(HttpMethod.GET, "/getStatusCfdi/:tracking_code/:isead",  PublicRouteMiddleware.getInstance(), this::getStatusCfdi);
       this.addHandler(HttpMethod.POST, "/updatePrintManifestCcp",  PublicRouteMiddleware.getInstance(), this::updatePrintsManifestCp);
        this.addHandler(HttpMethod.GET, "/getNoPrintManifest/:id_branchoffice",  PublicRouteMiddleware.getInstance(), this::getNoPrintManifest);
        this.addHandler(HttpMethod.GET,"/getLetterPorteXML/:tracking_code", PublicRouteMiddleware.getInstance(), this::getLetterPorteXML);
        this.addHandler(HttpMethod.POST, "/updatePrintManifestCcp",  AuthMiddleware.getInstance(), this::updatePrintsManifestCp);
        this.addHandler(HttpMethod.GET, "/getNoPrintManifest/:id_branchoffice",  AuthMiddleware.getInstance(), this::getNoPrintManifest);
        this.addHandler(HttpMethod.POST, "/registerManifestCcp/ead",  AuthMiddleware.getInstance(), this::registerManifestCcpEad);
        this.addHandler(HttpMethod.GET, "/getCfdiGlobal/:tracking_code", AuthMiddleware.getInstance(), this::getCfdiGlobal);
        this.addHandler(HttpMethod.GET, "/get/manifest/ead/:id/:date_init/:date_end", AuthMiddleware.getInstance(), this::getManifestCfdiEad);
        this.addHandler(HttpMethod.POST, "/generateCCP", AuthMiddleware.getInstance(), this::generateCCP);
        this.addHandler(HttpMethod.GET, "/getCCPXML/:ccp_type/:id", AuthMiddleware.getInstance(), this::getCCPXML);
        this.addHandler(HttpMethod.GET, "/getTravelLogCCPs/:travel_log_id", AuthMiddleware.getInstance(), this::getTravelLogCCPs);
        this.addHandler(HttpMethod.POST, "/updateCCPisStamped", AuthMiddleware.getInstance(), this::updateCCPisStamped);
        this.addHandler(HttpMethod.POST, "/generateCCPEadRad", AuthMiddleware.getInstance(), this::generateCCPEadRad);
        this.addHandler(HttpMethod.GET, "/getManifestCCPXML/:parcels_manifest_id", AuthMiddleware.getInstance(), this::getManifestCCPXML);
        super.start(startFuture);
    }

    private void getClaveProdServCP(RoutingContext context) {
        int limit = 0;
        String param = context.request().getParam("search_term") != null ? context.request().getParam("search_term") : "";
        try {
            limit = context.request().getParam("limit") != null ? Integer.parseInt(context.request().getParam("limit")) : 0;
        } catch (Exception ignored) {
        }
        JsonObject searchTerm = new JsonObject().put("searchTerm", param).put("limit", limit);
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_SEARCH_CLAVE_PRDUCT);
            vertx.eventBus().send(this.getDBAddress(), searchTerm, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                } catch (Exception e) {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);

        }
    }
    private void getConfigAutotransporte(RoutingContext context) {

        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_GET_CFDI_STATUS);
            vertx.eventBus().send(this.getDBAddress(),null, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                } catch (Exception e) {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);

        }
    }

    private void registerProducto(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(ComplementLetterPorteDBV.class.getSimpleName(), body, options(ComplementLetterPorteDBV.ACTION_CLAVEPRODUCTO_REGISTER), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }
    private void updateClaveProducto(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(ComplementLetterPorteDBV.class.getSimpleName(), body, options(ComplementLetterPorteDBV.ACTION_CLAVEPRODUCTO_UPDATE), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }

    private void getCfdiLetterPorte(RoutingContext context) {
       // int limit = 0;
        String param = context.request().getParam("tracking_code") ;

        JsonObject searchTerm = new JsonObject().put("tracking_code", param);
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_GET_CFDI_BODY);
            vertx.eventBus().send(this.getDBAddress(), searchTerm, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    JsonArray response= new JsonArray();
                    JsonObject resultData=new JsonObject();
                    resultData.put("letterPorteData",reply.result().body());
                    JsonArray result =new JsonArray().add(reply.result().body()) ;
                    try {
                        for(int i = 0 ; i < resultData.getJsonArray("letterPorteData").size() ; i++ ) {

                            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                            JsonObject resultParcel = resultData.getJsonArray("letterPorteData").getJsonObject(i);
                            String xmlBase64 = resultParcel.getString("xml");
                            byte[] decodedXML = Base64.getDecoder().decode(xmlBase64.getBytes(StandardCharsets.UTF_8));
                            InputStream XML = new ByteArrayInputStream(decodedXML);

                            Document documentValue = dBuilder.parse(XML);
                            JsonObject valueCFDI = new JsonObject();
                            JsonObject cartaporte = new JsonObject();
                            valueCFDI.put("Version", documentValue.getDocumentElement().getAttribute("Version"));
                            valueCFDI.put("Sello", documentValue.getDocumentElement().getAttribute("Sello"));
                            valueCFDI.put("Fecha", documentValue.getDocumentElement().getAttribute("Fecha"));
                            valueCFDI.put("Folio", documentValue.getDocumentElement().getAttribute("Folio"));
                            valueCFDI.put("Serie", documentValue.getDocumentElement().getAttribute("Serie"));
                            valueCFDI.put("NoCertificado", documentValue.getDocumentElement().getAttribute("NoCertificado"));
                            valueCFDI.put("Certificado", documentValue.getDocumentElement().getAttribute("Certificado"));

                            valueCFDI.put("SubTotal", documentValue.getDocumentElement().getAttribute("SubTotal"));
                            valueCFDI.put("Moneda", documentValue.getDocumentElement().getAttribute("Moneda"));
                            valueCFDI.put("Total", documentValue.getDocumentElement().getAttribute("Total"));
                            String totalLetra = UtilsMoney.numberToLetter(documentValue.getDocumentElement().getAttribute("Total"));
                            valueCFDI.put("totalLetra", totalLetra);
                            valueCFDI.put("TipoDeComprobante", documentValue.getDocumentElement().getAttribute("TipoDeComprobante"));
                            valueCFDI.put("LugarExpedicion", documentValue.getDocumentElement().getAttribute("LugarExpedicion"));
                            NodeList emisor = documentValue.getElementsByTagName("cfdi:Emisor");
                            valueCFDI.put("EmisorRfc", emisor.item(0).getAttributes().getNamedItem("Rfc").getNodeValue());
                            valueCFDI.put("EmisorNombre", emisor.item(0).getAttributes().getNamedItem("Nombre").getNodeValue());
                            valueCFDI.put("EmisorRegimenFiscal", emisor.item(0).getAttributes().getNamedItem("RegimenFiscal").getNodeValue());
                            NodeList receptor = documentValue.getElementsByTagName("cfdi:Receptor");
                            valueCFDI.put("ReceptorRfc", receptor.item(0).getAttributes().getNamedItem("Rfc").getNodeValue());
                            valueCFDI.put("ReceptorNombre", receptor.item(0).getAttributes().getNamedItem("Nombre").getNodeValue());
                            valueCFDI.put("ReceptorRegimenFiscal", receptor.item(0).getAttributes().getNamedItem("UsoCFDI").getNodeValue());
                            NodeList Conceptos = documentValue.getElementsByTagName("cfdi:Concepto");
                            JsonArray arrayConcepto = new JsonArray();
                            for (int x = 0; x < Conceptos.getLength(); x++) {
                                JsonObject ConceptosJSON = new JsonObject();
                                ConceptosJSON.put("ClaveProdServ", Conceptos.item(x).getAttributes().getNamedItem("ClaveProdServ").getNodeValue());
                                ConceptosJSON.put("NoIdentificacion", Conceptos.item(x).getAttributes().getNamedItem("NoIdentificacion").getNodeValue());
                                ConceptosJSON.put("Descripcion", Conceptos.item(x).getAttributes().getNamedItem("Descripcion").getNodeValue());
                                ConceptosJSON.put("Cantidad", Conceptos.item(x).getAttributes().getNamedItem("Cantidad").getNodeValue());
                                ConceptosJSON.put("ClaveUnidad", Conceptos.item(x).getAttributes().getNamedItem("ClaveUnidad").getNodeValue());
                                ConceptosJSON.put("Unidad", Conceptos.item(x).getAttributes().getNamedItem("Unidad").getNodeValue());
                                ConceptosJSON.put("ValorUnitario", Conceptos.item(x).getAttributes().getNamedItem("ValorUnitario").getNodeValue());
                                ConceptosJSON.put("Importe", Conceptos.item(x).getAttributes().getNamedItem("Importe").getNodeValue());
                                arrayConcepto.add(ConceptosJSON);
                            }
                            valueCFDI.put("Concepto", arrayConcepto);


                            NodeList TimbreFiscalDigital = documentValue.getElementsByTagName("tfd:TimbreFiscalDigital");
                            JsonObject TimbreFiscalDigitalJSON = new JsonObject();
                            TimbreFiscalDigitalJSON.put("FechaTimbrado", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("FechaTimbrado").getNodeValue());
                            TimbreFiscalDigitalJSON.put("RfcProvCertif", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("RfcProvCertif").getNodeValue());
                            TimbreFiscalDigitalJSON.put("SelloCFD", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("SelloCFD").getNodeValue());
                            TimbreFiscalDigitalJSON.put("NoCertificadoSAT", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("NoCertificadoSAT").getNodeValue());
                            TimbreFiscalDigitalJSON.put("SelloSAT", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("SelloSAT").getNodeValue());
                            TimbreFiscalDigitalJSON.put("NoCertificadoSAT", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("NoCertificadoSAT").getNodeValue());
                            TimbreFiscalDigitalJSON.put("NoCertificadoSAT", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("NoCertificadoSAT").getNodeValue());
                            TimbreFiscalDigitalJSON.put("UUID", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("UUID").getNodeValue());
                            valueCFDI.put("TimbreFiscalDigital", TimbreFiscalDigitalJSON);

                            //carta porte master
                            NodeList cartaPorteMaster = documentValue.getElementsByTagName("cartaporte20:CartaPorte");
                            JsonObject cartaPorteMasterJSON = new JsonObject();
                            //Sin version de cp entonces es ultima milla
                            if( cartaPorteMaster.getLength()>0){
                                cartaPorteMasterJSON.put("Version", cartaPorteMaster.item(0).getAttributes().getNamedItem("Version").getNodeValue());
                                cartaPorteMasterJSON.put("TranspInternac", cartaPorteMaster.item(0).getAttributes().getNamedItem("TranspInternac").getNodeValue());
                                cartaPorteMasterJSON.put("TotalDistRec", cartaPorteMaster.item(0).getAttributes().getNamedItem("TotalDistRec").getNodeValue());
                                cartaporte.put("cartaPorte", cartaPorteMasterJSON);
                                //ubicaciones
                                NodeList ubicacionesList = documentValue.getElementsByTagName("cartaporte20:Ubicacion");
                                JsonArray ubiacionesArray = new JsonArray();
                                for (int x = 0; x < ubicacionesList.getLength(); x++) {
                                    JsonObject ubicacionJson = new JsonObject();
                                    ubicacionJson.put("TipoUbicacion", ubicacionesList.item(x).getAttributes().getNamedItem("TipoUbicacion").getNodeValue());
                                    ubicacionJson.put("IDUbicacion", ubicacionesList.item(x).getAttributes().getNamedItem("IDUbicacion").getNodeValue());
                                    ubicacionJson.put("RFCRemitenteDestinatario", ubicacionesList.item(x).getAttributes().getNamedItem("RFCRemitenteDestinatario").getNodeValue());
                                    ubicacionJson.put("FechaHoraSalidaLlegada", ubicacionesList.item(x).getAttributes().getNamedItem("FechaHoraSalidaLlegada").getNodeValue());
                                    if (ubicacionesList.item(x).getAttributes().getNamedItem("DistanciaRecorrida") != null) {
                                        ubicacionJson.put("DistanciaRecorrida", ubicacionesList.item(x).getAttributes().getNamedItem("DistanciaRecorrida").getNodeValue());
                                    }
                                    NodeList domicilio = documentValue.getElementsByTagName("cartaporte20:Domicilio");
                                    JsonObject domicilioJson = new JsonObject();
                                    domicilioJson.put("Calle", domicilio.item(x).getAttributes().getNamedItem("Calle").getNodeValue());
                                    domicilioJson.put("NumeroExterior", domicilio.item(x).getAttributes().getNamedItem("NumeroExterior").getNodeValue());
                                    domicilioJson.put("NumeroInterior", domicilio.item(x).getAttributes().getNamedItem("NumeroInterior").getNodeValue());
                                    domicilioJson.put("Colonia", domicilio.item(x).getAttributes().getNamedItem("Colonia").getNodeValue());
                                    domicilioJson.put("Localidad", domicilio.item(x).getAttributes().getNamedItem("Localidad").getNodeValue());
                                    domicilioJson.put("Municipio", domicilio.item(x).getAttributes().getNamedItem("Municipio").getNodeValue());
                                    domicilioJson.put("Estado", domicilio.item(x).getAttributes().getNamedItem("Estado").getNodeValue());
                                    domicilioJson.put("Pais", domicilio.item(x).getAttributes().getNamedItem("Pais").getNodeValue());
                                    domicilioJson.put("CodigoPostal", domicilio.item(x).getAttributes().getNamedItem("CodigoPostal").getNodeValue());
                                    ubicacionJson.put("domicilio", domicilioJson);

                                    ubiacionesArray.add(ubicacionJson);
                                }
                                cartaporte.put("ubicaciones", ubiacionesArray);
                                NodeList mercancias = documentValue.getElementsByTagName("cartaporte20:Mercancias");
                                JsonObject mercanciasJson = new JsonObject();

                                mercanciasJson.put("PesoBrutoTotal", mercancias.item(0).getAttributes().getNamedItem("PesoBrutoTotal").getNodeValue());
                                mercanciasJson.put("UnidadPeso", mercancias.item(0).getAttributes().getNamedItem("UnidadPeso").getNodeValue());
                                mercanciasJson.put("NumTotalMercancias", mercancias.item(0).getAttributes().getNamedItem("NumTotalMercancias").getNodeValue());
                                NodeList mercanciasList = documentValue.getElementsByTagName("cartaporte20:Mercancia");
                                JsonArray mercanciaArray = new JsonArray();
                                for (int x = 0; x < mercanciasList.getLength(); x++) {
                                    JsonObject mercancia = new JsonObject();
                                    mercancia.put("BienesTransp", mercanciasList.item(x).getAttributes().getNamedItem("BienesTransp").getNodeValue());
                                    mercancia.put("Descripcion", mercanciasList.item(x).getAttributes().getNamedItem("Descripcion").getNodeValue());
                                    mercancia.put("Cantidad", mercanciasList.item(x).getAttributes().getNamedItem("Cantidad").getNodeValue());
                                    mercancia.put("ClaveUnidad", mercanciasList.item(x).getAttributes().getNamedItem("ClaveUnidad").getNodeValue());
                                    mercancia.put("Unidad", mercanciasList.item(x).getAttributes().getNamedItem("Unidad").getNodeValue());
                                    mercancia.put("PesoEnKg", mercanciasList.item(x).getAttributes().getNamedItem("PesoEnKg").getNodeValue());
                                    NodeList cantidadTransportadaList = documentValue.getElementsByTagName("cartaporte20:CantidadTransporta");
                                    JsonObject cantidadTransportadaJson = new JsonObject();
                                    cantidadTransportadaJson.put("Cantidad", cantidadTransportadaList.item(x).getAttributes().getNamedItem("Cantidad").getNodeValue());
                                    cantidadTransportadaJson.put("IDOrigen", cantidadTransportadaList.item(x).getAttributes().getNamedItem("IDOrigen").getNodeValue());
                                    cantidadTransportadaJson.put("IDDestino", cantidadTransportadaList.item(x).getAttributes().getNamedItem("IDDestino").getNodeValue());
                                    mercancia.put("CantidadTransporta", cantidadTransportadaJson);

                                    mercanciaArray.add(mercancia);
                                }
                                cartaporte.put("mercancia", mercanciaArray);
                                NodeList autotransporteList = documentValue.getElementsByTagName("cartaporte20:Autotransporte");
                                JsonArray autotransporteArray = new JsonArray();
                                for (int x = 0; x < autotransporteList.getLength(); x++) {
                                    JsonObject autotransporteJson = new JsonObject();
                                    autotransporteJson.put("PermSCT", autotransporteList.item(x).getAttributes().getNamedItem("PermSCT").getNodeValue());
                                    autotransporteJson.put("NumPermisoSCT", autotransporteList.item(x).getAttributes().getNamedItem("NumPermisoSCT").getNodeValue());
                                    autotransporteJson.put("PermSCT", autotransporteList.item(x).getAttributes().getNamedItem("PermSCT").getNodeValue());

                                    NodeList identificacionVehicularList = documentValue.getElementsByTagName("cartaporte20:IdentificacionVehicular");
                                    JsonObject identificacionVehicularJson = new JsonObject();
                                    identificacionVehicularJson.put("ConfigVehicular", identificacionVehicularList.item(x).getAttributes().getNamedItem("ConfigVehicular").getNodeValue());
                                    identificacionVehicularJson.put("PlacaVM", identificacionVehicularList.item(x).getAttributes().getNamedItem("PlacaVM").getNodeValue());
                                    identificacionVehicularJson.put("AnioModeloVM", identificacionVehicularList.item(x).getAttributes().getNamedItem("AnioModeloVM").getNodeValue());
                                    autotransporteJson.put("IdentificacionVehicular", identificacionVehicularJson);
                                    NodeList segurosList = documentValue.getElementsByTagName("cartaporte20:Seguros");
                                    JsonObject segurosJson = new JsonObject();
                                    segurosJson.put("AseguraRespCivil", segurosList.item(x).getAttributes().getNamedItem("AseguraRespCivil").getNodeValue());
                                    segurosJson.put("PolizaRespCivil", segurosList.item(x).getAttributes().getNamedItem("PolizaRespCivil").getNodeValue());
                                    // segurosJson.put("AseguraMedAmbiente", segurosList.item(x).getAttributes().getNamedItem("AseguraMedAmbiente").getNodeValue());
                                    //  segurosJson.put("PolizaMedAmbiente", segurosList.item(x).getAttributes().getNamedItem("PolizaMedAmbiente").getNodeValue());
                                    autotransporteJson.put("Seguros", segurosJson);

                                    autotransporteArray.add(autotransporteJson);
                                }
                                cartaporte.put("autotransporte", autotransporteArray);

                                NodeList figuraTransporteList = documentValue.getElementsByTagName("cartaporte20:TiposFigura");
                                JsonArray figuraTransporteArray = new JsonArray();
                                for (int x = 0; x < figuraTransporteList.getLength(); x++) {
                                    JsonObject figuraTransporteJson = new JsonObject();
                                    figuraTransporteJson.put("TipoFigura", figuraTransporteList.item(x).getAttributes().getNamedItem("TipoFigura").getNodeValue());
                                    figuraTransporteJson.put("RFCFigura", figuraTransporteList.item(x).getAttributes().getNamedItem("RFCFigura").getNodeValue());
                                    figuraTransporteJson.put("NumLicencia", figuraTransporteList.item(x).getAttributes().getNamedItem("NumLicencia").getNodeValue());
                                    figuraTransporteJson.put("NombreFigura", figuraTransporteList.item(x).getAttributes().getNamedItem("NombreFigura").getNodeValue());
                                    figuraTransporteArray.add(figuraTransporteJson);
                                }
                                cartaporte.put("figuraTransporte", figuraTransporteArray);
                                valueCFDI.put("complementoCartaPorte", cartaporte);
                        }
                            resultParcel.put("timbre", valueCFDI);
                            response.add(resultParcel);
                        }
                        responseOk(context, response);
                    } catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, "erro en el formato base64(xml)", t);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);

        }
    }
    private void registerManifestCcp(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            this.vertx.eventBus().send(ComplementLetterPorteDBV.class.getSimpleName(), body, options(ComplementLetterPorteDBV.ACTION_REGISTER_MANIFEST_CCP), reply -> {
                if (reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {

                        if (body.getJsonObject("data").getInteger("register") == 1)
                            responseOk(context, reply.result().body(), "Report");
                        else {
                            JsonObject result = new JsonObject().put("result", reply.result().body());
                            String cfdi = result.getJsonObject("result").getString("body_cfdi");
                            cfdi = Normalizer.normalize(cfdi, Normalizer.Form.NFD);
                            cfdi = cfdi.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
                            JsonObject body_cfdi = new JsonObject(cfdi);
                            execService(context, ParcelInvoice.ACTION, body_cfdi, parcelsResult -> {
                                String xml = XML.toString(parcelsResult.getString("factura_timbrada"));
                                //responseOk(context, new JsonObject().put("Response", xml).put("cadena_original", parcelsResult.getString("cadena_original")));
                                //System.out.println("timbre:"+xml);
                                // responseOk(context, new JsonObject().put("Response", xml));
                                JsonObject body_result = new JsonObject().put("XML", parcelsResult.getString("factura_timbrada")).put("updated_by", body.getJsonObject("data").getInteger("idUser")).put("parcel_invoice_complement_id", body.getJsonObject("data").getJsonObject("cartaPorte").getInteger("parcel_invoice_complement_id"));

                                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_INSERT_XML_TIMBRADO);
                                vertx.eventBus().send(this.getDBAddress(), body_result, options, reply2 -> {
                                    try {
                                        if (reply2.failed()) {
                                            throw new Exception(reply2.cause());
                                        }
                                        responseOk(context, reply2.result().body());
                                    } catch (Exception e) {
                                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                                    }
                                });
                            });
                        }
                    }

                } else {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });
        }  catch (Exception ex) {
        ex.printStackTrace();
        responseError(context, UNEXPECTED_ERROR, ex.getMessage());
         }
    }
    private void getStatusCfdi(RoutingContext context) {

        try {
            String param = context.request().getParam("tracking_code") ;
            String isead = context.request().getParam("isead") ;

            JsonObject searchTerm = new JsonObject().put("tracking_code", param).put("isead",Boolean.parseBoolean(isead));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_GET_CFDI_STATUS);
            vertx.eventBus().send(this.getDBAddress(),searchTerm, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                } catch (Exception e) {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);

        }
    }
    private void execService(RoutingContext context, String action, JsonObject body, Handler<JsonObject> handler) {
        try {
            vertx.eventBus().send(InvoiceDBV.class.getSimpleName(), body, options(action), (AsyncResult<Message<JsonObject>> reply) -> {
                if (reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {
                        handler.handle(reply.result().body());
                    }
                } else {
                    responseError(context, "Ocurrió un error inesperado", reply.cause().getMessage());
                }
            });
        } catch (Exception ex) {
        ex.printStackTrace();
        responseError(context, UNEXPECTED_ERROR, ex.getMessage());
    }
    }
    private void updatePrintsManifestCp(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        this.vertx.eventBus().send(ComplementLetterPorteDBV.class.getSimpleName(), body, options(ComplementLetterPorteDBV.ACTION_PRINT_MANIFEST_CP), reply -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    responseOk(context, reply.result().body(), "Report");
                }
            } else {
                responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
            }
        });
    }
    private void getNoPrintManifest(RoutingContext context) {

        try {
            String param = context.request().getParam("id_branchoffice") ;

            JsonObject searchTerm = new JsonObject().put("id_branchoffice", param);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_GET_NO_PRINT);
            vertx.eventBus().send(this.getDBAddress(),searchTerm, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
                } catch (Exception e) {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);

        }
    }

    private void getLetterPorteXML(RoutingContext context) {
        String param = context.request().getParam("tracking_code");

        JsonObject searchTerm = new JsonObject().put("tracking_code", param);

        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_GET_CFDI_BODY_SITE);
            vertx.eventBus().send(this.getDBAddress(), searchTerm, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    JsonArray response = new JsonArray();
                    JsonObject resultData = new JsonObject();
                    resultData.put("letterPorteData", reply.result().body());
                    InvoiceDBV.ServiceTypes serviceName = InvoiceDBV.ServiceTypes.getTypeByReservationCode(param);
                    try {
                        for (int i = 0; i < resultData.getJsonArray("letterPorteData").size(); i++) {
                            JsonObject resultParcel = resultData.getJsonArray("letterPorteData").getJsonObject(i);

                            if(InvoiceDBV.ServiceTypes.PARCEL.equals(serviceName)) {
                                boolean parcelNotesOnInvoice = resultParcel.getBoolean("parcel_notes_on_invoice");
                                if (!parcelNotesOnInvoice) {
                                    // Si el cliente no requiere las obs en la factura, eliminamos los atributos notes y notes_invoice
                                    resultParcel.remove("notes");
                                    resultParcel.remove("notes_invoice");
                                }
                            }
                            JsonObject valueCFDI = extractDataFromXMLObj(resultParcel);
                            resultParcel.put("timbre", valueCFDI);
                            response.add(resultParcel);
                        }
                        responseOk(context, response);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        responseError(context, "erro en el formato base64(xml)", t);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });
        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);

        }
    }
    private void registerManifestCcpEad(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            this.vertx.eventBus().send(ComplementLetterPorteDBV.class.getSimpleName(), body, options(ComplementLetterPorteDBV.ACTION_REGISTER_MANIFEST_CCP_EAD), reply -> {
                if (reply.succeeded()) {
                    if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                    } else {

                        if (body.getJsonObject("data").getInteger("register") == 1)
                            responseOk(context, reply.result().body(), "Report");
                        else {
                            JsonObject result = new JsonObject().put("result", reply.result().body());
                            String cfdi = result.getJsonObject("result").getString("body_cfdi");
                            cfdi = Normalizer.normalize(cfdi, Normalizer.Form.NFD);
                            cfdi = cfdi.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
                            JsonObject body_cfdi = new JsonObject(cfdi);
                            execService(context, ParcelInvoice.ACTION, body_cfdi, parcelsResult -> {
                              //  String xml = XML.toString(parcelsResult.getString("factura_timbrada"));
                                JsonObject body_result = new JsonObject().put("XML", parcelsResult.getString("factura_timbrada")).put("updated_by", body.getJsonObject("data").getInteger("idUser")).put("parcel_invoice_complement_id", result.getJsonObject("result").getInteger("parcel_invoice_complement_id"));
                                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_INSERT_XML_TIMBRADO);
                                vertx.eventBus().send(this.getDBAddress(), body_result, options, reply2 -> {
                                    try {
                                        if (reply2.failed()) {
                                            throw new Exception(reply2.cause());
                                        }
                                        responseOk(context, reply2.result().body());
                                    } catch (Exception e) {
                                        responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                                    }
                                });
                            });
                        }
                    }

                } else {
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });
        } catch (Exception ex) {
        ex.printStackTrace();
        responseError(context, UNEXPECTED_ERROR, ex.getMessage());
    }
    }

    private void getManifestCfdiEad(RoutingContext context) {
        HttpServerRequest request = context.request();
        String id = request.getParam("id");
        String date_init= request.getParam("date_init");
        String date_end= request.getParam("date_end");


        JsonObject body = new JsonObject().put("terminal_origin_id", id)
                .put("date_init",date_init)
                .put("date_end",date_end);
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_GET_MANIFEST_CFDI_EAD);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    responseOk(context, reply.result().body());

                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getMessage());
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void getCfdiGlobal(RoutingContext context) {
        // int limit = 0;
        String param = context.request().getParam("tracking_code") ;

        JsonObject searchTerm = new JsonObject().put("tracking_code", param);
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_GET_CFDI_GLOBAL_BODY);
            vertx.eventBus().send(this.getDBAddress(), searchTerm, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    JsonArray response= new JsonArray();
                    JsonObject resultData=new JsonObject();
                    resultData.put("letterPorteData",reply.result().body());
                    JsonArray result =new JsonArray().add(reply.result().body()) ;
                    try {
                        for(int i = 0 ; i < resultData.getJsonArray("letterPorteData").size() ; i++ ) {

                            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                            JsonObject resultParcel = resultData.getJsonArray("letterPorteData").getJsonObject(i);
                            String xmlBase64 = resultParcel.getString("xml");
                            byte[] decodedXML = Base64.getDecoder().decode(xmlBase64.getBytes(StandardCharsets.UTF_8));
                            InputStream XML = new ByteArrayInputStream(decodedXML);

                            Document documentValue = dBuilder.parse(XML);
                            JsonObject valueCFDI = new JsonObject();
                            JsonObject cartaporte = new JsonObject();
                            valueCFDI.put("Version", documentValue.getDocumentElement().getAttribute("Version"));
                            valueCFDI.put("Sello", documentValue.getDocumentElement().getAttribute("Sello"));
                            valueCFDI.put("Fecha", documentValue.getDocumentElement().getAttribute("Fecha"));
                            valueCFDI.put("Folio", documentValue.getDocumentElement().getAttribute("Folio"));
                            valueCFDI.put("Serie", documentValue.getDocumentElement().getAttribute("Serie"));
                            valueCFDI.put("FormaPago", documentValue.getDocumentElement().getAttribute("FormaPago"));
                            valueCFDI.put("MetodoPago", documentValue.getDocumentElement().getAttribute("MetodoPago"));
                            valueCFDI.put("NoCertificado", documentValue.getDocumentElement().getAttribute("NoCertificado"));
                            valueCFDI.put("Certificado", documentValue.getDocumentElement().getAttribute("Certificado"));

                            valueCFDI.put("SubTotal", documentValue.getDocumentElement().getAttribute("SubTotal"));
                            valueCFDI.put("Moneda", documentValue.getDocumentElement().getAttribute("Moneda"));
                            valueCFDI.put("Total", documentValue.getDocumentElement().getAttribute("Total"));
                            String totalLetra = UtilsMoney.numberToLetter(documentValue.getDocumentElement().getAttribute("Total"));
                            valueCFDI.put("totalLetra", totalLetra);
                            valueCFDI.put("TipoDeComprobante", documentValue.getDocumentElement().getAttribute("TipoDeComprobante"));
                            valueCFDI.put("LugarExpedicion", documentValue.getDocumentElement().getAttribute("LugarExpedicion"));
                            NodeList emisor = documentValue.getElementsByTagName("cfdi:Emisor");
                            valueCFDI.put("EmisorRfc", emisor.item(0).getAttributes().getNamedItem("Rfc").getNodeValue());
                            valueCFDI.put("EmisorNombre", emisor.item(0).getAttributes().getNamedItem("Nombre").getNodeValue());
                            valueCFDI.put("EmisorRegimenFiscal", emisor.item(0).getAttributes().getNamedItem("RegimenFiscal").getNodeValue());
                            NodeList receptor = documentValue.getElementsByTagName("cfdi:Receptor");
                            valueCFDI.put("ReceptorRfc", receptor.item(0).getAttributes().getNamedItem("Rfc").getNodeValue());
                            valueCFDI.put("ReceptorNombre", receptor.item(0).getAttributes().getNamedItem("Nombre").getNodeValue());
                            valueCFDI.put("ReceptorUsoCFDI", receptor.item(0).getAttributes().getNamedItem("UsoCFDI").getNodeValue());
                            valueCFDI.put("RegimenFiscalReceptor", receptor.item(0).getAttributes().getNamedItem("RegimenFiscalReceptor").getNodeValue());
                            valueCFDI.put("DomicilioFiscalReceptor", receptor.item(0).getAttributes().getNamedItem("DomicilioFiscalReceptor").getNodeValue());
                            BigDecimal iva = new BigDecimal(valueCFDI.getString("Total")).subtract(new BigDecimal(valueCFDI.getString("SubTotal")));
                            valueCFDI.put("ImporteImpuesto", iva.toString());

                            NodeList Conceptos = documentValue.getElementsByTagName("cfdi:Concepto");
                            JsonArray arrayConcepto = new JsonArray();
                            for (int x = 0; x < Conceptos.getLength(); x++) {
                                JsonObject ConceptosJSON = new JsonObject();
                                ConceptosJSON.put("ClaveProdServ", Conceptos.item(x).getAttributes().getNamedItem("ClaveProdServ").getNodeValue());
                                ConceptosJSON.put("NoIdentificacion", Conceptos.item(x).getAttributes().getNamedItem("NoIdentificacion").getNodeValue());
                                ConceptosJSON.put("Descripcion", Conceptos.item(x).getAttributes().getNamedItem("Descripcion").getNodeValue());
                                ConceptosJSON.put("Cantidad", Conceptos.item(x).getAttributes().getNamedItem("Cantidad").getNodeValue());
                                ConceptosJSON.put("ClaveUnidad", Conceptos.item(x).getAttributes().getNamedItem("ClaveUnidad").getNodeValue());
                                ConceptosJSON.put("Unidad", Conceptos.item(x).getAttributes().getNamedItem("Unidad").getNodeValue());
                                ConceptosJSON.put("ValorUnitario", Conceptos.item(x).getAttributes().getNamedItem("ValorUnitario").getNodeValue());
                                ConceptosJSON.put("Importe", Conceptos.item(x).getAttributes().getNamedItem("Importe").getNodeValue());
                                arrayConcepto.add(ConceptosJSON);
                            }
                            valueCFDI.put("Concepto", arrayConcepto);


                            NodeList TimbreFiscalDigital = documentValue.getElementsByTagName("tfd:TimbreFiscalDigital");
                            JsonObject TimbreFiscalDigitalJSON = new JsonObject();
                            TimbreFiscalDigitalJSON.put("FechaTimbrado", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("FechaTimbrado").getNodeValue());
                            TimbreFiscalDigitalJSON.put("RfcProvCertif", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("RfcProvCertif").getNodeValue());
                            TimbreFiscalDigitalJSON.put("SelloCFD", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("SelloCFD").getNodeValue());
                            TimbreFiscalDigitalJSON.put("NoCertificadoSAT", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("NoCertificadoSAT").getNodeValue());
                            TimbreFiscalDigitalJSON.put("SelloSAT", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("SelloSAT").getNodeValue());
                            TimbreFiscalDigitalJSON.put("NoCertificadoSAT", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("NoCertificadoSAT").getNodeValue());
                            TimbreFiscalDigitalJSON.put("NoCertificadoSAT", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("NoCertificadoSAT").getNodeValue());
                            TimbreFiscalDigitalJSON.put("UUID", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("UUID").getNodeValue());
                            valueCFDI.put("TimbreFiscalDigital", TimbreFiscalDigitalJSON);

                            //carta porte master
                            NodeList cartaPorteMaster = documentValue.getElementsByTagName("cartaporte20:CartaPorte");
                            JsonObject cartaPorteMasterJSON = new JsonObject();
                            //Sin version de cp entonces es ultima milla
                            if( cartaPorteMaster.getLength()>0){
                                cartaPorteMasterJSON.put("Version", cartaPorteMaster.item(0).getAttributes().getNamedItem("Version").getNodeValue());
                                cartaPorteMasterJSON.put("TranspInternac", cartaPorteMaster.item(0).getAttributes().getNamedItem("TranspInternac").getNodeValue());
                                cartaPorteMasterJSON.put("TotalDistRec", cartaPorteMaster.item(0).getAttributes().getNamedItem("TotalDistRec").getNodeValue());
                                cartaporte.put("cartaPorte", cartaPorteMasterJSON);
                                //ubicaciones
                                NodeList ubicacionesList = documentValue.getElementsByTagName("cartaporte20:Ubicacion");
                                JsonArray ubiacionesArray = new JsonArray();
                                for (int x = 0; x < ubicacionesList.getLength(); x++) {
                                    JsonObject ubicacionJson = new JsonObject();
                                    ubicacionJson.put("TipoUbicacion", ubicacionesList.item(x).getAttributes().getNamedItem("TipoUbicacion").getNodeValue());
                                    ubicacionJson.put("IDUbicacion", ubicacionesList.item(x).getAttributes().getNamedItem("IDUbicacion").getNodeValue());
                                    ubicacionJson.put("RFCRemitenteDestinatario", ubicacionesList.item(x).getAttributes().getNamedItem("RFCRemitenteDestinatario").getNodeValue());
                                    ubicacionJson.put("FechaHoraSalidaLlegada", ubicacionesList.item(x).getAttributes().getNamedItem("FechaHoraSalidaLlegada").getNodeValue());
                                    if (ubicacionesList.item(x).getAttributes().getNamedItem("DistanciaRecorrida") != null) {
                                        ubicacionJson.put("DistanciaRecorrida", ubicacionesList.item(x).getAttributes().getNamedItem("DistanciaRecorrida").getNodeValue());
                                    }
                                    NodeList domicilio = documentValue.getElementsByTagName("cartaporte20:Domicilio");
                                    JsonObject domicilioJson = new JsonObject();
                                    domicilioJson.put("Calle", domicilio.item(x).getAttributes().getNamedItem("Calle").getNodeValue());
                                    domicilioJson.put("NumeroExterior", domicilio.item(x).getAttributes().getNamedItem("NumeroExterior").getNodeValue());
                                    domicilioJson.put("NumeroInterior", domicilio.item(x).getAttributes().getNamedItem("NumeroInterior").getNodeValue());
                                    domicilioJson.put("Colonia", domicilio.item(x).getAttributes().getNamedItem("Colonia").getNodeValue());
                                    domicilioJson.put("Localidad", domicilio.item(x).getAttributes().getNamedItem("Localidad").getNodeValue());
                                    domicilioJson.put("Municipio", domicilio.item(x).getAttributes().getNamedItem("Municipio").getNodeValue());
                                    domicilioJson.put("Estado", domicilio.item(x).getAttributes().getNamedItem("Estado").getNodeValue());
                                    domicilioJson.put("Pais", domicilio.item(x).getAttributes().getNamedItem("Pais").getNodeValue());
                                    domicilioJson.put("CodigoPostal", domicilio.item(x).getAttributes().getNamedItem("CodigoPostal").getNodeValue());
                                    ubicacionJson.put("domicilio", domicilioJson);

                                    ubiacionesArray.add(ubicacionJson);
                                }
                                cartaporte.put("ubicaciones", ubiacionesArray);
                                NodeList mercancias = documentValue.getElementsByTagName("cartaporte20:Mercancias");
                                JsonObject mercanciasJson = new JsonObject();

                                mercanciasJson.put("PesoBrutoTotal", mercancias.item(0).getAttributes().getNamedItem("PesoBrutoTotal").getNodeValue());
                                mercanciasJson.put("UnidadPeso", mercancias.item(0).getAttributes().getNamedItem("UnidadPeso").getNodeValue());
                                mercanciasJson.put("NumTotalMercancias", mercancias.item(0).getAttributes().getNamedItem("NumTotalMercancias").getNodeValue());
                                NodeList mercanciasList = documentValue.getElementsByTagName("cartaporte20:Mercancia");
                                JsonArray mercanciaArray = new JsonArray();
                                for (int x = 0; x < mercanciasList.getLength(); x++) {
                                    JsonObject mercancia = new JsonObject();
                                    mercancia.put("BienesTransp", mercanciasList.item(x).getAttributes().getNamedItem("BienesTransp").getNodeValue());
                                    mercancia.put("Descripcion", mercanciasList.item(x).getAttributes().getNamedItem("Descripcion").getNodeValue());
                                    mercancia.put("Cantidad", mercanciasList.item(x).getAttributes().getNamedItem("Cantidad").getNodeValue());
                                    mercancia.put("ClaveUnidad", mercanciasList.item(x).getAttributes().getNamedItem("ClaveUnidad").getNodeValue());
                                    mercancia.put("Unidad", mercanciasList.item(x).getAttributes().getNamedItem("Unidad").getNodeValue());
                                    mercancia.put("PesoEnKg", mercanciasList.item(x).getAttributes().getNamedItem("PesoEnKg").getNodeValue());
                                    NodeList cantidadTransportadaList = documentValue.getElementsByTagName("cartaporte20:CantidadTransporta");
                                    JsonObject cantidadTransportadaJson = new JsonObject();
                                    cantidadTransportadaJson.put("Cantidad", cantidadTransportadaList.item(x).getAttributes().getNamedItem("Cantidad").getNodeValue());
                                    cantidadTransportadaJson.put("IDOrigen", cantidadTransportadaList.item(x).getAttributes().getNamedItem("IDOrigen").getNodeValue());
                                    cantidadTransportadaJson.put("IDDestino", cantidadTransportadaList.item(x).getAttributes().getNamedItem("IDDestino").getNodeValue());
                                    mercancia.put("CantidadTransporta", cantidadTransportadaJson);

                                    mercanciaArray.add(mercancia);
                                }
                                cartaporte.put("mercancia", mercanciaArray);
                                NodeList autotransporteList = documentValue.getElementsByTagName("cartaporte20:Autotransporte");
                                JsonArray autotransporteArray = new JsonArray();
                                for (int x = 0; x < autotransporteList.getLength(); x++) {
                                    JsonObject autotransporteJson = new JsonObject();
                                    autotransporteJson.put("PermSCT", autotransporteList.item(x).getAttributes().getNamedItem("PermSCT").getNodeValue());
                                    autotransporteJson.put("NumPermisoSCT", autotransporteList.item(x).getAttributes().getNamedItem("NumPermisoSCT").getNodeValue());
                                    autotransporteJson.put("PermSCT", autotransporteList.item(x).getAttributes().getNamedItem("PermSCT").getNodeValue());

                                    NodeList identificacionVehicularList = documentValue.getElementsByTagName("cartaporte20:IdentificacionVehicular");
                                    JsonObject identificacionVehicularJson = new JsonObject();
                                    identificacionVehicularJson.put("ConfigVehicular", identificacionVehicularList.item(x).getAttributes().getNamedItem("ConfigVehicular").getNodeValue());
                                    identificacionVehicularJson.put("PlacaVM", identificacionVehicularList.item(x).getAttributes().getNamedItem("PlacaVM").getNodeValue());
                                    identificacionVehicularJson.put("AnioModeloVM", identificacionVehicularList.item(x).getAttributes().getNamedItem("AnioModeloVM").getNodeValue());
                                    autotransporteJson.put("IdentificacionVehicular", identificacionVehicularJson);
                                    NodeList segurosList = documentValue.getElementsByTagName("cartaporte20:Seguros");
                                    JsonObject segurosJson = new JsonObject();
                                    segurosJson.put("AseguraRespCivil", segurosList.item(x).getAttributes().getNamedItem("AseguraRespCivil").getNodeValue());
                                    segurosJson.put("PolizaRespCivil", segurosList.item(x).getAttributes().getNamedItem("PolizaRespCivil").getNodeValue());
                                    // segurosJson.put("AseguraMedAmbiente", segurosList.item(x).getAttributes().getNamedItem("AseguraMedAmbiente").getNodeValue());
                                    //  segurosJson.put("PolizaMedAmbiente", segurosList.item(x).getAttributes().getNamedItem("PolizaMedAmbiente").getNodeValue());
                                    autotransporteJson.put("Seguros", segurosJson);

                                    autotransporteArray.add(autotransporteJson);
                                }
                                cartaporte.put("autotransporte", autotransporteArray);

                                NodeList figuraTransporteList = documentValue.getElementsByTagName("cartaporte20:TiposFigura");
                                JsonArray figuraTransporteArray = new JsonArray();
                                for (int x = 0; x < figuraTransporteList.getLength(); x++) {
                                    JsonObject figuraTransporteJson = new JsonObject();
                                    figuraTransporteJson.put("TipoFigura", figuraTransporteList.item(x).getAttributes().getNamedItem("TipoFigura").getNodeValue());
                                    figuraTransporteJson.put("RFCFigura", figuraTransporteList.item(x).getAttributes().getNamedItem("RFCFigura").getNodeValue());
                                    figuraTransporteJson.put("NumLicencia", figuraTransporteList.item(x).getAttributes().getNamedItem("NumLicencia").getNodeValue());
                                    figuraTransporteJson.put("NombreFigura", figuraTransporteList.item(x).getAttributes().getNamedItem("NombreFigura").getNodeValue());
                                    figuraTransporteArray.add(figuraTransporteJson);
                                }
                                cartaporte.put("figuraTransporte", figuraTransporteArray);
                                valueCFDI.put("complementoCartaPorte", cartaporte);
                            }
                            resultParcel.put("timbre", valueCFDI);
                            response.add(resultParcel);
                        }
                        responseOk(context, response);
                    } catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, "erro en el formato base64(xml)", t);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });

        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);

        }
    }

    private void generateCCP(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, ShipmentsDBV.TRAVEL_LOGS_ID, 0);
            body.put(CREATED_BY, context.<Integer>get(USER_ID));

            // TODO: obtain all travel log related data
            DeliveryOptions optionComplementDetail = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.GET_COMPLEMENT_DETAIL);

            vertx.eventBus().send(ComplementLetterPorteDBV.class.getSimpleName(),body, optionComplementDetail, replyDetail -> {
               try{
                   if (replyDetail.failed()){
                       responseError(context, UNEXPECTED_ERROR, replyDetail.cause());
                   }
                   Message<Object> resultDetail = replyDetail.result();
                   JsonObject complementDetail = (JsonObject) resultDetail.body();
                   complementDetail.put("type", "travel_log");
                   complementDetail.put("ccp_element", body.getJsonObject("ccp_element"));

                   DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.REGISTER_TRAVEL_LOG_CCP);
                   vertx.eventBus().send(ComplementLetterPorteDBV.class.getSimpleName(), body, options, reply -> {
                       try {
                           if (reply.succeeded()){
                               Message<Object> result = reply.result();
                               JsonObject tlCCP = (JsonObject) result.body();
                               Integer tlCCPId = tlCCP.getInteger("travel_logs_ccp_id");

                               DeliveryOptions optionsRelatedUUID = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_GET_RELATED_UUID);
                               vertx.eventBus().send(ComplementLetterPorteDBV.class.getSimpleName(), complementDetail, optionsRelatedUUID, replyRelatedUUID -> {
                                    try {
                                        Message<Object> resultUUIDs = replyRelatedUUID.result();
                                        JsonObject resultUUIBody = (JsonObject) resultUUIDs.body();
                                        JsonArray uuids = resultUUIBody.getJsonArray("uuids");
                                        complementDetail.put("uuids", uuids);

                                        // GENERAR Y TIMBRAR CCP
                                        execService(context, ParcelInvoice.ACTION_CCP, complementDetail, cfdiResult -> {
                                            String xml = XML.toString(cfdiResult.getString("factura_timbrada"));
                                            JsonObject invoice = new JsonObject()
                                                    .put("travel_log_id", body.getInteger(ShipmentsDBV.TRAVEL_LOGS_ID))
                                                    .put("cfdiResult", cfdiResult)
                                                    .put("travel_logs_ccp_id", tlCCPId)
                                                    .put("xml", xml)
                                                    .put("type", "travel_log")
                                                    .put("uuid", cfdiResult.getString("uuid"));


                                            DeliveryOptions optionsUpdate = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.UPDATE_CCP_STATUS);
                                            vertx.eventBus().send(InvoiceDBV.class.getSimpleName(), invoice, optionsUpdate, replyUpdateInvoice -> {
                                                try {
                                                    String error = cfdiResult.getString("error");
                                                    if(error != null) {
                                                        JsonObject deleteBody = new JsonObject()
                                                                .put("travel_logs_ccp_id", tlCCPId)
                                                                .put("travel_log_id", body.getInteger(ShipmentsDBV.TRAVEL_LOGS_ID))
                                                                .put("type", "travel_log");

                                                        DeliveryOptions optionsDeleteInvoice = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.DELETE_CCP_WITH_ERROR);
                                                        vertx.eventBus().send(InvoiceDBV.class.getSimpleName(), deleteBody, optionsDeleteInvoice, repDeleteInvoice -> {
                                                            if (repDeleteInvoice.succeeded()) {
                                                                responseOk(context,
                                                                        new JsonObject()
                                                                                .put("Response", xml)
                                                                                .put("cadena_original", cfdiResult.getString("cadena_original"))
                                                                                .put("error", cfdiResult.getString("error"))
                                                                                .put("complement_details", complementDetail)
                                                                                .put("body_stamp", cfdiResult.getJsonObject("body_stamp"))
                                                                                .put("xml_to_stamp", cfdiResult.getString("xml_to_stamp"))
                                                                );
                                                            } else {
                                                                System.out.println(repDeleteInvoice.cause().getMessage());
                                                                responseError(context, UNEXPECTED_ERROR, repDeleteInvoice.cause().getMessage());
                                                            }
                                                        });
                                                    } else {
                                                        DeliveryOptions optionsUpdateTL = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_UPDATE_TL_STAMP_STATUS);
                                                        vertx.eventBus().send(ComplementLetterPorteDBV.class.getSimpleName(), invoice, optionsUpdateTL, replyUpdateTLStatus -> {
                                                            try {
                                                                if (replyUpdateTLStatus.succeeded()) {
                                                                    responseOk(context,
                                                                            new JsonObject()
                                                                                    .put("Response", xml)
                                                                                    .put("cadena_original", cfdiResult.getString("cadena_original"))
                                                                                    .put("error", cfdiResult.getString("error"))
                                                                                    .put("travel_logs_ccp_id", tlCCPId));
                                                                } else {
                                                                    System.out.println(replyUpdateTLStatus.cause().getMessage());
                                                                    responseError(context, UNEXPECTED_ERROR, replyUpdateTLStatus.cause().getMessage());
                                                                }
                                                            } catch (Exception e) {
                                                                responseError(context, UNEXPECTED_ERROR, e.getCause());
                                                            }
                                                        });
                                                    }
                                                } catch (Exception e) {
                                                    responseError(context, UNEXPECTED_ERROR, e.getCause());
                                                }
                                            });
                                        });
                                    } catch (Exception e) {
                                        System.out.println(replyRelatedUUID.cause().getMessage());
                                        responseError(context, UNEXPECTED_ERROR, e.getCause());
                                    }
                               });
                           } else {
                               responseError(context, UNEXPECTED_ERROR, reply.cause());
                           }
                       } catch (Exception e) {
                           responseError(context, UNEXPECTED_ERROR, e.getCause());
                       }
                   });

               }  catch (Exception e) {
                   responseError(context, UNEXPECTED_ERROR, e.getCause());
               }
            });
        } catch (UtilsValidation.PropertyValueException e){
            responseError(context, e);
        } catch (Exception e){
            responseError(context, "Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void getCCPXML(RoutingContext context) {
        Integer id = Integer.valueOf(context.request().getParam("id"));
        String ccpType = context.request().getParam("ccp_type");
        JsonObject params = new JsonObject().put("id", id).put("ccp_type", ccpType);
        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_GET_CCP_XML_INFO);
            vertx.eventBus().send(this.getDBAddress(), params, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    try {
                        JsonArray resultData = (JsonArray) reply.result().body();
                        JsonArray xmlArray = new JsonArray();

                        for (int i = 0; i < resultData.size(); i++) {
                            JsonObject resultCCP = resultData.getJsonObject(i);
                            CcpXMLToJsonConverter converter = new CcpXMLToJsonConverter();
                            JsonObject XMLObject = converter.convertXMLToJSON(resultCCP.getString("xml"));
                            String totalLetra = UtilsMoney.numberToLetter(XMLObject.getJsonObject("Comprobante").getString("Total"));
                            XMLObject.put("xml", resultCCP.getString("xml"));
                            XMLObject.getJsonObject("Comprobante").put("totalLetra", totalLetra);
                            xmlArray.add(XMLObject);
                        }
                        responseOk(context, new JsonObject().put("xml_ccps", xmlArray));
                    } catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, "error al leer XML de la carta porte", t);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });
        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
        }
    }

    private void generateCCPEadRad(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body.getJsonObject("data"), "id", 0);
            body.getJsonObject("data").put(CREATED_BY, context.<Integer>get(USER_ID));

            DeliveryOptions optionComplementDetail = new DeliveryOptions().addHeader(ACTION, EadRadDBV.ACTION_GET_MANIFEST_ALL_PACKAGES);

            vertx.eventBus().send(EadRadDBV.class.getSimpleName(), body, optionComplementDetail, replyDetail -> {
                try{
                    if (replyDetail.failed()){
                        responseError(context, UNEXPECTED_ERROR, replyDetail.cause());
                    }
                    Message<Object> resultDetail = replyDetail.result();
                    JsonArray details = (JsonArray) resultDetail.body();
                    if(!details.isEmpty()) {
                        JsonArray packages = details.getJsonArray(0);
                        JsonObject body_cfdi = new JsonObject().put("type", "EAD/RAD").put("packages", packages);

                        DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_REGISTER_EAD_RAD_CCP);
                        vertx.eventBus().send(ComplementLetterPorteDBV.class.getSimpleName(), body.getJsonObject("data"), options, reply -> {
                            try {
                                if (reply.succeeded()){
                                    Message<Object> result = reply.result();
                                    JsonObject ccp = (JsonObject) result.body();
                                    Integer pmCCPId = ccp.getInteger("parcels_manifest_ccp_id");
                                    // GENERAR Y TIMBRAR CCP
                                    execService(context, ParcelInvoice.ACTION_CCP, body_cfdi, cfdiResult -> {
                                        String xml = XML.toString(cfdiResult.getString("factura_timbrada"));
                                        JsonObject invoice = new JsonObject()
                                                .put("parcels_manifest_id", body.getInteger("id"))
                                                .put("cfdiResult", cfdiResult)
                                                .put("parcels_manifest_ccp_id", pmCCPId)
                                                .put("xml", xml)
                                                .put("type", "EAD/RAD");

                                        DeliveryOptions optionsUpdate = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.UPDATE_CCP_STATUS);
                                        vertx.eventBus().send(InvoiceDBV.class.getSimpleName(), invoice, optionsUpdate, replyUpdateInvoice -> {
                                            try {
                                                String error = cfdiResult.getString("error");
                                                if(error != null) {
                                                    JsonObject deleteBody = new JsonObject()
                                                            .put("parcels_manifest_ccp_id", pmCCPId)
                                                            .put("parcels_manifest_id", body.getJsonObject("data").getInteger("id"))
                                                            .put("type", "EAD/RAD");

                                                    DeliveryOptions optionsDeleteInvoice = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.DELETE_CCP_WITH_ERROR);
                                                    vertx.eventBus().send(InvoiceDBV.class.getSimpleName(), deleteBody, optionsDeleteInvoice, repDeleteInvoice -> {
                                                        if (repDeleteInvoice.succeeded()) {
                                                            responseOk(context,
                                                                    new JsonObject()
                                                                            .put("Response", xml)
                                                                            .put("cadena_original", cfdiResult.getString("cadena_original"))
                                                                            .put("error", cfdiResult.getString("error")));
                                                        } else {
                                                            System.out.println(repDeleteInvoice.cause().getMessage());
                                                            responseError(context, UNEXPECTED_ERROR, repDeleteInvoice.cause().getMessage());
                                                        }
                                                    });
                                                } else {
                                                    responseOk(context,
                                                            new JsonObject()
                                                                    .put("Response", xml)
                                                                    .put("cadena_original", cfdiResult.getString("cadena_original"))
                                                                    .put("error", cfdiResult.getString("error"))
                                                                    .put("parcels_manifest_ccp_id", pmCCPId));
                                                }
                                            } catch (Exception e) {
                                                responseError(context, UNEXPECTED_ERROR, e.getCause());
                                            }
                                        });
                                    });
                                } else {
                                    responseError(context, UNEXPECTED_ERROR, reply.cause());
                                }
                            } catch (Exception e) {
                                responseError(context, UNEXPECTED_ERROR, e.getCause());
                            }
                        });
                    } else {
                        responseError(context, UNEXPECTED_ERROR, "No se encontraron paquetes en la bitácora");
                    }
                }  catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getCause());
                }
            });
        } catch (UtilsValidation.PropertyValueException e){
            responseError(context, e);
        } catch (Exception e){
            responseError(context, "Ocurrió un error inesperado",e.getMessage());
        }
    }

    private void getManifestCCPXML(RoutingContext context) {
        Integer pmId = Integer.valueOf(context.request().getParam("parcels_manifest_id"));

        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_GET_MANIFEST_CCP_XML_INFO);
            vertx.eventBus().send(this.getDBAddress(), new JsonObject().put("parcels_manifest_id", pmId), options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    JsonObject resultData = new JsonObject();
                    resultData.put("letterPorteData",reply.result().body());
                    try {
                        JsonObject resultCCP = resultData.getJsonArray("letterPorteData").getJsonObject(0);
                        CcpXMLToJsonConverter converter = new CcpXMLToJsonConverter();
                        JsonObject XMLObject = converter.convertXMLToJSON(resultCCP.getString("xml"));
                        String totalLetra = UtilsMoney.numberToLetter(XMLObject.getJsonObject("Comprobante").getString("Total"));
                        XMLObject.put("xml", resultCCP.getString("xml"));
                        XMLObject.getJsonObject("Comprobante").put("totalLetra", totalLetra);
                        responseOk(context, new JsonObject().put("xml_ccp", XMLObject));
                    } catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, "error al leer XML de la bitacora EAD/RAD", t);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", reply.cause().getMessage());
                }
            });
        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
        }
    }

    private void getTravelLogCCPs(RoutingContext context) {
        Integer tlId = Integer.valueOf(context.request().getParam("travel_log_id"));

        try {
            DeliveryOptions optionComplementDetail = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.GET_COMPLEMENT_DETAIL);
            vertx.eventBus().send(ComplementLetterPorteDBV.class.getSimpleName(), new JsonObject().put("travel_logs_id", tlId), optionComplementDetail, replyDetail -> {
                try{
                    if(replyDetail.failed()){
                        throw new Exception(replyDetail.cause());
                    }

                    Message<Object> resultDetail = replyDetail.result();
                    JsonObject complementDetail = (JsonObject) resultDetail.body();

                    DeliveryOptions optionsCCP = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_GET_TRAVEL_LOG_CCPS);
                    vertx.eventBus().send(this.getDBAddress(), complementDetail, optionsCCP, repTLccps -> {
                        if (repTLccps.succeeded()) {
                            Message<Object> resultCCPs = repTLccps.result();
                            JsonObject ccps = (JsonObject) resultCCPs.body();
                            responseOk(context, ccps);
                        } else {
                            System.out.println(repTLccps.cause().getMessage());
                            responseError(context, UNEXPECTED_ERROR, repTLccps.cause().getMessage());
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                    responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", e.getMessage());
                }
            });
        } catch (Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
        }
    }

    private void updateCCPisStamped(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, "travel_log_id", 0);
            body.put("is_stamped", true);
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, ComplementLetterPorteDBV.ACTION_UPDATE_TL_STAMP_STATUS);

            vertx.eventBus().send(ComplementLetterPorteDBV.class.getSimpleName(), body, options, reply -> {
                try {
                    if (reply.failed()){
                        responseError(context, UNEXPECTED_ERROR, reply.cause());
                    }
                    responseOk(context, new JsonObject());
                }  catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getCause());
                }
            });
        } catch(Exception ex) {
            responseError(context, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", ex);
        }
    }

    private JsonObject extractDataFromXMLObj(JsonObject xmlObj) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            String xmlBase64 = xmlObj.getString("xml");
            byte[] decodedXML = Base64.getDecoder().decode(xmlBase64.getBytes(StandardCharsets.UTF_8));
            InputStream XML = new ByteArrayInputStream(decodedXML);

            Document documentValue = dBuilder.parse(XML);
            JsonObject valueCFDI = new JsonObject();
            valueCFDI.put("Serie", documentValue.getDocumentElement().getAttribute("Serie"));
            valueCFDI.put("Version", documentValue.getDocumentElement().getAttribute("Version"));
            valueCFDI.put("Sello", documentValue.getDocumentElement().getAttribute("Sello"));
            valueCFDI.put("Fecha", documentValue.getDocumentElement().getAttribute("Fecha"));
            valueCFDI.put("Folio", documentValue.getDocumentElement().getAttribute("Folio"));
            valueCFDI.put("NoCertificado", documentValue.getDocumentElement().getAttribute("NoCertificado"));
            valueCFDI.put("Certificado", documentValue.getDocumentElement().getAttribute("Certificado"));

            valueCFDI.put("SubTotal", documentValue.getDocumentElement().getAttribute("SubTotal"));
            valueCFDI.put("Moneda", documentValue.getDocumentElement().getAttribute("Moneda"));
            valueCFDI.put("FormaPago", documentValue.getDocumentElement().getAttribute("FormaPago"));
            valueCFDI.put("MetodoPago", documentValue.getDocumentElement().getAttribute("MetodoPago"));
            valueCFDI.put("Total", documentValue.getDocumentElement().getAttribute("Total"));
            String totalLetra = UtilsMoney.numberToLetter(documentValue.getDocumentElement().getAttribute("Total"));
            valueCFDI.put("totalLetra", totalLetra);
            valueCFDI.put("TipoDeComprobante", documentValue.getDocumentElement().getAttribute("TipoDeComprobante"));
            valueCFDI.put("LugarExpedicion", documentValue.getDocumentElement().getAttribute("LugarExpedicion"));
            NodeList emisor = documentValue.getElementsByTagName("cfdi:Emisor");
            valueCFDI.put("EmisorRfc", emisor.item(0).getAttributes().getNamedItem("Rfc").getNodeValue());
            valueCFDI.put("EmisorNombre", emisor.item(0).getAttributes().getNamedItem("Nombre").getNodeValue());
            valueCFDI.put("EmisorRegimenFiscal", emisor.item(0).getAttributes().getNamedItem("RegimenFiscal").getNodeValue());
            NodeList receptor = documentValue.getElementsByTagName("cfdi:Receptor");
            valueCFDI.put("ReceptorRfc", receptor.item(0).getAttributes().getNamedItem("Rfc").getNodeValue());
            valueCFDI.put("ReceptorNombre", receptor.item(0).getAttributes().getNamedItem("Nombre").getNodeValue());
            valueCFDI.put("ReceptorUsoCFDI", receptor.item(0).getAttributes().getNamedItem("UsoCFDI").getNodeValue());
            valueCFDI.put("DomicilioFiscalReceptor", receptor.item(0).getAttributes().getNamedItem("DomicilioFiscalReceptor").getNodeValue());
            valueCFDI.put("RegimenFiscalReceptor", receptor.item(0).getAttributes().getNamedItem("RegimenFiscalReceptor").getNodeValue());
            NodeList Conceptos = documentValue.getElementsByTagName("cfdi:Concepto");
            JsonArray arrayConcepto = new JsonArray();
            for (int x = 0; x < Conceptos.getLength(); x++) {
                JsonObject ConceptosJSON = new JsonObject();
                ConceptosJSON.put("ClaveProdServ", Conceptos.item(x).getAttributes().getNamedItem("ClaveProdServ").getNodeValue());
                ConceptosJSON.put("NoIdentificacion", Conceptos.item(x).getAttributes().getNamedItem("NoIdentificacion").getNodeValue());
                ConceptosJSON.put("Descripcion", Conceptos.item(x).getAttributes().getNamedItem("Descripcion").getNodeValue());
                ConceptosJSON.put("Cantidad", Conceptos.item(x).getAttributes().getNamedItem("Cantidad").getNodeValue());
                ConceptosJSON.put("ClaveUnidad", Conceptos.item(x).getAttributes().getNamedItem("ClaveUnidad").getNodeValue());
                ConceptosJSON.put("Unidad", Conceptos.item(x).getAttributes().getNamedItem("Unidad").getNodeValue());
                ConceptosJSON.put("ValorUnitario", Conceptos.item(x).getAttributes().getNamedItem("ValorUnitario").getNodeValue());
                ConceptosJSON.put("Importe", Conceptos.item(x).getAttributes().getNamedItem("Importe").getNodeValue());
                arrayConcepto.add(ConceptosJSON);
            }
            NodeList Traslados = documentValue.getElementsByTagName("cfdi:Traslado");
            valueCFDI.put("TasaOCuota", Traslados.item(0).getAttributes().getNamedItem("TasaOCuota").getNodeValue());
            NodeList impuestosList = documentValue.getElementsByTagName("cfdi:Impuestos");
            if (impuestosList.getLength() > 0) {
                Element impuestosElement = (Element) impuestosList.item(impuestosList.getLength() - 1);
                valueCFDI.put("ImporteImpuesto", impuestosElement.getAttribute("TotalImpuestosTrasladados"));

                if (impuestosElement.hasAttribute("TotalImpuestosRetenidos")) {
                    valueCFDI.put("ImporteImpuestoRetenido", impuestosElement.getAttribute("TotalImpuestosRetenidos"));
                }
            }

            valueCFDI.put("Concepto", arrayConcepto);

            // UUID RELACIONADOS
            NodeList cfdiRelacionadosList = documentValue.getElementsByTagName("cfdi:CfdiRelacionados");
            if (cfdiRelacionadosList.getLength() > 0) {
                Element cfdiRelacionadosElement = (Element) cfdiRelacionadosList.item(0);
                JsonObject cfdiRelacionadosJson = new JsonObject();

                String tipoRelacion = cfdiRelacionadosElement.getAttribute("TipoRelacion");
                cfdiRelacionadosJson.put("TipoRelacion", tipoRelacion);

                NodeList cfdiRelacionadoList = cfdiRelacionadosElement.getElementsByTagName("cfdi:CfdiRelacionado");
                JsonArray relacionadosArray = new JsonArray();

                for (int z = 0; z < cfdiRelacionadoList.getLength(); z++) {
                    Element cfdiRelacionadoElement = (Element) cfdiRelacionadoList.item(z);
                    JsonObject relacionadoJson = new JsonObject();
                    String uuid = cfdiRelacionadoElement.getAttribute("UUID");
                    relacionadoJson.put("UUID", uuid);
                    relacionadosArray.add(relacionadoJson);
                }

                cfdiRelacionadosJson.put("CfdiRelacionado", relacionadosArray);
                valueCFDI.put("CfdiRelacionados", cfdiRelacionadosJson);
            }

            NodeList TimbreFiscalDigital = documentValue.getElementsByTagName("tfd:TimbreFiscalDigital");
            JsonObject TimbreFiscalDigitalJSON = new JsonObject();
            TimbreFiscalDigitalJSON.put("FechaTimbrado", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("FechaTimbrado").getNodeValue());
            TimbreFiscalDigitalJSON.put("RfcProvCertif", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("RfcProvCertif").getNodeValue());
            TimbreFiscalDigitalJSON.put("SelloCFD", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("SelloCFD").getNodeValue());
            TimbreFiscalDigitalJSON.put("NoCertificadoSAT", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("NoCertificadoSAT").getNodeValue());
            TimbreFiscalDigitalJSON.put("SelloSAT", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("SelloSAT").getNodeValue());
            TimbreFiscalDigitalJSON.put("NoCertificadoSAT", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("NoCertificadoSAT").getNodeValue());
            TimbreFiscalDigitalJSON.put("NoCertificadoSAT", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("NoCertificadoSAT").getNodeValue());
            TimbreFiscalDigitalJSON.put("UUID", TimbreFiscalDigital.item(0).getAttributes().getNamedItem("UUID").getNodeValue());
            valueCFDI.put("TimbreFiscalDigital", TimbreFiscalDigitalJSON);

            return valueCFDI;
        } catch (Throwable t) {
            t.printStackTrace();
            return new JsonObject();
        }
    }
}

