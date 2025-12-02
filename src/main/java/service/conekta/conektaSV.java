    /*
 * To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service.conekta;

import database.commons.ErrorCodes;
import database.conekta.conektaDBV;
import database.configs.GeneralConfigDBV;
import database.e_wallet.EwalletDBV;
import database.promos.PromosDBV;
import io.conekta.Conekta;
import io.conekta.ConektaList;
import io.conekta.Customer;
import io.conekta.Error;
import io.conekta.ErrorList;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.JSONObject;

import static database.conekta.conektaDBV.SAVE_PAYMENT;
import static database.promos.PromosDBV.FLAG_PROMO;

import service.commons.ServiceVerticle;
import service.commons.middlewares.AuthMiddleware;
import service.commons.middlewares.PromosMiddleware;
import service.commons.middlewares.PublicRouteMiddleware;
import utils.*;

import static service.commons.Constants.*;
import static service.commons.Constants.USER_ID;
import static utils.UtilsResponse.responseError;
import static utils.UtilsResponse.responseInvalidToken;
import static utils.UtilsResponse.responseOk;
import static utils.UtilsResponse.responseWarning;
import static utils.UtilsValidation.INVALID_FORMAT;
import utils.UtilsValidation.PropertyValueException;

/**
 *
 * @author Saul
 */
public class conektaSV extends ServiceVerticle {

    @Override
    protected String getDBAddress() {
        return conektaDBV.class.getSimpleName();
    }

