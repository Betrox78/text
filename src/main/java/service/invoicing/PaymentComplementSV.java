package service.invoicing;

import database.commons.ErrorCodes;
import database.invoicing.InvoiceDBV;
import database.invoicing.PaymentComplementDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.json.XML;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.PaymentCXMLToJsonConverter;
import utils.UtilsMoney;

import static service.commons.Constants.*;
import static utils.UtilsResponse.*;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsValidation.isEmptyAndNotNull;

public class PaymentComplementSV extends ServiceVerticle {
    @Override
    protected String getDBAddress() {
        return PaymentComplementDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/payment_complement";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.POST, "/generatePC", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::generatePC);
        this.addHandler(HttpMethod.GET, "/getXML/:payment_complement_id", AuthMiddleware.getInstance(), this::getXML);
        this.addHandler(HttpMethod.GET, "/getPcByServiceCode/:service_code", AuthMiddleware.getInstance(), this::getPcByServiceCode);
        super.start(startFuture);
    }

    private void generatePC(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            body.put(CREATED_BY, context.<Integer>get(USER_ID));
            JsonObject paymentComplement = new JsonObject();
            JsonArray paymentIds = body.getJsonObject("complement").getJsonArray("payment_ids");
            body.getJsonObject("complement").remove("payment_ids");

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PaymentComplementDBV.ACTION_REGISTER_PC);
            vertx.eventBus().send(this.getDBAddress(), body, options, replyRegister -> {
                try {
                    if (replyRegister.succeeded()) {
                        Message<Object> result = replyRegister.result();
                        JsonObject registerPCBody = (JsonObject) result.body();

                        execService(context, PaymentComplementDBV.ACTION_HANDLE_STAMP, registerPCBody, cfdiResult -> {
                            String xml = XML.toString(cfdiResult.getString("factura_timbrada"));
                            paymentComplement.put("cfdiResult", cfdiResult);
                            paymentComplement.put("payment_complement_id", registerPCBody.getInteger("payment_complement_id"));
                            paymentComplement.put("xml", xml);
                            paymentComplement.put("uuid", cfdiResult.getString("uuid"));
                            paymentComplement.put("payment_id", body.getJsonObject("complement").getInteger("payment_id"));
                            paymentComplement.put("payment_ids", paymentIds);
                            paymentComplement.put("invoices", body.getJsonArray("details"));
                            paymentComplement.put("user_id", context.<Integer>get(USER_ID));

                            String error = cfdiResult.getString("error");
                            if(error != null) {
                                DeliveryOptions optionsDeleteInvoice = new DeliveryOptions().addHeader(ACTION, PaymentComplementDBV.ACTION_UPDATE_PC_WITH_ERROR);
                                vertx.eventBus().send(this.getDBAddress(), paymentComplement, optionsDeleteInvoice, repDeleteInvoice -> {
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
                                DeliveryOptions optionsUpdate = new DeliveryOptions().addHeader(ACTION, PaymentComplementDBV.ACTION_UPDATE_PC_SUCCESS);
                                vertx.eventBus().send(this.getDBAddress(), paymentComplement, optionsUpdate, replyUpdateInvoice -> {
                                    try {
                                        if (replyUpdateInvoice.succeeded()) {
                                            responseOk(context,
                                                    new JsonObject()
                                                            .put("Response", xml)
                                                            .put("payment_complement_id", registerPCBody.getInteger("payment_complement_id"))
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
                responseError(context, "Ocurrió un error inesperado", reply.cause().getMessage());
            }
        });
    }

    private void getXML(RoutingContext context) {
        Integer pmId = Integer.valueOf(context.request().getParam("payment_complement_id"));

        try {
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PaymentComplementDBV.ACTION_GET_PCI_XML);
            vertx.eventBus().send(this.getDBAddress(), new JsonObject().put("payment_complement_id", pmId), options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }

                    JsonObject resultData = new JsonObject();
                    resultData.put("payment_complement_cfdi", reply.result().body());
                    try {
                        JsonObject resultPC = resultData.getJsonArray("payment_complement_cfdi").getJsonObject(0);
                        PaymentCXMLToJsonConverter converter = new PaymentCXMLToJsonConverter();
                        JsonObject xmlObj = converter.convertXMLToJSON(resultPC.getString("xml"));
                        String totalLetra = UtilsMoney.numberToLetter(xmlObj.getJsonObject("Comprobante").getString("Total"));
                        xmlObj.getJsonObject("Comprobante").put("totalLetra", totalLetra);
                        xmlObj.put("xml", resultPC.getString("xml"));
                        responseOk(context, new JsonObject().put("payment_complement", xmlObj));
                    } catch (Throwable t){
                        t.printStackTrace();
                        responseError(context, "error al leer XML complemento carta porte", t);
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

    private void getPcByServiceCode(RoutingContext context) {
        try {
            String serviceCode = context.request().getParam("service_code");
            JsonObject body = new JsonObject().put("service_code", serviceCode);
            isEmptyAndNotNull(body, "service_code");

            InvoiceDBV.ServiceTypes serviceType = InvoiceDBV.ServiceTypes.getTypeByReservationCode(serviceCode);
            body.put("tabla_name", serviceType.getTable());
            body.put("service_type", serviceType.getType());

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, PaymentComplementDBV.ACTION_GET_PC_BY_SERVICE_CODE);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.failed()) {
                        throw new Exception(reply.cause());
                    }
                    responseOk(context, reply.result().body());
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