package database.invoicing.xml;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.Normalizer;
import java.util.concurrent.CompletableFuture;

public class CfdiXmlUpdater {
    private CfdiXmlUpdater() {
    }

    public static CompletableFuture<Boolean> updateXML(JsonObject cfdi, String file, String cfdiType) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final boolean isGlobal = "global".equalsIgnoreCase(cfdiType);
        final boolean isIngreso = "ingreso".equalsIgnoreCase(cfdiType);
        final boolean isEgreso = "egreso".equalsIgnoreCase(cfdiType);

        String cfdi_normal = cfdi.getJsonObject("cfdi_body").toString();
        String serieCFDI = cfdi.getJsonObject("cfdi_body").getString("Serie");
        Integer folio;
        if (isGlobal) {
            folio = cfdi.getJsonObject("cfdi_body").getInteger("Folio");
            cfdi_normal = Normalizer.normalize(cfdi_normal, Normalizer.Form.NFD);
            String regex = "\\bAño\\b|[^\\p{ASCII}&&[^ñA-Za-z0-9\\s]]+";
            cfdi_normal = cfdi_normal.replaceAll(regex, "$0");
        } else { // ingreso y egreso
            folio = cfdi.getInteger("folio");
        }

        cfdi.remove("cfdi_body");
        cfdi.put("cfdi_body", new JsonObject(cfdi_normal));
        JsonObject body = cfdi.getJsonObject("cfdi_body");
        String invoice_date = body.getString("Fecha");
        JsonObject emisor = body.getJsonObject("Emisor");
        JsonObject receptor = body.getJsonObject("Receptor");
        JsonArray conceptos = body.getJsonArray("Conceptos");
        JsonObject impuestos = body.getJsonObject("Impuestos");
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
            Node globalInfoNode = doc.getElementsByTagName("cfdi:InformacionGlobal").item(0);

            if (isGlobal) {
                // ------- ACTUALIZAR INFORMACION GLOBAL --------
                NamedNodeMap gi = globalInfoNode.getAttributes();
                Node periodicidad = gi.getNamedItem("Periodicidad");
                Node meses = gi.getNamedItem("Meses");
                periodicidad.setTextContent(body.getString("Periodicidad"));
                meses.setTextContent(body.getString("Meses"));
                Element globalInfoElement = (Element) globalInfoNode;
                globalInfoElement.removeAttribute("A?o");
                globalInfoElement.setAttribute("Año", body.getString("Año"));
            } else {
                NodeList nodeList = doc.getElementsByTagName("cfdi:InformacionGlobal");
                Element informacionGlobal = (Element) nodeList.item(0);
                Node parent = informacionGlobal.getParentNode();
                parent.removeChild(informacionGlobal);
            }

            //----- CFDI RELACIONADOS
            if (cfdiRelacionados != null && !cfdiRelacionados.isEmpty()) {
                JsonArray cfdiRelacionadosArray = cfdiRelacionados.getJsonArray("relacionados");
                if (!cfdiRelacionadosArray.isEmpty()) {
                    Element cfdiRelacionadosE = doc.createElement("cfdi:CfdiRelacionados");
                    cfdiRelacionadosE.setAttribute("TipoRelacion", cfdiRelacionados.getString("tipoRelacion"));
                    for (int i = 0; i < cfdiRelacionadosArray.size(); i++) {
                        Element cfdiRelacionado = doc.createElement("cfdi:CfdiRelacionado");
                        JsonObject el = cfdiRelacionadosArray.getJsonObject(i);
                        cfdiRelacionado.setAttribute("UUID", el.getString("UUID"));
                        cfdiRelacionadosE.appendChild(cfdiRelacionado);
                    }
                    comprobante.insertBefore(cfdiRelacionadosE, emisorNode);
                }
            }