    @Override
    protected String getEndpointAddress() {
        return "/conekta";
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Conekta.setApiVerion("2.0.0");
        Conekta.setApiKey(config().getString("conekta_api_key"));

        this.addHandler(HttpMethod.POST, "/pay", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), PromosMiddleware.getInstance(vertx), this::getOrderInfo);
        this.addHandler(HttpMethod.POST, "/recharge_e_wallet", PublicRouteMiddleware.getInstance(), AuthMiddleware.getInstance(), PromosMiddleware.getInstance(vertx), this::rechargeEwallet);
        super.start(startFuture);
    }

    private void getOrderInfo(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();
            // Validar datos del body aqui
            if (body.getInteger("idOrder") == null) {
                throw new PropertyValueException("idOrder", INVALID_FORMAT);
            }
            if (body.getString("token_id") == null) {
                throw new PropertyValueException("token_id", INVALID_FORMAT);
            }

            JsonArray payments = body.getJsonArray("payments");
            if(payments==null || !payments.getJsonObject(0).containsKey("amount") || payments.size()>1 ){
                throw new PropertyValueException("payments", INVALID_FORMAT);
            }
            UtilsValidation.isGrater(payments.getJsonObject(0), "amount", -1);

            String customerName = body.getString("name");
            Integer customerID = body.getInteger("idCustomer");
            body.put("id", body.getInteger("idOrder")); // Boarding_pass_id
            body.put("idCustomer", customerID);
            body.put(FLAG_PROMO, context.<Boolean>get(FLAG_PROMO));
            body.put(UPDATED_BY,context.<Integer>get(USER_ID));
            String token_id = body.getString("token_id"); // token_id Card

            Future<Message<JsonArray>> futureAcumTickets = Future.future();
            Future<Message<JsonArray>> futureOrderInfo = Future.future();
            Future<Message<JsonArray>> futureCustomerInfo = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();
            Future<Message<JsonObject>> f3 = Future.future();
            Future<Message<JsonObject>> f4 = Future.future();
            Future<Message<JsonObject>> f5 = Future.future();
            Future<Message<JsonObject>> f6 = Future.future();

            DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, conektaDBV.GET_ORDER_INFO);
            DeliveryOptions optionsAcum = new DeliveryOptions().addHeader(ACTION, conektaDBV.GET_TOTAL_AMOUNT);
            DeliveryOptions optionsCustomer = new DeliveryOptions().addHeader(ACTION, conektaDBV.GET_CUSTOMER_INFO);

            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "iva"), new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "currency_id"), new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f3.completer());
            vertx.eventBus().send(this.getDBAddress(), body, optionsAcum, futureAcumTickets.completer());
            vertx.eventBus().send(this.getDBAddress(), body, options, futureOrderInfo.completer());
            vertx.eventBus().send(this.getDBAddress(), body, optionsCustomer, futureCustomerInfo.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "expire_open_tickets_after"), new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f4.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "bonus_reference_e_wallet_percent"), new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f5.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "boarding_pass_e_wallet_percent"),new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f6.completer());

            List<Future> futures = new ArrayList<>();
            futures.add(futureAcumTickets);
            futures.add(futureOrderInfo);
            futures.add(futureCustomerInfo);
            futures.add(f2);
            futures.add(f3);
            futures.add(f4);
            futures.add(f5);
            futures.add(f6);

            CompositeFuture.all(futures)
                    .setHandler(reply -> {
                        try {
                            if (reply.succeeded()) {
                                String amountCents = "";
                                String exchange = "";
                                String currency_id = "";
                                String currency = "";

                                Message<JsonObject> messageAcumTickets = reply.result().resultAt(0);

                                Message<JsonArray> messageOrder = reply.result().resultAt(1);
                                Message<JsonObject> messageCustomerInfo = reply.result().resultAt(2);
                                Message<JsonObject> ivaPercentMsg = reply.result().resultAt(3);
                                Message<JsonObject> currencyMsg = reply.result().resultAt(4);
                                Message<JsonObject> expireOpenTicketsAfterMsg = reply.result().resultAt(5);
                                Message<JsonObject> bonusReferralMsg = reply.result().resultAt(6);
                                Message<JsonObject> bonusBoardingPassMsg = reply.result().resultAt(7);

                                JsonObject expireOpenTicketsAfterObj = expireOpenTicketsAfterMsg.body();
                                Integer expireOpenTicketsAfter = Integer.parseInt(expireOpenTicketsAfterObj.getString(VALUE));
                                body.put("expire_open_tickets_after", expireOpenTicketsAfter);

                                JsonArray orderInfo = messageOrder.body();
                                JsonObject ivaPercentO = ivaPercentMsg.body();
                                JsonObject bonusReferralO = bonusReferralMsg.body();
                                JsonObject bonusBpWalletO = bonusBoardingPassMsg.body();
                                final Double ivaPercent = Double.parseDouble(ivaPercentO.getValue("value").toString());
                                final Double bonusReferral = Double.parseDouble(bonusReferralO.getValue("value").toString());
                                final Double bonusBpWallet = Double.parseDouble(bonusBpWalletO.getValue("value").toString());

                                JsonObject resultAcum = messageAcumTickets.body();
                                String amountAcum = resultAcum.getValue("amount").toString();
                                String discountAcum = resultAcum.getValue("discount").toString();
                                String totalAcum = resultAcum.getValue("total_amount").toString();

                                // result CustomerInfo
                                JsonObject datos = messageCustomerInfo.body();
                                if (datos != null) {
                                    if (customerID == null) {
                                        JsonObject first = orderInfo.getJsonObject(0);
                                        datos.put("name", customerName);
                                        datos.put("phone", first.getValue("phone"));
                                        datos.put("email", first.getValue("email"));
                                    }

                                    JSONObject customer = customerCreateJson(datos, token_id);

                                    List<JsonObject> data = new ArrayList<>();

                                    for(int i =0; i<orderInfo.size(); i++ ){
                                        data.add(orderInfo.getJsonObject(i));
                                    }

                                    for (JsonObject dat : data) {
                                        if (dat.containsKey("total_amount")) {
                                            amountCents = String.valueOf(Math
                                                    .round(dat.getDouble("total_amount") * 100));
                                        }
                                        if (dat.containsKey("exchange_id")) {
                                            exchange = String.valueOf((dat.getInteger("exchange_id")));
                                            body.put("exchange_id",exchange);
                                        }
                                        if (dat.containsKey("currency_id")) {
                                            currency_id = String.valueOf((dat.getInteger("currency_id")));
                                        }
                                        if (dat.containsKey("currency")) {
                                            currency = String.valueOf((dat.getString("currency")));
                                        }
                                    }

                                    String order = orderCreate(data, customer, currency, token_id, totalAcum);

                                    Double total = (Double.parseDouble(totalAcum));
                                    body.put("currency",currency_id);
                                    body.put("payment_method","card");
                                    body.put("amount",total);
                                    body.put("finalAmount",amountAcum);
                                    body.put("finalDiscount",discountAcum);
                                    body.put("ivaPercent",ivaPercent);
                                    body.put("finalTotalAmount",totalAcum);
                                    body.put("orderJson", order);
                                    body.put("bonus_referral", bonusReferral);
                                    body.put("bonus_bp", bonusBpWallet);

                                    System.out.println(body);
                                    DeliveryOptions optionsInsertP = new DeliveryOptions()
                                            .addHeader(ACTION, SAVE_PAYMENT);
                                    vertx.eventBus().send(this.getDBAddress(), body, optionsInsertP,
                                            replySavePayment -> {
                                                try{
                                                    if(replySavePayment.failed()){
                                                        throw  new Exception(replySavePayment.cause());
                                                    }
                                                    Message<Object> resultInsert = replySavePayment.result();
                                                    MultiMap headers = resultInsert.headers();
                                                    if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                                        responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                                                    } else {
                                                        responseOk(context, resultInsert.body(), "Paid");
                                                    }
                                                } catch(Exception e) {
                                                    responseError(context, UNEXPECTED_ERROR, replySavePayment.cause().getMessage());
                                                }
                                            });
                                } else {
                                    responseError(context,
                                            "Ocurrió un error inesperado, consulte con el proveedor de sistemas");
                                }
                            } else {
                                responseOk(context, "Not Success");
                            }
                        } catch (ParseException ex) {
                            responseError(context,
                                    "ParseException: Ocurrió un error inesperado, consulte con el proveedor de sistemas",
                                    ex.getCause().getMessage());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            responseError(context, ex.getMessage());
                        }
                    });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (ClassCastException e) {
            responseError(context, "Ocurrió un error en casteo, compruebe el tipo de los datos", e);
        } catch (Exception ex){
            responseError(context, "Ocurrió un error inesperado, contacte al proveedor de sistemas", ex);
        }
    }

    private void SaveIdOrder(String idOrderConekta, int idOrder) {
        JsonObject body = new JsonObject();
        body.put("idOrder", idOrder);
        body.put("idOrderConekta", idOrderConekta);
        DeliveryOptions optionsSaveIdConekta = new DeliveryOptions().addHeader(ACTION, conektaDBV.SAVE_IDORDER);
        vertx.eventBus().send(this.getDBAddress(), body, optionsSaveIdConekta, replySaveIdConekta -> {
            if (replySaveIdConekta.succeeded()) {
                MultiMap headers = replySaveIdConekta.result().headers();
                if (headers.contains(ErrorCodes.DB_ERROR.toString())) {

                } else {

                }
            }
        });
    }

    private void savePayment(String id_boarding_pass, String payment_method, String amount, ConektaList charges,
            String currency_id, String exchange_id) {
        String reference = "";
        String o = charges.get(0).toString();
        int index = o.indexOf("last4=");
        int index_name = o.indexOf("name=");
        int index_exp = o.indexOf("exp_month");
        int index_type = o.indexOf("type=");
        int index_brand = o.indexOf("brand=");
        int index_auth = o.indexOf("auth_code=");

        reference += "Name " + o.substring(index_name + 5, index_exp - 2) + ", ";
        reference += "last4=" + o.substring(index + 6, index + 10) + ", ";
        reference += "type=" + o.substring(index_type + 5, index_brand - 2) + ", ";
        reference += "brand=" + o.substring(index_brand + 6, index_auth - 2) + " ";

        JsonObject body = new JsonObject();

        body.put("reference", reference);
        body.put("currency", currency_id);
        body.put("payment_method", payment_method);
        body.put("amount", String.valueOf(amount));
        body.put("idBoarding_pass", id_boarding_pass);
        body.put("exchange_id", exchange_id);

        DeliveryOptions optionsSaveIdConekta = new DeliveryOptions().addHeader(ACTION, SAVE_PAYMENT);
        vertx.eventBus().send(this.getDBAddress(), body, optionsSaveIdConekta, replySaveIdConekta -> {
            if (replySaveIdConekta.succeeded()) {
                if (replySaveIdConekta.result().body() == null) {
                    System.out.println("Payment Save");
                } else {
                    System.out.println("Payment Info not saved");
                }
            }
        });
    }

    /**
     * Return a JSONObject to create a Customer in Conekta
     * 
     * @param datos         JsonObject
     * @param token_Conekta String
     * @return JSONObject with the info to create a customer in Conekta
     * @throws ParseException
     */
    private JSONObject customerCreateJson(JsonObject datos, String token_Conekta) throws ParseException {

        // Parse String Date to Instant to insert in Conekta
        String s;
        String date = datos.getValue("created_at").toString();
        DateTimeFormatter format = DateTimeFormatter.ISO_INSTANT;
        s = getTimestamp(format.parse(date).toString());

        String name, phone, email, code, state, city, country, street;

        name = datos.getString("name");
        phone = datos.getString("phone");
        email = datos.getString("email");
        code = datos.getString("code");
        state = datos.getString("state");
        city = datos.getString("city");
        country = datos.getString("country");
        street = datos.getString("street");

        if (datos.containsKey("zip_code")) {
            if (datos.getValue("zip_code").toString().equals("")) {
                code = "";
            } else {
                code = String.valueOf(datos.getInteger("zip_code"));
            }
        } else {
            code = "";
        }

        if (datos.containsKey("State")) {
            if (datos.getValue("State") == null || datos.getString("State").equals("")) {
                state = "";
            } else {
                state = datos.getString("State");
            }
        } else {
            state = "";
        }

        if (datos.containsKey("street")) {
            if (datos.getValue("street") == null || datos.getString("street").equals("")) {
                street = "";
            } else {
                String[] streetSplit = datos.getString("street").split(",");
                street = streetSplit[0];
            }
        } else {
            street = "";
        }

        if (datos.containsKey("City")) {
            if (datos.getValue("City") == null || datos.getString("City").equals("")) {
                city = "";
            } else {
                city = datos.getString("City");
            }
        } else {
            city = "";
        }

        if (datos.containsKey("Country")) {
            if (datos.getValue("Country") == null || datos.getString("Country").equals("")) {
                country = "";
            } else {
                country = datos.getString("Country");
            }
        } else {
            country = "";
        }
        s = s.replace(",ISO", "");

        JSONObject asd = new JSONObject(
                "{" + "'name':  '" + name + "', " + "'email': '" + email + "'," + "'phone': '" + phone + "',"
                // + "'plan_id': 'gold-plan',"
                        + "'corporate': false," + "'antifraud_info': {'paid_transactions': 0, " + "'account_created_at': " + s + ","
                        + "'first_paid_at': " + s + "}," + "'payment_sources': [{" + "'token_id': '" + token_Conekta
                        + "'," + "'type': 'card'" + "}]," + "'shipping_contacts': [{" + "'phone': '" + phone + "',"
                        + "'receiver': '" + name + "'," + "'between_streets': '" + street + "'," + "'address': {"
                        + "'street1': '" + street + "'," + "'street2': '" + street + "'," + "'city': '" + city + "',"
                        + "'state': '" + state + "'," + "'country': 'MX'," + "'postal_code': '" + code + "',"
                        + "'residential': true" + "}" + "}]" + "}");

        return asd;
    }

    /**
     * Return the Instant time in milliseconds of the a date in a String with format
     * "2018-08-31T09:15:00Z"
     * 
     * @param data
     * @return
     */
    private String getTimestamp(String data) {

        JsonObject object = new JsonObject();
        data = data.replace("{", "");
        data = data.replace("[", "");
        data = data.replaceAll("]", "");
        data = data.replaceAll("}", "");

        // Esto va a causar problemas en street, el campo street contiene ','
        String[] dat = data.split(", ");

        for (String da : dat) {
            String[] d = da.split("=");
            if (d[0].equals("InstantSeconds")) {
                return d[1].replace(",ISO", "");
            }
        }
        return "";
    }

    /**
     * 
     * @param items  List<JsonObject>
     * @param cus    Conekta.Customer info of the customer to create the Order and
     *               Charge
     * @param token  String contains the data of the card
     * @param amount String total to pay
     * @return A Order in conekta
     * @throws ErrorList
     * @throws Error
     */
    private String orderCreate(List<JsonObject> items, JSONObject cus, String currency, String token, String amount)
            throws ParseException {
        String lineItems = lineItemsCreate(items);
        JSONObject anti = cus.getJSONObject("antifraud_info");

        String cad = "{" + "'currency': '" + currency + "'," + "'metadata': {" + "    'test': true" + "},"
                + "'line_items': " + lineItems + "," + "'customer_info': {" + "'antifraud_info': " + anti.toString() + ","
                + "'email': '" + cus.getString("email") + "'," + "'name': '" + cus.getString("name") + "'," + "'phone': '" + cus.getString("phone") + "'"
                + "}," + "'charges': [{" + "    'payment_method': {" + "        'type': 'card',"
                + "        'token_id': '" + token + "'" + "    }, " + "    'amount': "
                + Integer.valueOf(String.valueOf(Math.round(UtilsMoney.round(Double.parseDouble(amount) * 100,0)))) + "" + "}]" + "}";
        
        return cad;
    }

    /**
     * Return a String with the items to add in a Conekta.Order
     * 
     * @param items List<JsonObject>
     * @return
     */
    public String lineItemsCreate(List<JsonObject> items) throws ParseException {
        String it = "";
        it = it.concat("[");
        List<JsonObject> resultsItems = new ArrayList<>();
        String ticketClass = items.get(0).getString("ticket_class");
        if(ticketClass.equals("abierto_redondo")){
            for(JsonObject item : items){
                Double unitPrice = item.getDouble("unit_price");
                Integer boardingPassPassengerId = item.getInteger("boarding_pass_passenger_id");
                for (JsonObject itemR : items) {
                    Integer boardingPassPassengerIdR = itemR.getInteger("boarding_pass_passenger_id");
                    if (boardingPassPassengerId.equals(boardingPassPassengerIdR) && itemR.getInteger("trip_id") == null && item.getInteger("trip_id") != null) {
                        unitPrice = unitPrice + itemR.getDouble("unit_price");
                        item.put("unit_price", unitPrice);
                        resultsItems.add(item);
                        break;
                    }
                }
            }
            items = resultsItems;
        }
        for (JsonObject item : items) {
            it = it.concat("{");
            it = it.concat("'name': '" + item.getString("name") + "',");
            Integer unit_price = Integer.valueOf(String.valueOf(Math.round(UtilsMoney.round(item.getDouble("unit_price") * 100, 0))));
            it = it.concat("'unit_price': '" + String.valueOf(unit_price) + "',");
            it = it.concat("'quantity': '" + String.valueOf(item.getInteger("quantity")) + "',");
            it = it.concat("'antifraud_info': {");
            it = it.concat("'trip_id': '" + String.valueOf(item.getInteger("trip_id")) + "',");

            String arrives_at, departs_at ;
            String date = item.getString("departs_at");
            //String date = item.getValue("departs_at").toString();
            String date1 = item.getString("arrives_at");
            //String date1 = item.getValue("arrives_at").toString();

            int DAY = 1000 * 60 * 60 * 24;
            departs_at = Long.valueOf((UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(date).getTime() + DAY) / 1000).toString();
            arrives_at = Long.valueOf((UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(date1).getTime() + DAY) / 1000).toString();


            String seat = item.getString("seat_number");

            it = it.concat("'departs_at': " + departs_at + ",");
            it = it.concat("'arrives_at': " + arrives_at + ",");
            it = it.concat("'ticket_class': '" + item.getString("ticket_class") + "',");
            it = it.concat("'seat_number': '" + String.format("%2s", seat).replace(' ', '0') + "',");
            it = it.concat("'origin': '" + item.getString("origin") + "',");
            it = it.concat("'destination': '" + item.getString("destiny") + "',");
            it = it.concat("'passenger_type': '" + item.getString("passenger_type") + "'");
            it = it.concat("}");
            it = it.concat("}");
            if (!items.get(items.size() - 1).equals(item)) {
                it = it.concat(",");
            }
        }
        it = it.concat("]");
        return it;
    }

    private String orderRechargeWalletCreate(JsonObject item, JSONObject cus, String currency, String token, String amount)
            throws ParseException {
        String lineItems = lineItemRechargeCreate(item);
        JSONObject anti = cus.getJSONObject("antifraud_info");

        String cad = "{" + "'currency': '" + currency + "'," + "'metadata': {" + "    'test': true" + "},"
                + "'line_items': " + lineItems + "," + "'customer_info': {" + "'antifraud_info': " + anti.toString() + ","
                + "'email': '" + cus.getString("email") + "'," + "'name': '" + cus.getString("name") + "'," + "'phone': '" + cus.getString("phone") + "'"
                + "}," + "'charges': [{" + "    'payment_method': {" + "        'type': 'card',"
                + "        'token_id': '" + token + "'" + "    }, " + "    'amount': "
                + Integer.valueOf(String.valueOf(Math.round(UtilsMoney.round(Double.parseDouble(amount) * 100,0)))) + "" + "}]" + "}";

        return cad;
    }

    public String lineItemRechargeCreate(JsonObject item) throws ParseException {
        String it = "";
        it = it.concat("[");
        it = it.concat("{");
        it = it.concat("'name': '" + item.getString("name") + "',");
        Integer unit_price = Integer.valueOf(String.valueOf(Math.round(UtilsMoney.round(item.getDouble("recharge_amount") * 100, 0))));
        it = it.concat("'unit_price': '" + String.valueOf(unit_price) + "',");
        it = it.concat("'quantity': '" + String.valueOf(item.getInteger("quantity")) + "',");
        it = it.concat("'antifraud_info': {");

        int DAY = 1000 * 60 * 60 * 24;
        String date = Long.valueOf((new Date().getTime() + DAY) / 1000).toString();
        it = it.concat("'wallet_id': '" + String.valueOf(item.getInteger("id")) + "',");
        it = it.concat("'date': " + date + ",");
        it = it.concat("'wallet_code': '" + item.getString("code") + "',");
        it = it.concat("'user_id': '" + String.valueOf(item.getInteger("user_id")) + "'");
        it = it.concat("}");
        it = it.concat("}");
        it = it.concat("]");
        return it;
    }

    private void rechargeEwallet(RoutingContext context) {
        try {
            JsonObject body = context.getBodyAsJson();

            if (body.getString("token_id") == null) {
                throw new PropertyValueException("token_id", INVALID_FORMAT);
            }
            if (body.getInteger("idCustomer") == null) {
                throw new PropertyValueException("idCustomer", INVALID_FORMAT);
            }
            JsonObject payment = body.getJsonObject("payment");

            if(payment.isEmpty()) {
                throw new PropertyValueException("payments", INVALID_FORMAT);
            }
            UtilsValidation.isGrater(payment, "amount", -1);
            String token_id = body.getString("token_id"); // token_id Card
            Double rechargeAmount = payment.getDouble("amount");

            Future<Message<JsonArray>> futureWalletInfo = Future.future();
            Future<Message<JsonArray>> futureTotalRecharge = Future.future();
            Future<Message<JsonArray>> futureCustomerInfo = Future.future();
            Future<Message<JsonObject>> f1 = Future.future();
            Future<Message<JsonObject>> f2 = Future.future();

            DeliveryOptions optionsWalletInfo = new DeliveryOptions().addHeader(ACTION, EwalletDBV.GET_WALLET_BY_CUSTOMER_ID);
            DeliveryOptions optionsTotalRecharge = new DeliveryOptions().addHeader(ACTION, EwalletDBV.GET_TOTALS_RECHARGE);
            DeliveryOptions optionsCustomerInfo = new DeliveryOptions().addHeader(ACTION, conektaDBV.GET_CUSTOMER_INFO);

            vertx.eventBus().send(EwalletDBV.class.getSimpleName(), body, optionsWalletInfo, futureWalletInfo.completer());
            vertx.eventBus().send(EwalletDBV.class.getSimpleName(), body, optionsTotalRecharge, futureTotalRecharge.completer());
            vertx.eventBus().send(this.getDBAddress(), body, optionsCustomerInfo, futureCustomerInfo.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "currency_id"),
                    new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f1.completer());
            vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                    new JsonObject().put("fieldName", "iva"), new DeliveryOptions()
                            .addHeader(ACTION, GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD), f2.completer());

            CompositeFuture.all(futureWalletInfo, futureTotalRecharge, futureCustomerInfo, f1, f2)
                    .setHandler(reply -> {
                        try {
                            if (reply.succeeded()) {
                                Message<JsonObject> messageWalletInfo = reply.result().resultAt(0);
                                Message<JsonObject> messageTotalsRecharge = reply.result().resultAt(1);
                                Message<JsonObject> messageCustomerInfo = reply.result().resultAt(2);
                                Message<JsonObject> currencyMsg = reply.result().resultAt(3);
                                Message<JsonObject> ivaPercentMsg = reply.result().resultAt(4);

                                JsonObject ivaPercentO = ivaPercentMsg.body();
                                final Double ivaPercent = Double.parseDouble(ivaPercentO.getValue("value").toString());
                                JsonObject walletInfo = messageWalletInfo.body();
                                JsonObject currencyInfo = currencyMsg.body();
                                JsonObject resultTotalsRecharge = messageTotalsRecharge.body();
                                String currency_id = currencyInfo.getString("value");
                                String currency = "MXN";

                                if(walletInfo.isEmpty()) {
                                    responseError(context, "Tu usuario no cuenta con monedero electrónico activado, " +
                                            "activa tu QR para obtener beneficios con tus compras.");
                                } else {
                                    JsonObject customerInfo = messageCustomerInfo.body();
                                    if (customerInfo != null) {
                                        JSONObject customer = customerCreateJson(customerInfo, token_id);
                                        walletInfo.put("name", "Recarga de saldo en monedero electrónico");
                                        walletInfo.put("recharge_amount", rechargeAmount);
                                        walletInfo.put("quantity", 1);

                                        String order = orderRechargeWalletCreate(walletInfo, customer, currency, token_id, rechargeAmount.toString());
                                        body.put("currency", currency_id);
                                        body.put("payment_method","card");
                                        body.put("amount", rechargeAmount);
                                        body.put("finalAmount", rechargeAmount);
                                        body.put("finalDiscount", 0);
                                        body.put("ivaPercent", ivaPercent);
                                        body.put("finalTotalAmount", rechargeAmount);
                                        body.put("orderJson", order);
                                        body.put("wallet", walletInfo);
                                        body.put("totals_recharge", resultTotalsRecharge);

                                        DeliveryOptions optionsInsertP = new DeliveryOptions()
                                                .addHeader(ACTION, conektaDBV.SAVE_WALLET_RECHARGE);
                                        vertx.eventBus().send(this.getDBAddress(), body, optionsInsertP,
                                                replySavePayment -> {
                                                    try{
                                                        if(replySavePayment.failed()){
                                                            throw  new Exception(replySavePayment.cause());
                                                        }
                                                        Message<Object> resultInsert = replySavePayment.result();
                                                        MultiMap headers = resultInsert.headers();
                                                        if (headers.contains(ErrorCodes.DB_ERROR.toString())) {
                                                            responseWarning(context, headers.get(ErrorCodes.DB_ERROR.name()));
                                                        } else {
                                                            responseOk(context, resultInsert.body(), "Paid");
                                                        }

                                                    }catch(Exception e){
                                                        responseError(context, UNEXPECTED_ERROR, e.getMessage());
                                                    }
                                                });
                                    } else {
                                        responseError(context,
                                                "Error al encontrar información del cliente, verifica haber iniciado sesión");
                                    }
                                }
                            } else {
                                responseError(context, "Error al obtener información para recarga");
                            }
                        } catch (ParseException ex) {
                            responseError(context,
                                    "ParseException: Ocurrió un error inesperado, consulte con el proveedor de sistemas",
                                    ex.getCause().getMessage());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            responseError(context, ex.getMessage());
                        }
                    });
        } catch (PropertyValueException ex) {
            UtilsResponse.responsePropertyValue(context, ex);
        } catch (ClassCastException e) {
            responseError(context, "Ocurrió un error en casteo, compruebe el tipo de los datos", e);
        } catch (Exception ex){
            responseError(context, "Ocurrió un error inesperado, contacte al proveedor de sistemas", ex);
        }
    }

}
