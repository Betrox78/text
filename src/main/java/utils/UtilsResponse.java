/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import models.ModelReponse;
import static models.ModelReponse.Status.*;
import static service.commons.Constants.*;

import models.PropertyError;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utils class for redundant presentation of the responses in http requests, use this to encapsulate data and messages into a generic model
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class UtilsResponse {

    private static final Pattern timeoutPattern = Pattern.compile("^\\(TIMEOUT,.*\\).*$");

    public static void responseOk(RoutingContext context, String message, String devMessage, Object data) {
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        if (data == null) {
            response.end(Json.encode(new ModelReponse(WARNING, "Element not found")));
        } else {
            response.end(Json.encode(new ModelReponse(OK, message, devMessage, data)));
        }
    }

    public static void responseOk(RoutingContext context, String message, Object data) {
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        if (data == null) {
            response.end(Json.encode(new ModelReponse(WARNING, "Element not found")));
        } else {
            response.end(Json.encode(new ModelReponse(OK, message, data)));
        }
    }

    public static void responseOk(RoutingContext context, Object data) {
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        if (data == null) {
            response.end(Json.encode(new ModelReponse(WARNING, "Element not found")));
        } else {
            response.end(Json.encode(new ModelReponse(OK, data)));
        }
    }
    public static void sendPDFFile(byte[] fileBytes, RoutingContext context, String message) {
        Buffer buffer = Buffer.buffer(fileBytes);
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/pdf");
        if (buffer == null) {
            response.end(Json.encode(new ModelReponse(WARNING, "Error generate PDF")));
        } else {
            response.end(buffer);
        }
    }

    public static void responseOk(RoutingContext context, Object data, String devMessage) {
        ModelReponse res = new ModelReponse(OK);
        res.setData(data);
        res.setDevMessage(devMessage);
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        if (data == null) {
            response.end(Json.encode(new ModelReponse(WARNING, "Element not found")));
        } else {
            response.end(Json.encode(res));
        }
    }

    public static void responseOk(RoutingContext context, String message) {
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        response.end(Json.encode(new ModelReponse(OK, message)));
    }

    public static void responseWarning(RoutingContext context, String message) {
        HttpServerResponse response = context.response();
        verifyTimeout(message, response);
        response.putHeader("Content-Type", "application/json");
        response.end(Json.encode(new ModelReponse(WARNING, message)));
        printRequestLog(context, message, null, null);
    }

    public static void responseWarning(RoutingContext context, String message, String devMessage) {
        HttpServerResponse response = context.response();
        verifyTimeout(message, response);
        verifyTimeout(devMessage, response);
        response.putHeader("Content-Type", "application/json");
        response.end(Json.encode(new ModelReponse(WARNING, message, devMessage)));
        printRequestLog(context, message, devMessage, null);
    }

    public static void responseWarning(RoutingContext context, String message, String devMessage, Object data) {
        HttpServerResponse response = context.response();
        verifyTimeout(message, response);
        verifyTimeout(devMessage, response);
        response.putHeader("Content-Type", "application/json");
        response.end(Json.encode(new ModelReponse(WARNING, message, devMessage, data)));
        printRequestLog(context, message, null, data);
    }

    public static void responseWarning(RoutingContext context, String message, Object data) {
        HttpServerResponse response = context.response();
        verifyTimeout(message, response);
        response.putHeader("Content-Type", "application/json");
        response.end(Json.encode(new ModelReponse(WARNING, message, data)));
        printRequestLog(context, message, null, data);
    }

    public static void responseWarning(RoutingContext context, Object data) {
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        response.end(Json.encode(new ModelReponse(WARNING, data)));
        printRequestLog(context, null, null, data);
    }

    public static void responseWarning(RoutingContext context, Object data, String devMessage) {
        ModelReponse res = new ModelReponse(WARNING);
        res.setData(data);
        res.setDevMessage(devMessage);
        HttpServerResponse response = context.response();
        verifyTimeout(devMessage, response);
        response.putHeader("Content-Type", "application/json");
        response.end(Json.encode(res));
        printRequestLog(context, null, devMessage, data);
    }

    public static boolean responsePropertyValue(RoutingContext context, UtilsValidation.PropertyValueException ex) {
        responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, new PropertyError(ex.getName(), ex.getError()));
        return false;
    }

    public static void responseInvalidToken(RoutingContext context) {
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        response.end(Json.encode(new ModelReponse(INVALID_TOKEN, "Out of session", "the json web token in authorization header is invalid")));
    }

    public static void responseError(RoutingContext context, String message, String devMessage, Object data) {
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        verifyTimeout(devMessage, response);
        verifyTimeout(message, response);
        response.end(Json.encode(new ModelReponse(ERROR, message, devMessage, data)));
        printRequestLog(context, message, devMessage, data);
    }

    private static void verifyTimeout(String message, HttpServerResponse response) {
        try {
            Matcher matcher = timeoutPattern.matcher(message);
            if (matcher.matches()) {
                response.setStatusCode(500);
            }
        } catch (Exception ex) {
            System.err.println("Error to verify timeout: ".concat(ex.getMessage()));
        }
    }

    public static void responseError(RoutingContext context, String message, Object data) {
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        verifyTimeout(message, response);
        response.end(Json.encode(new ModelReponse(ERROR, message, data)));
        printRequestLog(context, message, null, data);
    }

    public static void responseError(RoutingContext context, Object data) {
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        response.end(Json.encode(new ModelReponse(ERROR, data)));
        printRequestLog(context, null, null, data);
    }

    public static void responseError(RoutingContext context, Throwable t) {
        t.printStackTrace();
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        verifyTimeout(t.getMessage(), response);
        response.end(Json.encode(new ModelReponse(ERROR, t.getMessage())));
        printRequestLog(context, null, null, t.getMessage());
    }

    public static void responseError(RoutingContext context, Object data, String devMessage) {
        ModelReponse res = new ModelReponse(ERROR);
        res.setData(data);
        res.setDevMessage(devMessage);
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        verifyTimeout(devMessage, response);
        response.end(Json.encode(res));
        printRequestLog(context, null, devMessage, data);
    }

    public static void responseError(RoutingContext context, String message) {
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        verifyTimeout(message, response);
        response.end(Json.encode(new ModelReponse(ERROR, "Ocurrió un error inesperado, consulte con el proveedor de sistemas", message)));
        printRequestLog(context, message, null, null);
    }

    public static void responseError(RoutingContext context, String message, String devMessage) {
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        verifyTimeout(devMessage, response);
        verifyTimeout(message, response);
        response.end(Json.encode(new ModelReponse(ERROR, message, devMessage)));
        printRequestLog(context, message, devMessage, null);
    }

    public static void responseDatatable(RoutingContext context, Object data) {
        HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json");
        response.end(Json.encode(data));
    }

    private static void printRequestLog(RoutingContext context, String message, String devMessage, Object data) {
        try {
            StringBuilder log = new StringBuilder();
            HttpServerRequest request = context.request();
            log.append("== [START] Request data ==");
            log.append("\nMethod: ").append(request.method());
            log.append("\nPath: ").append(request.path());
            log.append("\nParams: \n").append(request.params());

            JsonObject bodyRequest = context.getBodyAsJson();
            if (bodyRequest != null) {
                if (bodyRequest.containsKey("pass")) {
                    bodyRequest.put("pass", "*********");
                }
                log.append("\nBody: \n").append(bodyRequest.encodePrettily());
            }

            Integer userId = context.get(USER_ID);
            if (userId != null) {
                log.append("\nUser ID: ").append(userId);
            }
            log.append("\n== [END] Request data ==");

            log.append("\n== [START] Response data ==");
            if (message != null) {
                log.append("\nMessage: ").append(message);
            }
            if (devMessage != null) {
                log.append("\ndevMessage: ").append(devMessage);
            }
            if (data != null) {
                log.append("\nData: ").append(data);
            }
            log.append("\n== [END] Response data ==");

            System.out.println(log.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Error to print log: ".concat(ex.getMessage()));
        }
    }

}
