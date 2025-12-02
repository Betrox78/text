package utils;

        import io.vertx.core.json.JsonArray;
        import io.vertx.core.json.JsonObject;
        import org.w3c.dom.*;

        import javax.xml.parsers.DocumentBuilder;
        import javax.xml.parsers.DocumentBuilderFactory;
        import java.io.ByteArrayInputStream;
        import java.io.InputStream;
        import java.nio.charset.StandardCharsets;
        import java.util.*;
        import java.util.Base64;

public class GenericCFDIToJsonConverter {

    public JsonObject convertBase64ToJSON(String xmlBase64, boolean includeTotalLetra) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(xmlBase64.getBytes(StandardCharsets.UTF_8));
        return convertStreamToJSON(new ByteArrayInputStream(decoded), includeTotalLetra);
    }

    public JsonObject convertXmlStringToJSON(String xmlString, boolean includeTotalLetra) throws Exception {
        return convertStreamToJSON(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)), includeTotalLetra);
    }


    private JsonObject convertStreamToJSON(InputStream xmlStream, boolean includeTotalLetra) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlStream);
        Element root = doc.getDocumentElement(); // cfdi:Comprobante

        if (!removeNs(root.getTagName()).equals("Comprobante")) {
            throw new IllegalArgumentException("El XML no parece ser un CFDI v4.0 (raíz != Comprobante)");
        }

        JsonObject out = new JsonObject();
        JsonObject comprobante = new JsonObject();

        putAllAttributes(root, comprobante);

        if (includeTotalLetra) {
            String totalStr = comprobante.getString("Total");
            if (totalStr != null) {
                try {
                    String totalLetra = UtilsMoney.numberToLetter(totalStr);
                    comprobante.put("totalLetra", totalLetra);
                } catch (Throwable ignore) {}
            }
        }

        Element emisor = firstChildByLocal(root, "Emisor");
        if (emisor != null) {
            JsonObject emisorJson = new JsonObject();
            putAllAttributes(emisor, emisorJson);
            comprobante.put("Emisor", emisorJson);
        }

        Element receptor = firstChildByLocal(root, "Receptor");
        if (receptor != null) {
            JsonObject receptorJson = new JsonObject();
            putAllAttributes(receptor, receptorJson);
            comprobante.put("Receptor", receptorJson);
        }

        Element rels = firstChildByLocal(root, "CfdiRelacionados");
        if (rels != null) {
            JsonObject relsJson = new JsonObject();
            putAllAttributes(rels, relsJson);

            List<Element> relacionados = childrenByLocal(rels, "CfdiRelacionado");
            JsonArray arr = new JsonArray();
            for (Element e : relacionados) {
                JsonObject r = new JsonObject();
                putAllAttributes(e, r);
                arr.add(r);
            }
            relsJson.put("CfdiRelacionado", arr);
            comprobante.put("CfdiRelacionados", relsJson);
        }

        Element conceptos = firstChildByLocal(root, "Conceptos");
        if (conceptos != null) {
            List<Element> conceptoList = childrenByLocal(conceptos, "Concepto");
            JsonArray conceptosArr = new JsonArray();
            for (Element c : conceptoList) {
                JsonObject cJson = new JsonObject();
                putAllAttributes(c, cJson);

                Element impuestosC = firstChildByLocal(c, "Impuestos");
                if (impuestosC != null) {
                    cJson.put("Impuestos", impuestosToJson(impuestosC));
                }
                conceptosArr.add(cJson);
            }
            comprobante.put("Conceptos", conceptosArr);
        }

        Element impuestos = firstChildByLocal(root, "Impuestos");
        if (impuestos != null) {
            comprobante.put("Impuestos", impuestosToJson(impuestos));
        }

        Element complemento = firstChildByLocal(root, "Complemento");
        if (complemento != null) {
            JsonObject compJson = new JsonObject();

            Element timbre = firstChildByLocal(complemento, "TimbreFiscalDigital"); // tfd:...
            if (timbre != null) {
                JsonObject tfdJson = new JsonObject();
                putAllAttributes(timbre, tfdJson);
                compJson.put("TimbreFiscalDigital", tfdJson);
            }
            comprobante.put("Complemento", compJson);
        }

        out.put("Comprobante", comprobante);
        return out;
    }

    private JsonObject impuestosToJson(Element impuestosEl) {
        JsonObject impJson = new JsonObject();
        putAllAttributes(impuestosEl, impJson);

        Element trasladosEl = firstChildByLocal(impuestosEl, "Traslados");
        if (trasladosEl != null) {
            List<Element> traslados = childrenByLocal(trasladosEl, "Traslado");
            JsonArray arr = new JsonArray();
            for (Element t : traslados) {
                JsonObject tJson = new JsonObject();
                putAllAttributes(t, tJson);
                arr.add(tJson);
            }
            impJson.put("Traslados", arr);
        }

        Element retEl = firstChildByLocal(impuestosEl, "Retenciones");
        if (retEl != null) {
            List<Element> retenciones = childrenByLocal(retEl, "Retencion");
            JsonArray arr = new JsonArray();
            for (Element r : retenciones) {
                JsonObject rJson = new JsonObject();
                putAllAttributes(r, rJson);
                arr.add(rJson);
            }
            impJson.put("Retenciones", arr);
        }

        return impJson;
    }

    private static String removeNs(String qName) {
        int idx = qName.indexOf(':');
        return idx >= 0 ? qName.substring(idx + 1) : qName;
    }

    private static void putAllAttributes(Element el, JsonObject json) {
        NamedNodeMap atts = el.getAttributes();
        for (int i = 0; i < atts.getLength(); i++) {
            Node a = atts.item(i);
            json.put(removeNs(a.getNodeName()), a.getNodeValue());
        }
    }

    private static Element firstChildByLocal(Element parent, String localName) {
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                if (removeNs(e.getTagName()).equals(localName)) return e;
            }
        }
        return null;
    }

    private static List<Element> childrenByLocal(Element parent, String localName) {
        List<Element> out = new ArrayList<>();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                if (removeNs(e.getTagName()).equals(localName)) out.add(e);
            }
        }
        return out;
    }
}