            Element itemImpuesto = (Element) doc.getElementsByTagName("cfdi:Impuestos").item(0);
            // ACTUALIZAR CONCEPTOS
            Element itemNode = (Element) doc.getElementsByTagName("cfdi:Conceptos").item(0);
            for (int i = 0; i < conceptos.size(); i++) {
                Element concepto = doc.createElement("cfdi:Concepto");
                JsonObject el = conceptos.getJsonObject(i);
                JsonObject ivaTrasladado = el.getJsonObject("Impuestos").getJsonObject("Traslados");
                if(isEgreso) {
                    concepto.setAttribute("Cantidad", el.getString("Cantidad"));
                    concepto.setAttribute("ClaveProdServ", el.getString("ClaveProdServ"));
                    concepto.setAttribute("ClaveUnidad", el.getString("ClaveUnidad"));
                    concepto.setAttribute("Unidad", el.getString("Unidad"));
                    concepto.setAttribute("NoIdentificacion", el.getString("NoIdentificacion"));
                    concepto.setAttribute("Descripcion", el.getString("Descripcion"));
                    concepto.setAttribute("Importe", el.getString("Importe"));
                    concepto.setAttribute("ValorUnitario", el.getString("ValorUnitario"));
                    concepto.setAttribute("ObjetoImp", el.getString("ObjetoImp"));
                } else {
                    concepto.setAttribute("Cantidad", el.getString("Cantidad"));
                    concepto.setAttribute("ClaveProdServ", el.getString("ClaveProdServ"));
                    concepto.setAttribute("ClaveUnidad", el.getString("Unidad"));
                    concepto.setAttribute("Unidad", el.getString("Unidad"));
                    concepto.setAttribute("Descripcion", el.getString("Descripcion"));
                    concepto.setAttribute("Importe", el.getString("ValorUnitario"));
                    concepto.setAttribute("NoIdentificacion", el.getString("NoIdentificacion"));
                    concepto.setAttribute("ValorUnitario", el.getString("ValorUnitario"));
                    concepto.setAttribute("ObjetoImp", "02");
                }

                Double descuento = Double.parseDouble(el.getString("Descuento"));
                if (descuento > 0) {
                    concepto.setAttribute("Descuento", el.getString("Descuento"));
                }
                Element Impuestos = doc.createElement("cfdi:Impuestos");
                Element Traslados = doc.createElement("cfdi:Traslados");
                Element traslado = doc.createElement("cfdi:Traslado");
                traslado.setAttribute("Importe", ivaTrasladado.getString("Importe"));
                traslado.setAttribute("TipoFactor", ivaTrasladado.getString("TipoFactor"));
                traslado.setAttribute("TasaOCuota", ivaTrasladado.getString("TasaOCuota"));
                traslado.setAttribute("Impuesto", ivaTrasladado.getString("Impuesto"));
                traslado.setAttribute("Base", ivaTrasladado.getString("Base"));
                Traslados.appendChild(traslado);
                Impuestos.appendChild(Traslados);

                JsonObject ivaRetenido = el.getJsonObject("Impuestos").getJsonObject("Retenciones");
                if (ivaRetenido != null) {
                    Element Retenciones = doc.createElement("cfdi:Retenciones");
                    Element retencion = doc.createElement("cfdi:Retencion");
                    retencion.setAttribute("Importe", ivaRetenido.getString("Importe"));
                    retencion.setAttribute("TipoFactor", ivaRetenido.getString("TipoFactor"));
                    retencion.setAttribute("TasaOCuota", ivaRetenido.getString("TasaOCuota"));
                    retencion.setAttribute("Impuesto", ivaRetenido.getString("Impuesto"));
                    retencion.setAttribute("Base", ivaRetenido.getString("Base"));
                    Retenciones.appendChild(retencion);
                    Impuestos.appendChild(Retenciones);
                }


                concepto.appendChild(Impuestos);
                itemNode.appendChild(concepto);
            }

            // --- ACTUALIZAR IMPUESTOS---

            // RETENCIONES
            JsonObject retencion = impuestos.getJsonObject("Retencion");
            if (retencion != null) {
                itemImpuesto.setAttribute("TotalImpuestosRetenidos", impuestos.getString("TotalImpuestosRetenidos"));
                Element retencionesE = doc.createElement("cfdi:Retenciones");
                Element retencionE = doc.createElement("cfdi:Retencion");
                retencionE.setAttribute("Impuesto", retencion.getString("Impuesto"));
                retencionE.setAttribute("Importe", retencion.getString("Importe"));
                retencionesE.appendChild(retencionE);
                itemImpuesto.appendChild(retencionesE);
            }

            // TRASLADOS
            JsonObject traslado = impuestos.getJsonObject("Traslado");
            itemImpuesto.setAttribute("TotalImpuestosTrasladados", impuestos.getString("TotalImpuestosTrasladados"));
            Element trasladosImpuesto = doc.createElement("cfdi:Traslados");
            Element trasladoImpuesto = doc.createElement("cfdi:Traslado");
            trasladoImpuesto.setAttribute("Importe", traslado.getString("Importe"));
            trasladoImpuesto.setAttribute("TipoFactor", traslado.getString("TipoFactor"));
            trasladoImpuesto.setAttribute("TasaOCuota", traslado.getString("TasaOCuota"));
            trasladoImpuesto.setAttribute("Impuesto", traslado.getString("Impuesto"));
            trasladoImpuesto.setAttribute("Base", traslado.getString("Base"));
            trasladosImpuesto.appendChild(trasladoImpuesto);
            itemImpuesto.appendChild(trasladosImpuesto);

