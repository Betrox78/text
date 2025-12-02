package utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import static service.commons.Constants.PC_PREFIX;
public class PaymentCXMLToJsonConverter {
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
                case "Pago":
                    JsonObject pagoJson = new JsonObject();
                    processPago(element, pagoJson);
                    json.put(nodeName, pagoJson);
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

    private void processPago(Element pagoElement, JsonObject pagoJson) {
        addAllAttributes(pagoElement, pagoJson);
        NodeList children = pagoElement.getChildNodes();
        Map<String, Integer> childNameCount = new HashMap<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String childName = child.getNodeName();
                childNameCount.put(childName, childNameCount.getOrDefault(childName, 0) + 1);
            }
        }
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String fullChildName = child.getNodeName();
                String unprefixedName = removeNamespacePrefix(fullChildName);
                if (unprefixedName.equals("DoctoRelacionado")) {
                    JsonArray jsonArray = pagoJson.getJsonArray("DoctoRelacionados");
                    if (jsonArray == null) {
                        jsonArray = new JsonArray();
                        pagoJson.put("DoctoRelacionados", jsonArray);
                    }
                    JsonObject childJson = new JsonObject();
                    processNode(child, childJson);
                    jsonArray.add(childJson);
                } else {
                    processNode(child, pagoJson);
                }
            }
        }
    }
}
