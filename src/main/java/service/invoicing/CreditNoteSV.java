package service.invoicing;

import database.commons.ErrorCodes;
import database.invoicing.CreditNoteDBV;
import database.invoicing.PaymentComplementDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.json.XML;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import utils.GenericCFDIToJsonConverter;
import utils.PaymentCXMLToJsonConverter;
import utils.UtilsMoney;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsResponse.responseOk;

public class CreditNoteSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return CreditNoteDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/credit_note";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/generateCN", AuthMiddleware.getInstance(), this::generateCN);
        this.addHandler(HttpMethod.GET, "/getXML/:credit_note_id", AuthMiddleware.getInstance(), this::getXML);
        super.start(startFuture);
    }

    private void generateCN(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            JsonObject creditNote = new JsonObject();

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CreditNoteDBV.ACTION_REGISTER_CN);
            vertx.eventBus().send(this.getDBAddress(), body, options, replyRegister -> {
                try {
                    if (replyRegister.succeeded()) {
                        Message<Object> result = replyRegister.result();
                        JsonObject registerCNBody = (JsonObject) result.body();

                        execService(context, CreditNoteDBV.ACTION_HANDLE_STAMP, registerCNBody, cfdiResult -> {
                            String xml = XML.toString(cfdiResult.getString("factura_timbrada"));
                            creditNote.put("cfdiResult", cfdiResult);
                            creditNote.put("credit_note_id", registerCNBody.getInteger("credit_note_id"));
                            creditNote.put("xml", xml);
                            creditNote.put("uuid", cfdiResult.getString("uuid"));
                            creditNote.put("invoices", body.getJsonArray("invoices"));
                            creditNote.put("services", body.getJsonArray("services"));
                            creditNote.put("user_id", context.<Integer>get(USER_ID));

                            String error = cfdiResult.getString("error");
                            if(error != null) {
                                DeliveryOptions optionsDeleteInvoice = new DeliveryOptions().addHeader(ACTION, CreditNoteDBV.ACTION_UPDATE_CN_WITH_ERROR);
                                vertx.eventBus().send(this.getDBAddress(), creditNote, optionsDeleteInvoice, repDeleteInvoice -> {
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
                                DeliveryOptions optionsUpdate = new DeliveryOptions().addHeader(ACTION, CreditNoteDBV.ACTION_UPDATE_CN_SUCCESS);
                                vertx.eventBus().send(this.getDBAddress(), creditNote, optionsUpdate, replyUpdateInvoice -> {
                                    try {
                                        if (replyUpdateInvoice.succeeded()) {
                                            responseOk(context,
                                                    new JsonObject()
                                                            .put("Response", xml)
                                                            .put("credit_note_id", registerCNBody.getInteger("credit_note_id"))
                                                            .put("cadena_original", cfdiResult.getString("cadena_original"))
                                                            .put("error", cfdiResult.getString("error")));
                                        } else {
                                            responseError(context, UNEXPECTED_ERROR, replyUpdateInvoice.cause().getMessage());
                                        }
                                    } catch (Exception e) {
                                        responseError(context, UNEXPECTED_ERROR, e.getCause());
                                    }
                                });
                            }
                        });
                    } else {
                        responseError(context, UNEXPECTED_ERROR, replyRegister.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getCause());
                }
            });
        } catch (Exception e) {
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void execService(RoutingContext context, String action, JsonObject body, Handler<JsonObject> handler) {
        vertx.eventBus().send(this.getDBAddress(), body, options(action), (AsyncResult<Message<JsonObject>> reply) -> {
            if (reply.succeeded()) {
                if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                    responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                } else {
                    handler.handle(reply.result().body());
                }
            } else {
                System.out.println(reply.cause().getMessage());
                responseError(context, "Ocurrió un error inesperado", reply.cause().getMessage());
            }
        });
    }

    private void getXML(RoutingContext context) {
        Integer cnId = Integer.valueOf(context.request().getParam("credit_note_id"));

        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, CreditNoteDBV.ACTION_GET_CNI_XML);
            vertx.eventBus().send(this.getDBAddress(), new JsonObject().put("credit_note_id", cnId), options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    JsonObject resultData = new JsonObject();
                    resultData.put("credit_note_invoice", reply.result().body());
                    try {
                        JsonObject resultPC = resultData.getJsonArray("credit_note_invoice").getJsonObject(0);
                        GenericCFDIToJsonConverter converter = new GenericCFDIToJsonConverter();
                        String xmlBase64 = resultPC.getString("xml");
                        JsonObject cfdiJson = converter.convertBase64ToJSON(xmlBase64, false);
                        cfdiJson.put("xml", xmlBase64);
                        responseOk(context, new JsonObject().put("credit_note", cfdiJson));
                    } catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, "error al leer XML de nota de credito", t);
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
}