            //--------------- FIGURA TRANSPORTE-----------------------

            NamedNodeMap attr = comprobante.getAttributes();
            NamedNodeMap attrEmisor = emisorNode.getAttributes();
            NamedNodeMap attrReceptor = receptorNode.getAttributes();

            Double descuentoCantidad = Double.parseDouble(body.getString("Descuento"));
            if (descuentoCantidad == 0) {
                attr.removeNamedItem("Descuento");
            } else {
                Node descuento = attr.getNamedItem("Descuento");
                descuento.setTextContent(body.getString("Descuento"));
            }

            Node lugarExpedicion = attr.getNamedItem("LugarExpedicion");
            lugarExpedicion.setTextContent(body.getString("LugarExpedicion"));
            Node folioCfdi = attr.getNamedItem("Folio");
            folioCfdi.setTextContent(folio.toString());
            Node serie = attr.getNamedItem("Serie");
            serie.setTextContent(serieCFDI);
            Node total = attr.getNamedItem("Total");
            total.setTextContent(body.getString("Total"));
            Node moneda = attr.getNamedItem("Moneda");
            moneda.setTextContent(body.getString("Moneda"));
            Node subTotal = attr.getNamedItem("SubTotal");
            subTotal.setTextContent(body.getString("Subtotal"));
            Node formaPago = attr.getNamedItem("FormaPago");
            if (body.getString("FormaPago") == null) {
                formaPago.setTextContent("01");
            } else {
                formaPago.setTextContent(body.getString("FormaPago"));
            }

            if (body.getString("MetodoPago") != null) {
                Node metodoPago = attr.getNamedItem("MetodoPago");
                metodoPago.setTextContent(body.getString("MetodoPago"));
            }


            Node tipoDeComprobante = attr.getNamedItem("TipoDeComprobante");
            tipoDeComprobante.setTextContent(body.getString("TipoDeComprobante"));
            //**********************EL SELLO SE GENERA DE FORMA DINAMICA DESDE EL PROCESO**********************
            Node sello = attr.getNamedItem("Sello");
            //TODO AQUI SE GENERA EL SELLO NUEVO
            sello.setTextContent("sello");
            //**********************EL SELLO SE GENERA DE FORMA DINAMICA DESDE EL PROCESO**********************

            //Obtener la fecha actual y remplazarla en el atributo fecha
            Node fecha = attr.getNamedItem("Fecha");
            fecha.setTextContent(invoice_date);

            //-------ACTUALIZAR BASE--------
            //-------ACTUALIZAR EMISOR------
            Node emisorRFC = attrEmisor.getNamedItem("Rfc");
            emisorRFC.setTextContent(emisor.getString("Rfc").toUpperCase());
            Node emisorNombre = attrEmisor.getNamedItem("Nombre");
            emisorNombre.setTextContent(emisor.getString("Nombre").toUpperCase());
            Node emisorRegimenFiscal = attrEmisor.getNamedItem("RegimenFiscal");
            emisorRegimenFiscal.setTextContent(emisor.getString("RegimenFiscal"));
            //-------ACTUALIZAR EMISOR------
            //-------ACTUALIZAR RECEPTOR ----
            Node receptorNombre = attrReceptor.getNamedItem("Nombre");
            receptorNombre.setTextContent(receptor.getString("Nombre").toUpperCase().trim());
            Node receptorRfc = attrReceptor.getNamedItem("Rfc");
            receptorRfc.setTextContent(receptor.getString("Rfc").toUpperCase());
            Node receptorUsoCFDI = attrReceptor.getNamedItem("UsoCFDI");
            receptorUsoCFDI.setTextContent(receptor.getString("UsoCFDI"));
            Node receptorRegimenFiscal = attrReceptor.getNamedItem("RegimenFiscalReceptor");
            receptorRegimenFiscal.setTextContent(receptor.getString("RegimenFiscalReceptor"));
            Node receptorDomicilio = attrReceptor.getNamedItem("DomicilioFiscalReceptor");
            if (isGlobal) {
                receptorDomicilio.setTextContent(body.getString("LugarExpedicion"));
            } else {
                receptorDomicilio.setTextContent(receptor.getString("DomicilioFiscalReceptor").toString());
            }
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            DOMSource src = new DOMSource((Node) doc);
            StreamResult res = new StreamResult(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            transformer.transform(src, res);
            future.complete(true);
        } catch (Exception e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }
        return future;
    }

    public static String extractAttributeFromXML(org.w3c.dom.Document doc, String tagName, String attributeName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes != null && nodes.getLength() > 0 && nodes.item(0) instanceof Element) {
            return ((Element) nodes.item(0)).getAttribute(attributeName);
        }
        return null;
    }
}
