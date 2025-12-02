/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.money;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ulises
 */
public class ExchangeRateService {

    private static String exchangeRateService() throws MalformedURLException, IOException {
        //Code to make a webservice HTTP request
        String responseString = "";
        String outputString = "";
        String wsURL = "http://www.banxico.org.mx/DgieWSWeb/DgieWS";
        URL url = new URL(wsURL);
        URLConnection connection = url.openConnection();
        HttpURLConnection httpConn = (HttpURLConnection) connection;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        String xmlInput = "<soapenv:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ws=\"http://ws.dgie.banxico.org.mx\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <ws:tiposDeCambioBanxico soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"/>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        byte[] buffer = new byte[xmlInput.length()];
        buffer = xmlInput.getBytes();
        bout.write(buffer);
        byte[] b = bout.toByteArray();

        // Set the appropriate HTTP parameters.
        httpConn.setRequestProperty("Content-Length",
                String.valueOf(b.length));
        httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        httpConn.setRequestProperty("SOAPAction", "");
        httpConn.setRequestMethod("POST");
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);

        OutputStream out = httpConn.getOutputStream();
        //Write the content of the request to the outputstream of the HTTP Connection.
        out.write(b);
        out.close();
        //Ready with sending the request.

        //Read the response.
        InputStreamReader isr = null;
        if (httpConn.getResponseCode() == 200) {
            isr = new InputStreamReader(httpConn.getInputStream());
        } else {
            isr = new InputStreamReader(httpConn.getErrorStream());
        }
        BufferedReader in = new BufferedReader(isr);

        //Write the SOAP message response to a String.
        while ((responseString = in.readLine()) != null) {
            outputString = outputString + responseString;
        }
        return outputString;
    }

    public static JsonArray getResult() throws IOException {
        String res = exchangeRateService();
        return processRequest(res);
    }

    private static Double getSerieValue(String sery) {
        String[] valueLine = sery.split("OBS_VALUE=&quot;");
        String[] numberPart = valueLine[1].split("&");
        return Double.parseDouble(numberPart[0]);
    }

    private static JsonArray processRequest(String res) {
        JsonArray result = new JsonArray();
        List<String> series = new ArrayList<>();
        Pattern p = Pattern.compile("bm:Series(.+?)/bm:Series");
        Matcher m = p.matcher(res);
        m.find();
        series.add(m.group(1));
        m.find();
        series.add(m.group(1));
        m.find();
        series.add(m.group(1));
        m.find();
        series.add(m.group(1));
        m.find();
        series.add(m.group(1));
        m.find();
        series.add(m.group(1));
        for (String sery : series) {
            for (SERIE_A_PESO_MEXICANO serie : SERIE_A_PESO_MEXICANO.values()) {
                if (sery.contains(serie.getId())) {
                    Double changeAmount = getSerieValue(sery);
                    result.add(new JsonObject()
                            .put("serie_id", serie.getId())
                            .put("serie_name", serie.getName())
                            .put("serie_iso", serie.getIso())
                            .put("change_amount", changeAmount));
                }
            }
        }
        return result;
    }

    public static enum SERIE_A_PESO_MEXICANO {
        DOLAR_CANADIENSE("SF60632", "Dolar canadiense", "CAD", "C$"),
        DOLAR_EUA_FD("SF43718", "Dólar EUA Fecha Det.", "USD", "$"),
        DOLAR_EUA_FL("SF60653", "Dólar EUA Fecha Liq.", "USD", "$"),
        LIBRA_EXTERLINA("SF46407", "Libra Exterlina", "GBP", "£"),
        YEN_JAP("SF46406", "Yen Japones", "JPY", "¥"),
        EURO("SF46410", "Euro", "EUR", "€");

        private final String id;
        private final String name;
        private final String iso;
        private final String symbol;

        SERIE_A_PESO_MEXICANO(String id, String name, String iso, String symbol) {
            this.id = id;
            this.name = name;
            this.iso = iso;
            this.symbol = symbol;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getIso() {
            return iso;
        }

        public String getSymbol() {
            return symbol;
        }

    }

    public static void printSeriesValues() {
        SERIE_A_PESO_MEXICANO[] series = SERIE_A_PESO_MEXICANO.values();
        List<JsonObject> obs = new ArrayList<>();
        for (SERIE_A_PESO_MEXICANO sery : series) {
            obs.add(new JsonObject()
                    .put("iso", sery.getIso())
                    .put("symbol", sery.getSymbol())
                    .put("serie_id", sery.getId())
                    .put("rate", sery.name()));
        }
        System.out.println(Arrays.toString(obs.toArray()));
    }
}
