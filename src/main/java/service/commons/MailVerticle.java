/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.commons;

import database.commons.ErrorCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


import static service.commons.Constants.ACTION;
import static service.commons.Constants.CONFIG_HTTP_SERVER_PORT;

import utils.UtilsResponse;
import utils.UtilsRouter;
import static utils.UtilsValidation.*;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class MailVerticle extends AbstractVerticle {

    public static final String ACTION_SEND_HTML_TEMPLATE_MAIL = "MailVerticle.actionSendHTMLMailTemplate";
    public static final String ACTION_SEND_HTML_TEMPLATE_MAIL_MONGODB = "MailVerticle.actionSendHTMLMailTemplateMongoDB";
    public static final String ACTION_SEND_RECOVER_PASS = "MailVerticle.sendRecoverPass";

    private MailClient mailClient;

    private MongoClient mongoClient;

    @Override
    public void start() throws Exception {
        super.start();
        MailConfig config = new MailConfig();        
        config.setHostname(config().getString("hostName"));
        config.setPort(config().getInteger("port"));
        config.setUsername(config().getString("userName"));
        config.setPassword(config().getString("password"));
        config.setSsl(config().getBoolean("ssl"));
        config.setTrustAll(true);
        String tls = config().getString("tls");
        if (tls != null) {
            config.setStarttls(StartTLSOptions.valueOf(tls));
        }
        
        mailClient = MailClient.createShared(vertx, config);

        // MongoDB client
        mongoClient = MongoClient.createNonShared(vertx, config()
                .getJsonObject("mongodb")
                .put("useObjectId", true));

        Router router = Router.router(vertx);
        router.post("/sendOne").handler(BodyHandler.create());
        router.post("/sendOne").handler(this::sendHTMLMail);
        router.post("/sendToContact").handler(BodyHandler.create());
        router.post("/sendToContact").handler(this::sendToContact);
        router.post("/sendMany").handler(BodyHandler.create());
        router.post("/sendMany").handler(this::sendManyHTMLMail);
        router.post("/sendTemplate").handler(BodyHandler.create());
        router.post("/sendTemplate").handler(this::sendHTMLMailTemplate);

        UtilsRouter.getInstance(vertx).mountSubRouter("/mail", router);
        Integer portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT);
        if (portNumber == null) {
            System.out.println("Could not start a HTTP server"
                    + this.getClass().getSimpleName()
                    + ", no port speficied in configuration");
        } else {
            HttpServer server = vertx.createHttpServer();
            server.requestHandler(UtilsRouter.getInstance(vertx)::accept)
                    .listen(portNumber, ar -> {
                        if (ar.succeeded()) {
                            System.out.println(this.getClass().getSimpleName()
                                    + " running");
                        } else {
                            System.out.println("Could not start a HTTP server "
                                    + this.getClass().getSimpleName()
                                    + ", "
                                    + ar.cause());
                        }
                    });
        }
        vertx.eventBus().consumer(this.getClass().getSimpleName(), this::onMessage);
    }

    /**
     * Validates if the action in the headers is valid
     *
     * @param message the message from the event bus
     * @return true if containg an action, false otherwise
     */
    protected boolean isValidAction(Message<JsonObject> message) {
        if (!message.headers().contains("action")) {
            message.fail(ErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No action header specified");
            return false;
        }
        return true;
    }

    private void onMessage(Message<JsonObject> message) {
        if (isValidAction(message)) {
            String action = message.headers().get(ACTION);
            switch (action) {
                case ACTION_SEND_HTML_TEMPLATE_MAIL:
                    this.actionSendHTMLMailTemplate(message);
                    break;
                case ACTION_SEND_RECOVER_PASS:
                    this.sendRecoverPass(message);
                    break;
                case ACTION_SEND_HTML_TEMPLATE_MAIL_MONGODB:
                    this.actionSendHTMLMailTemplateToMongoDB(message);
                    break;
            }
        }
    }

    private void sendManyHTMLMail(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isEmptyAndNotNull(body.getJsonArray("send_to"), "send_to");
            isEmptyAndNotNull(body, "subject");
            isEmptyAndNotNull(body, "html_content");
        } catch (PropertyValueException e) {
            UtilsResponse.responsePropertyValue(context, e);
            return;
        }

        MailMessage mail = new MailMessage();
        mail.setFrom(config().getString("userName"));

        JsonArray mails = body.getJsonArray("send_to");
        List<String> mailsTo = new ArrayList<>();
        for (int i = 0; i < mails.size(); i++) {
            mailsTo.add(mails.getString(i));
        }

        mail.setTo(mailsTo);
        mail.setSubject(body.getString("subject"));
        mail.setHtml(body.getString("html_content"));

        mailClient.sendMail(mail, reply -> {
            if (reply.succeeded()) {
                UtilsResponse.responseOk(context, "Mails sended");
            } else {
                UtilsResponse.responseError(context, reply.cause().getMessage());
            }
        });
    }

    private void sendToContact(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        body.put("send_to", "contacto@ptxpaqueteria.com");
        context.setBody(body.toBuffer());
        sendHTMLMail(context);
    }

    private void sendHTMLMail(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isEmptyAndNotNull(body, "send_to");
            isEmptyAndNotNull(body, "subject");
            isEmptyAndNotNull(body, "html_content");
        } catch (PropertyValueException e) {
            UtilsResponse.responsePropertyValue(context, e);
            return;
        }

        String sendTo = body.getString("send_to");
        List<String> recipients = Arrays.stream(sendTo.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        for (String email : recipients) {
            if (!isValidEmail(email)) {
                UtilsResponse.responseError(context, "Invalid email format: " + email);
                return;
            }
        }

        MailMessage mail = new MailMessage();
        mail.setFrom(config().getString("userName"));
        mail.setTo(recipients);
        mail.setSubject(body.getString("subject"));
        mail.setHtml(body.getString("html_content"));

        mailClient.sendMail(mail, reply -> {
            if (reply.succeeded()) {
                UtilsResponse.responseOk(context, "Mail sended");
            } else {
                UtilsResponse.responseError(context, reply.cause().getMessage());
            }
        });
    }

    private void actionSendHTMLMailTemplateToMongoDB(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            isMultipleMailAndNotNull(body, "to");
            isEmptyAndNotNull(body, "subject");
            isEmptyAndNotNull(body, "template");
            if (body.containsKey("bcc")) {
                isMultipleMailAndNotNull(body, "bcc");
            }
            body.put("createdAt", new JsonObject().put("$date", new Date().toInstant()));
            body.put("sendAt", new JsonObject().put("$date", new Date(0).toInstant()));
            body.put("sentAt", new JsonObject().put("$date", new Date(0).toInstant()));

            this.mongoClient.save("mailings", body, reply -> {
                try {
                    if (reply.failed()) {
                        message.fail(0, reply.cause().getMessage());
                    } else {
                        message.reply(reply.result());
                    }
                } catch (Exception e) {
                    message.fail(0, e.getMessage());
                }
            });
        } catch (PropertyValueException e) {
            message.fail(0, e.getName() + " " + e.getError());
        } catch (Exception e) {
            message.fail(0, e.getMessage());
        }
    }

    private void sendHTMLMailTemplate(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        try {
            isEmptyAndNotNull(body, "send_to");
            isEmptyAndNotNull(body, "subject");
            isEmptyAndNotNull(body, "html_template");
        } catch (PropertyValueException e) {
            UtilsResponse.responsePropertyValue(context, e);
            return;
        }
        this.actionSendHTMLMailTemplate(
                body.getString("html_template"),
                body.getString("send_to"),
                body.getString("subject"),
                body.getJsonObject("data"))
        .whenComplete((s, t) -> {
            try{
                if (t != null) {
                    UtilsResponse.responseError(context, t.getMessage());
                } else {
                    UtilsResponse.responseOk(context, s);
                }
            } catch (Exception e){
                UtilsResponse.responseError(context, e);
            }
        });;
    }

    private void actionSendHTMLMailTemplate(Message<JsonObject> message) {
        JsonObject body = message.body();
        try {
            isEmptyAndNotNull(body, "send_to");
            isEmptyAndNotNull(body, "subject");
            isEmptyAndNotNull(body, "html_template");
            this.actionSendHTMLMailTemplate(
                    body.getString("html_template"),
                    body.getString("send_to"),
                    body.getString("subject"),
                    body.getJsonObject("data"))
                    .whenComplete((s, t) -> {
                        try{
                            if (t != null) {
                                String error = t.getMessage() == null ? t.toString() : t.getMessage();
                                message.fail(0, error);
                            } else {
                                message.reply(s);
                            }
                        } catch (Exception e){
                            message.fail(0, e.getMessage());
                        }
                    });
        } catch (PropertyValueException e) {
            message.fail(0, e.getName() + " " + e.getError());
        }
    }


    private CompletableFuture<Void> actionSendHTMLMailTemplate(String htmlTemplateName, String sendTo, String subject, JsonObject replaceParams) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        this.vertx.fileSystem().readFile("./files/mail/" + htmlTemplateName, r -> {
            try{
                if (r.succeeded()) {
                    String htmlMailTemplate = r.result().toString();

                    for (String fieldName : replaceParams.fieldNames()) {
                        htmlMailTemplate = htmlMailTemplate.replace(
                                "%%" + fieldName + "%%",
                                replaceParams.getString(fieldName));
                    }

                    MailMessage mail = new MailMessage();
                    mail.setFrom(config().getString("userName"));
                    mail.setTo(sendTo);
                    mail.setSubject(subject);
                    mail.setHtml(htmlMailTemplate);

                    mailClient.sendMail(mail, reply -> {
                        try{
                            if (reply.failed()) {
                                System.out.println("Mail cant be sent, cause: "
                                        + reply.cause().getMessage());
                                future.completeExceptionally(reply.cause());
                            } else {
                                future.complete(null);
                            }
                        } catch (Exception e){
                            e.printStackTrace();
                            future.completeExceptionally(e);
                        }
                    });
                } else {
                    System.out.println("someting goes wrong with template: "
                            + htmlTemplateName + " cause: "
                            + r.cause().getMessage());
                    future.completeExceptionally(r.cause());
                }
            } catch (Exception e){
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private void sendRecoverPass(Message<JsonObject> message) {
        String employeeName = message.body().getString("employee_name");
        String employeeMail = message.body().getString("employee_email");
        String recoverCode = message.body().getString("recover_code");

        MailMessage mail = new MailMessage();
        mail.setFrom(config().getString("userName"));
        mail.setTo(employeeMail);
        mail.setSubject("Recover password");
        mail.setHtml("<html>\n"
                + "    <head>\n"
                + "        <title>Servicio de recuperación de contraseña</title>\n"
                + "    </head>\n"
                + "    <body style=\"color: rgba(0,0,0, 0.8); font-family: verdana; font-size: 14px;\">\n"
                + "        <div style=\"width: 100%; display: flex; justfy-content: center;\">\n"
                + "            <div style=\"width: 70%;\">\n"
                + "                <div style=\"border: 1px solid rgba(128, 128, 128, 0.31); width: 100%; padding: 20px;\">\n"
                + "                    <h3 style=\"margin-top: 0;\">"
                + "HOLA " + employeeName
                + "                    </h3>\n"
                + "                    <p>Utilize el siguiente código para continuar con el proceso de recuperación de su contraseña</p>\n"
                + "\n"
                + "                    <div style=\"background: #009BD2; color: white; padding: 10px; width: 100px; text-align: center;\">\n"
                + recoverCode
                + "                    </div>\n"
                + "                </div>\n"
                + "\n"
                + "            </div>\n"
                + "        </div>\n"
                + "    </body>\n"
                + "</html>");
        mailClient.sendMail(mail, reply -> {
            if (reply.succeeded()) {
                message.reply(new JsonObject());
            } else {
                message.fail(0, reply.cause().getMessage());
            }
        });
    }
}
