package service.invoicing;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.ConfirmSubscriptionRequest;
import com.amazonaws.services.sns.model.SubscribeRequest;
import database.commons.ErrorCodes;
import database.invoicing.InvoiceDBV;
import database.invoicing.handlers.parcelInvoiceDBV.ParcelInvoice;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.json.XML;
import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.UtilsResponse;

import static database.customers.CustomersBillingInfoDBV.ASSIGN_TO_CUSTOMER;
import static utils.UtilsValidation.isValidEmail;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static database.invoicing.InvoiceDBV.GET_UNBILLED;
import static service.commons.Constants.*;
import static service.commons.Constants.INVALID_DATA_MESSAGE;
import static utils.UtilsResponse.*;
import static utils.UtilsValidation.*;
import static utils.UtilsValidation.isEmptyAndNotNull;

public class InvoiceSV extends ServiceVerticle {

    private AmazonSNS amazonSNSClient;

    @Override
    protected String getDBAddress() {
        return InvoiceDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/invoices";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        this.addHandler(HttpMethod.GET, "/serviceDetail/:type/:code", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::serviceDetail);
        this.addHandler(HttpMethod.POST, "/", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::register);
        this.addHandler(HttpMethod.POST, "/register/parcel", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::registerParcel);
        this.addHandler(HttpMethod.POST, "/global", AuthMiddleware.getInstance(), this::registerGlobal);
        this.addHandler(HttpMethod.POST, "/snsNotification", this::receiveSNSInvoice);
        this.addHandler(HttpMethod.GET, "/report/unbilled", AuthMiddleware.getInstance(), this::getUnbilled);
        this.addHandler(HttpMethod.POST, "/associate", AuthMiddleware.getInstance(), this::associate);
        this.addHandler(HttpMethod.POST, "/disassociate", AuthMiddleware.getInstance(), this::disassociate);
        this.addHandler(HttpMethod.POST, "/disassociateGlobalBilling", AuthMiddleware.getInstance(), this::disassociateGlobal);
        this.addHandler(HttpMethod.POST, "/exportGlobal", AuthMiddleware.getInstance(), this::exportGlobal);
        this.addHandler(HttpMethod.GET, "/globalInvoice/:fileName",AuthMiddleware.getInstance(),this::getGlobalInvoiceCsv);
        this.addHandler(HttpMethod.GET, "/globalInvoice/getXML/:type/:id",AuthMiddleware.getInstance(),this::getGlobalInvoiceXML);
        this.addHandler(HttpMethod.GET, "/getXML/:type/:id",AuthMiddleware.getInstance(),this::getXML);
        this.addHandler(HttpMethod.POST, "/timboxSite", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::getTimboxInfo);
        this.addHandler(HttpMethod.POST, "/timboxRegisterSite", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::registerTimboxParcel);
        this.addHandler(HttpMethod.POST, "/registerMultipleInvoice", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), this::registerMultipleInvoice);
        this.addHandler(HttpMethod.POST, "/getCustomerServices", AuthMiddleware.getInstance(), this::getCustomerServices);
        this.addHandler(HttpMethod.POST, "/getInvoicesForPaymentComplement", AuthMiddleware.getInstance(), this::getInvoicesForPaymentComplement);
        this.addHandler(HttpMethod.POST, "/searchAdvancedCFDI", AuthMiddleware.getInstance(), this::searchAdvancedCFDI);
        this.addHandler(HttpMethod.POST, "/getServicesByInvoice", AuthMiddleware.getInstance(), this::getServicesByInvoice);
        this.addHandler(HttpMethod.POST, "/assignCustomerBillingInfo", AuthMiddleware.getInstance(), this::assignCustomerBillingInfo);

