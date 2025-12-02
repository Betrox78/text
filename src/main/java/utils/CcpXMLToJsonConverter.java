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

import static service.commons.Constants.CCP_PREFIX;

public class CcpXMLToJsonConverter {
    public JsonObject convertXMLToJSON(String xmlBase64) throws Exception {
        byte[] decodedXML = Base64.getDecoder().decode(xmlBase64.getBytes(StandardCharsets.UTF_8));
        InputStream xmlStream = new ByteArrayInputStream(decodedXML);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlStream);

        JsonObject resultJson = new JsonObject();
        Element root = doc.getDocumentElement();
        processNode(root, resultJson);

        return resultJson;
    }

    private void processNode(Node node, JsonObject json) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String nodeName = removeNamespacePrefix(element.getTagName());

            switch (nodeName) {
                case "Conceptos":
                    json.put(nodeName, processArrayNode(element, "cfdi:Concepto"));
                    break;
                case "Ubicaciones":
                    json.put(nodeName, processArrayNode(element, CCP_PREFIX + ":Ubicacion"));
                    break;
                case "Mercancias":
                    JsonObject mercanciasJson = new JsonObject();
                    addAllAttributes(element, mercanciasJson);
                    mercanciasJson.put("Mercancia", processArrayNode(element, CCP_PREFIX + ":Mercancia"));
                    processChildNodes(element, mercanciasJson, "Autotransporte", CCP_PREFIX +":Autotransporte");
                    json.put(nodeName, mercanciasJson);
                    break;
                case "CfdiRelacionados":
                    processCfdiRelacionados(element, json);
                    break;
                default:
                    JsonObject childJson = new JsonObject();
                    processAttributesAndChildren(element, childJson);
                    json.put(nodeName, childJson);
                    break;
            }
        }
    }

    private String removeNamespacePrefix(String nodeName) {
        return nodeName.contains(":") ? nodeName.substring(nodeName.indexOf(':') + 1) : nodeName;
    }

    private void addAllAttributes(Element element, JsonObject json) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            json.put(removeNamespacePrefix(attr.getNodeName()), attr.getNodeValue());
        }
    }

    private JsonArray processArrayNode(Element parentElement, String childNodeName) {
        JsonArray jsonArray = new JsonArray();
        NodeList children = parentElement.getElementsByTagName(childNodeName);
        for (int i = 0; i < children.getLength(); i++) {
            Node childNode = children.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                JsonObject childJson = new JsonObject();
                processAttributesAndChildren((Element) childNode, childJson);
                jsonArray.add(childJson);
            }
        }
        return jsonArray;
    }

    private void processAttributesAndChildren(Element element, JsonObject childJson) {
        // Process attributes
        if (element.hasAttributes()) {
            for (int i = 0; i < element.getAttributes().getLength(); i++) {
                Node attr = element.getAttributes().item(i);
                childJson.put(attr.getNodeName(), attr.getNodeValue());
            }
        }

        // Process child nodes
        NodeList children = element.getChildNodes();
        Map<String, Integer> childNameCount = new HashMap<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node childNode = children.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String childNodeName = childNode.getNodeName();
                childNameCount.put(childNodeName, childNameCount.getOrDefault(childNodeName, 0) + 1);
            }
        }

        for (int i = 0; i < children.getLength(); i++) {
            Node childNode = children.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String childNodeName = childNode.getNodeName();
                if (childNameCount.get(childNodeName) > 1) {
                    JsonArray jsonArray = childJson.getJsonArray(childNodeName);
                    if (jsonArray == null) {
                        jsonArray = new JsonArray();
                        childJson.put(childNodeName, jsonArray);
                    }
                    JsonObject childJsonObject = new JsonObject();
                    processNode(childNode, childJsonObject);
                    jsonArray.add(childJsonObject);
                } else {
                    processNode(childNode, childJson);
                }
            }
        }
    }

    private void processChildNodes(Element parentElement, JsonObject json, String childJsonName, String childNodeName) {
        NodeList children = parentElement.getElementsByTagName(childNodeName);
        if (children.getLength() > 0) {
            Node childNode = children.item(0);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                JsonObject childJson = new JsonObject();
                processAttributesAndChildren((Element) childNode, childJson);
                processRemolques(childNode, childJson, "Remolques", CCP_PREFIX + ":Remolques");
                json.put(childJsonName, childJson);
            }
        }
    }

    private void processRemolques(Node parentElement, JsonObject json, String childJsonName, String childNodeName) {
        NodeList children = ((Element) parentElement).getElementsByTagName(childNodeName);
        if (children.getLength() > 0) {
            Node childNode = children.item(0);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                json.put(childJsonName, processArrayNode((Element) childNode, CCP_PREFIX + ":Remolque"));
            }
        }
    }

    private void processCfdiRelacionados(Element element, JsonObject json) {
        String tipoRelacion = element.getAttribute("TipoRelacion");
        JsonArray cfdiRelacionadosArray = processArrayNode(element, "cfdi:CfdiRelacionado");
        JsonObject cfdiRelacionadosJson = new JsonObject()
                .put("TipoRelacion", tipoRelacion)
                .put("CfdiRelacionado", cfdiRelacionadosArray);
        json.put("CfdiRelacionados", cfdiRelacionadosJson);
    }
}