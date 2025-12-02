package database.invoicing.handlers.parcelInvoiceDBV;
import io.vertx.core.json.JsonObject;

import javax.xml.soap.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class Timbrado {

    // URL del servicio
    static String URLServer = "https://staging.ws.timbox.com.mx/timbrado_cfdi40/wsdl";
    // Accion para el timbrado
    final static String ACCION = "timbrar_cfdi";
    // Propiedades
    private String usuario = "", contrasena = "", sxml = "";
    private JsonObject config = new JsonObject();

    public Timbrado(String usuarioValue, String contrasenaValue, String documentoValue) {
        usuario = usuarioValue;
        contrasena = contrasenaValue;
        sxml = documentoValue;
    }

    public Timbrado(String usuarioValue, String contrasenaValue, String documentoValue, JsonObject config) {
        usuario = usuarioValue;
        contrasena = contrasenaValue;
        sxml = documentoValue;
        this.config = config;
    }

    public String Timbrar() throws Exception {
        // Conexion SOAP
        String response = "";
        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection soapConnection = soapConnectionFactory.createConnection();
        MessageFactory messageFactory = MessageFactory.newInstance();
        messageFactory.createMessage();
        SOAPMessage soapResponse;
        URL endpoint =
                 new URL(new URL(config.getString("timbox_url")),
                        "/timbrado_cfdi40/action",
                        new URLStreamHandler() {
                            @Override
                            protected URLConnection openConnection(URL url) throws IOException {
                                URL target = new URL(url.toString());
                                URLConnection connection = target.openConnection();
                                // Connection settings
                                connection.setConnectTimeout(60000); // 1 min
                                connection.setReadTimeout(120000); // 2 min
                                return(connection);
                            }
                        });
        try {
            // Ejecucion del metodo para timbrar_cfdi
            soapResponse = soapConnection.call(peticionTimbrado(), endpoint);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            soapResponse.writeTo(outputStream);
            response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            soapConnection.close();
        }

        return response;
    }
    public CompletableFuture<String> timbrarFactura() {
        CompletableFuture<String> future = new CompletableFuture<>();
        try{
            String response = "";
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();
            MessageFactory messageFactory = MessageFactory.newInstance();
            messageFactory.createMessage();
            SOAPMessage soapResponse;
            URL endpoint =
                    new URL(new URL(config.getString("timbox_url")),
                            "/timbrado_cfdi40/action",
                            new URLStreamHandler() {
                                @Override
                                protected URLConnection openConnection(URL url) throws IOException {
                                    URL target = new URL(url.toString());
                                    URLConnection connection = target.openConnection();
                                    // Connection settings
                                    connection.setConnectTimeout(10000); // 10 sec
                                    connection.setReadTimeout(60000); // 1 min
                                    return(connection);
                                }
                            });
            try {
                // Ejecucion del metodo para timbrar_cfdi
                soapResponse = soapConnection.call(peticionTimbrado(), endpoint);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                soapResponse.writeTo(outputStream);
                future.complete(outputStream.toString());
            } finally {
                soapConnection.close();
            }
        } catch (Throwable t){
            t.printStackTrace();
            future.completeExceptionally(t);
        }
        return future;
    }

    private SOAPMessage peticionTimbrado() throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();

        // Creacion de envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("urn", "urn:WashOut");
        envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
        // Cuerpo del envelope
        SOAPBody soapBody = envelope.getBody();
        SOAPElement urn = soapBody.addChildElement(ACCION, "urn");
        // Parametros para el usuario
        SOAPElement usernameElement = urn.addChildElement("username");
        usernameElement.addTextNode(usuario);
        usernameElement.setAttribute("xsi:type", "xsd:string");
        // Parametros para la contrasena
        SOAPElement passwordElement = urn.addChildElement("password");
        passwordElement.addTextNode(contrasena);
        passwordElement.setAttribute("xsi:type", "xsd:string");
        // Parametros para el xml
        SOAPElement sxmlElement = urn.addChildElement("sxml");
        sxmlElement.addTextNode(sxml);
        sxmlElement.setAttribute("xsi:type", "xsd:string");

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", ACCION);

        soapMessage.saveChanges();

        return soapMessage;
    }
}