        super.start(startFuture);
        if (InvoiceDBV.REGISTER_INVOICES) {
            subscribeSNSInvoice();
        }
    }

    private void serviceDetail(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject()
                    .put("type", request.getParam("type"))
                    .put("code", request.getParam("code"));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.SERVICE_DETAIL);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(),"Found");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getCause());
                }
            });

        }catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex.getCause());
        }
    }

    private void registerParcel(RoutingContext context) {
        try{
            JsonObject body = context.getBodyAsJson()
                .put(CREATED_BY, context.<Integer>get(USER_ID));
            execService(context, ParcelInvoice.ACTION, body, parcelsResult -> {
                String xml = XML.toString(parcelsResult.getString("factura_timbrada"));
                responseOk(context,
                        new JsonObject()
                                .put("Response", xml)
                                .put("cadena_original", parcelsResult.getString("cadena_original"))
                                .put("error", parcelsResult.getString("error"))
                );
            });
        } catch (Exception e) {
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getTimboxInfo(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson()
                    .put(CREATED_BY, context.<Integer>get(USER_ID));

            isEmptyAndNotNull(body, "code");
            isEmptyAndNotNull(body, "type");
            isGraterAndNotNull(body, "billing_id", 0);
            List<String> emailList = normalizeEmails(body);

            if (!emailList.isEmpty()) {
                for (String email : emailList) {
                    if (!isValidEmail(email)) {
                        throw new PropertyValueException("Invalid email format: " + email);
                    }
                }
            }
            body.put("email_list", new JsonArray(emailList));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.GET_TIMBOX_INFO);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(),"Created");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    responseError(context, UNEXPECTED_ERROR, e.getCause());
                }
            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void registerTimboxParcel(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            Integer folio = body.getInteger("parcel_id");
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.REGISTER_TIMBOX_SITE);
            Integer parcel_id = body.getInteger("parcel_id");
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
               try {
                   if (reply.succeeded()) {
                       Message<Object> result = reply.result();


                       JsonObject cfdi_body = new JsonObject().put("cfdi_body",result.body());
                       JsonObject invoice =  cfdi_body.getJsonObject("cfdi_body").getJsonObject("invoice");
                       cfdi_body.put("folio", folio);
                       execService(context, ParcelInvoice.ACTION_INGRESO, cfdi_body, cfdiResult -> {
                           String xml = XML.toString(cfdiResult.getString("factura_timbrada"));
                           invoice.put("cfdiResult", cfdiResult);
                           invoice.put("parcel_id", parcel_id);
                           invoice.put("xml", xml);
                           invoice.put("uuid", cfdiResult.getString("uuid"));
                           invoice.put("serie", cfdiResult.getString("serie"));
                           invoice.put("folio", cfdiResult.getString("folio"));

                           DeliveryOptions optionsUpdate = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.UPDATE_INVOICE_STATUS);
                           vertx.eventBus().send(this.getDBAddress(), invoice, optionsUpdate, replyUpdateInvoice -> {
                               try {
                                   if (replyUpdateInvoice.succeeded()) {
                                       String error = cfdiResult.getString("error");
                                       if(error != null) {
                                           invoice.put("code", body.getString("code"));
                                           DeliveryOptions optionsDeleteInvoice = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.DELETE_INVOICE_WITH_ERROR);
                                           vertx.eventBus().send(this.getDBAddress(), invoice, optionsDeleteInvoice, repDeleteInvoice -> {
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
                                          responseOk(context,
                                            new JsonObject()
                                               .put("Response", xml)
                                               .put("cadena_original", cfdiResult.getString("cadena_original"))
                                               .put("error", cfdiResult.getString("error")));
                                       }
                                   } else {
                                       responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                                   }
                               } catch (Exception e) {
                                   responseError(context, UNEXPECTED_ERROR, e.getCause());
                               }
                           });

                       });
                   }
               } catch (Exception e) {
                   responseError(context, UNEXPECTED_ERROR, e.getCause());
               }
            });
        } catch (Exception e) {
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void registerMultipleInvoice(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            JsonObject referenceService = body.getJsonObject("reference_service");
            vertx.eventBus().send(this.getDBAddress(), body, new DeliveryOptions().addHeader(ACTION, InvoiceDBV.REGISTER_MULTIPLE_INVOICE), reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();

                        JsonObject cfdi_body = new JsonObject().put("cfdi_body",result.body());
                        JsonObject invoice =  cfdi_body.getJsonObject("cfdi_body").getJsonObject("invoice");
                        cfdi_body.put("folio", referenceService.getJsonObject("invoice").getInteger("id"));

                        execService(context, ParcelInvoice.ACTION_INGRESO, cfdi_body, cfdiResult -> {
                            String xml = XML.toString(cfdiResult.getString("factura_timbrada"));
                            invoice.put("cfdiResult", cfdiResult);
                            invoice.put("xml", xml);
                            invoice.put("uuid", cfdiResult.getString("uuid"));
                            invoice.put("serie", cfdiResult.getString("serie"));
                            invoice.put("folio", cfdiResult.getString("folio"));

                            DeliveryOptions optionsUpdate = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.UPDATE_MULTIPLE_INVOICE_STATUS);
                            vertx.eventBus().send(this.getDBAddress(), invoice, optionsUpdate, replyUpdateInvoice -> {
                                try {
                                    if (replyUpdateInvoice.succeeded()) {
                                        String error = cfdiResult.getString("error");
                                        if(error != null) {
                                            invoice.put("code", body.getString("code"));
                                            DeliveryOptions optionsDeleteInvoice = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.DELETE_INVOICE_MULTIPLE_WITH_ERROR);
                                            vertx.eventBus().send(this.getDBAddress(), invoice, optionsDeleteInvoice, repDeleteInvoice -> {
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
                                            responseOk(context,
                                                    new JsonObject()
                                                            .put("Response", xml)
                                                            .put("cadena_original", cfdiResult.getString("cadena_original"))
                                                            .put("error", cfdiResult.getString("error")));
                                        }
                                    } else {
                                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                                    }
                                } catch (Exception e) {
                                    responseError(context, UNEXPECTED_ERROR, e.getCause());
                                }
                            });
                        });
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getCause());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void register(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson()
                    .put(CREATED_BY, context.<Integer>get(USER_ID));

            isEmptyAndNotNull(body, "code");
            isEmptyAndNotNull(body, "type");
            isGraterAndNotNull(body, "billing_id", 0);
            isMailAndNotNull(body, "email");

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.REGISTER);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(),"Created");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getCause());
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Exception e) {
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getUnbilled(RoutingContext context) {
        /*
        * Filter invoices
        * */
        try {
            JsonObject message = new JsonObject();
            HttpServerRequest request = context.request();
            message.put("period", request.getParam("period"));
            message.put("service", request.getParam("service"));
            message.put("purchase_origin", request.getParam("purchase_origin"));
            if(request.getParam("range") != null)
                message.put("range", Integer.valueOf(request.getParam("range")));
            String start = request.getParam("start");
            if (start != null) {
                message.put("start", Integer.valueOf(start));
            }
            String draw = request.getParam("draw");
            if (draw != null) {
                message.put("draw", Integer.valueOf(draw));
            }
            String length = request.getParam("length");
            if (length != null) {
                message.put("length", Integer.valueOf(length));
            }
            String branchofficeID = request.getParam("branchoffice_id");
            if (branchofficeID != null) {
                message.put("branchoffice_id", Integer.valueOf(branchofficeID));
            }
            if (request.getParam("init_date") != null) {
                message.put("init_date", request.getParam("init_date"));
            }
            if (request.getParam("end_date") != null) {
                message.put("end_date", request.getParam("end_date"));
            }
            if (request.getParam("service_subtype") != null) {
                message.put("service_subtype", request.getParam("service_subtype"));
            }

            vertx.eventBus().send(this.getDBAddress(), message,
                    options(GET_UNBILLED), reply -> {
                        try{
                            if(reply.failed()){
                                throw  new Exception(reply.cause());
                            }
                            responseDatatable(context, reply.result().body());
                        }catch(Exception e){
                            e.printStackTrace();
                            responseDatatable(context, new JsonObject()
                                .put("draw", 0)
                                .put("recordsTotal", 0)
                                .put("recordsFiltered", 0)
                                .put("data", new JsonArray()));
                        }

                    });
        } catch(Exception ex) {
            ex.printStackTrace();
            responseDatatable(context, new JsonObject()
                    .put("draw", 0)
                    .put("recordsTotal", 0)
                    .put("recordsFiltered", 0)
                    .put("data", new JsonArray()));
        }
    }

    private void registerGlobal(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson()
                    .put(CREATED_BY, context.<Integer>get(USER_ID));
            isEmptyAndNotNull(body, "service");
            isEmptyAndNotNull(body, "purchase_origin");
            isGraterAndNotNull(body, "total", 0);
            isGraterAndNotNull(body, "count", 0);
            isEmptyAndNotNull(body, "periodicity");
            isEmptyAndNotNull(body, "init_date");
            isEmptyAndNotNull(body, "end_date");
            isEmptyAndNotNull(body, "invoice_date");

            String serviceSubtype = body.getString("service_subtype", "");
            if (serviceSubtype.isEmpty() && body.containsKey("branchoffice_id")) {
                isGraterAndNotNull(body, "branchoffice_id", 0);
            }

            vertx.eventBus().send(this.getDBAddress(), body, options(InvoiceDBV.REGISTER_GLOBAL), reply -> {
                try {
                    if (reply.succeeded()) {
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            JsonObject cfdi_body = new JsonObject().put("cfdi_body", reply.result().body());
                            if(body.containsKey("dateLastRange")) {
                                cfdi_body.put("dateLastRange", body.getString("dateLastRange"));
                            }

                            execService(context, ParcelInvoice.ACTION_INGRESO_GLOBAL, cfdi_body, cfdiResult -> {
                                String xml = XML.toString(cfdiResult.getString("factura_timbrada"));
                                cfdi_body.put("xml", xml);
                                cfdi_body.put("cfdiResult",cfdiResult);
                                cfdi_body.put("uuid", cfdiResult.getString("uuid"));
                                cfdi_body.put("serie", cfdiResult.getString("serie"));
                                cfdi_body.put("folio", cfdiResult.getString("folio"));

                                DeliveryOptions optionsUpdate = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.UPDATE_INVOICE_GLOBAL);
                                vertx.eventBus().send(this.getDBAddress(), cfdi_body, optionsUpdate, replyUpdateInvoice -> {
                                    try {
                                        if (replyUpdateInvoice.succeeded()) {
                                            responseOk(context,
                                                    new JsonObject()
                                                            .put("Response", xml)
                                                            .put("cfdi_body", cfdi_body)
                                                            .put("cadena_original", cfdiResult.getString("cadena_original"))
                                                            .put("error", cfdiResult.getString("error"))
                                            );
                                        } else {
                                            responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                                        }
                                    } catch (Exception e) {
                                        responseError(context, UNEXPECTED_ERROR, e.getCause());
                                    }
                                });
                            });
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception ex) {
                    responseError(context, UNEXPECTED_ERROR, ex.getCause());
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex.getCause());
        }
    }

    private void exportGlobal(RoutingContext context){
        try {
            JsonObject body = context.getBodyAsJson()
                    .put(CREATED_BY, context.<Integer>get(USER_ID));

            isEmptyAndNotNull(body, "service");
            isEmptyAndNotNull(body, "init_date");
            isEmptyAndNotNull(body, "end_date");
            isEmptyAndNotNull(body, "purchase_origin");
            isGraterAndNotNull(body, "total", 0);
            isGraterAndNotNull(body, "count", 0);

            vertx.eventBus().send(this.getDBAddress(), body, options(InvoiceDBV.EXPORT_GLOBAL), reply -> {
                try {
                    if (reply.succeeded()) {
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(),"Created");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception ex) {
                    responseError(context, UNEXPECTED_ERROR, ex.getCause());
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex.getCause());
        }
    }
    private void getGlobalInvoiceCsv(RoutingContext context){
        try{
            JsonObject body = new JsonObject();
            HttpServerRequest request = context.request();
            body.put("fileName", request.getParam("fileName"));
            isEmptyAndNotNull(body, "fileName");
            vertx.eventBus().send(this.getDBAddress(), body, options(InvoiceDBV.GET_INVOICE_CSV), reply->{
               try{
                   if(reply.failed()){
                       throw new Exception(reply.cause());
                   }
                   Buffer buffer = Buffer.buffer((byte[]) reply.result().body());
                   context.response()
                           .putHeader(HttpHeaders.CONTENT_TYPE, "attachment/".concat(".csv"))
                           .putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(buffer.length()))
                           .write(buffer)
                           .end();
               } catch (Exception ex){
                   ex.printStackTrace();
                   responseError(context, UNEXPECTED_ERROR, ex.getCause());
               }
            });
        }catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        }catch (Exception ex){
            responseError(context, UNEXPECTED_ERROR, ex.getCause());
        }
    }

    private void getGlobalInvoiceXML(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject()
                    .put("type", request.getParam("type"))
                    .put("id", request.getParam("id"));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.GET_GLOBAL_INVOICE_XML);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(),"Found");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getCause());
                }
            });

        }catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex.getCause());
        }
    }

    private void getXML(RoutingContext context) {
        try {
            HttpServerRequest request = context.request();
            JsonObject body = new JsonObject()
                    .put("type", request.getParam("type"))
                    .put("id", request.getParam("id"));
            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.GET_INVOICE_XML);
            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        if (reply.result().headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, reply.result().body());
                        } else {
                            responseOk(context, reply.result().body(),"Found");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getCause());
                }
            });

        }catch (Exception ex) {
            responseError(context, UNEXPECTED_ERROR, ex.getCause());
        }
    }


    private void subscribeSNSInvoice() {
        JsonObject _config = config();
        try {
            // Create a client
            String topic = _config.getString("aws_sns_topic_invoice");
            String topicARN = "arn:aws:sns:us-east-2:738558694912:"+ topic;

            BasicAWSCredentials credentials = new BasicAWSCredentials(
                    _config.getString("aws_sns_access_key"),
                    _config.getString("aws_sns_secret_key")
            );
            amazonSNSClient = AmazonSNSClientBuilder.standard()
                    .withRegion(Regions.US_EAST_2)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();

            // Subscribe to topic
            String endpoint = "https://".concat(_config.getString("self_server_host"))
                    .concat("/invoices/snsNotification");
            SubscribeRequest subscribeReq = new SubscribeRequest()
                    .withTopicArn(topicARN)
                    .withProtocol("https")
                    .withEndpoint(endpoint);
            System.out.println("Subscribing to: ".concat(topicARN).concat(" | with endpoint: ").concat(endpoint));
            amazonSNSClient.subscribe(subscribeReq);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveSNSInvoice(RoutingContext context) {
        HttpServerResponse res = context.response();
        try {
            // Create a client
            String topicARN = "arn:aws:sns:us-east-2:738558694912:FacturacionSNSTopic";
            JsonObject message = context.getBodyAsJson();
            System.out.println("message: ".concat(message.encodePrettily()));

            String token = message.getString("Token");
            if (token != null) {
                // Confirm subscription
                ConfirmSubscriptionRequest confirmReq = new ConfirmSubscriptionRequest()
                        .withTopicArn(topicARN)
                        .withToken(token);
                amazonSNSClient.confirmSubscription(confirmReq);
                System.out.println("Confirm subscription to topic: ".concat(topicARN));
                res.setStatusCode(200).end("OK");
            } else {
                String msg = message.getString("Message");
                System.out.println("Getting SNS message: ".concat(msg));
                JsonObject value = new JsonObject(msg);
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.CONFIRM_REGISTER);

                String status = value.getString("status");
                if (status != null && status.equalsIgnoreCase("OK")) {
                    System.out.println("Confirm register invoice: ".concat(msg));
                    Integer instanceId = (Integer) value.remove("empresa");
                    value.put("instance_id", instanceId);
                    vertx.eventBus().send(this.getDBAddress(), value, options, reply -> {
                        try {
                            if (reply.succeeded()) {
                                Message<Object> result = reply.result();
                                if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                                    res.setStatusCode(500).end(result.body().toString());
                                } else {
                                    res.setStatusCode(200).end("OK");
                                }
                            } else {
                                reply.cause().printStackTrace();
                                res.setStatusCode(500).end(reply.cause().getMessage());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            res.setStatusCode(500).end(reply.cause().getMessage());
                        }

                    });
                } else {
                    // TODO: What to do on wrapper exceptions?
                    System.out.println("Status is not OK: ".concat(value.encodePrettily()));
                    res.setStatusCode(200).end("OK");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatusCode(500).end(e.getMessage());
        }
    }

    private void associate(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson()
                    .put(CREATED_BY, context.<Integer>get(USER_ID));

            isEmptyAndNotNull(body, "code");
            isEmptyAndNotNull(body, "document_id");
            isEmptyAndNotNull(body, "pdf_document_name");
            isEmptyAndNotNull(body, "xml_document_name");
            isMailAndNotNull(body, "email");

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.ASSOCIATE);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(),"Created");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e.getCause());
                }
            });
        } catch (PropertyValueException ex) {
            responsePropertyValue(context, ex);
        } catch (Exception e) {
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void disassociate(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson()
                    .put(UPDATED_BY, context.<Integer>get(USER_ID));

            isEmptyAndNotNull(body, "code");

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.DISASSOCIATE);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(),"Updated");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e);
                }
            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void disassociateGlobal(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson()
                    .put(UPDATED_BY, context.<Integer>get(USER_ID));

            isGraterAndNotNull(body, "invoice_id", 0);

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.DISASSOCIATE_GLOBAL);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(),"Updated");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause().getMessage());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e);
                }
            });
        } catch (PropertyValueException ex) {
            ex.printStackTrace();
            responsePropertyValue(context, ex);
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getCustomerServices(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            try {
                isDateTimeAndNotNull(body, "init_date", "");
                isDateTimeAndNotNull(body, "end_date", "");
                isGraterAndNotNull(body, "customer_id", 0);
                isEmpty(body, "service_type");
                isGraterAndNotNull(body, LIMIT, 0);
                isGraterAndNotNull(body, PAGE, 0);

                try {
                    DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.GET_CUSTOMER_SERVICES);
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
            } catch (PropertyValueException ex) {
                UtilsResponse.responsePropertyValue(context, ex);
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getInvoicesForPaymentComplement(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.GET_INVOICES_FOR_PAYMENT_COMPLEMENT);
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
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void searchAdvancedCFDI(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            try {
                isDateTimeAndNotNull(body, "init_date", "");
                isDateTimeAndNotNull(body, "end_date", "");
                isEmpty(body, "cfdi_type");
                isGraterAndNotNull(body, LIMIT, 0);
                isGraterAndNotNull(body, PAGE, 0);

                try {
                    DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.SEARCH_ADVANCED_CFDI);
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
            } catch (PropertyValueException ex) {
                UtilsResponse.responsePropertyValue(context, ex);
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void getServicesByInvoice(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            try {
                DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, InvoiceDBV.GET_SERVICES_BY_INVOICE);
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
        } catch (Exception e) {
            e.printStackTrace();
            responseError(context, UNEXPECTED_ERROR, e.getMessage());
        }
    }

    private void assignCustomerBillingInfo(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            isGraterAndNotNull(body, "service_id", 0);
            isEmptyAndNotNull(body, "type");
            isGraterAndNotNull(body, _CUSTOMER_BILLING_INFORMATION_ID, 0);
            body.put(USER_ID, context.<Integer>get(USER_ID));

            DeliveryOptions options = new DeliveryOptions()
                    .addHeader(ACTION, InvoiceDBV.ASSIGN_CUSTOMER_BILLING_INFO);

            vertx.eventBus().send(this.getDBAddress(), body, options, reply -> {
                try {
                    if (reply.succeeded()) {
                        Message<Object> result = reply.result();
                        if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
                            responseWarning(context, INVALID_DATA, INVALID_DATA_MESSAGE, result.body());
                        } else {
                            responseOk(context, result.body(), "Assigned");
                        }
                    } else {
                        responseError(context, UNEXPECTED_ERROR, reply.cause());
                    }
                } catch (Exception e) {
                    responseError(context, UNEXPECTED_ERROR, e);
                }

            });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (Exception e) {
            responseError(context, UNEXPECTED_ERROR, e);
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

    private List<String> normalizeEmails(JsonObject body) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();

        String emailField = body.getString("email");
        if (emailField != null) {
            java.util.Arrays.stream(emailField.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(out::add);
        }

        io.vertx.core.json.JsonArray emailsArr = body.getJsonArray("emails");
        if (emailsArr != null) {
            for (int i = 0; i < emailsArr.size(); i++) {
                Object v = emailsArr.getValue(i);
                if (v instanceof String) {
                    String s = ((String) v).trim();
                    if (!s.isEmpty()) out.add(s);
                }
            }
        }
        return new java.util.ArrayList<>(out);
    }
}
