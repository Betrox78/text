package database.authorizationcodes;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.configs.GeneralConfigDBV;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import service.commons.MailVerticle;
import utils.UtilsDate;
import utils.UtilsID;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import static database.configs.GeneralConfigDBV.ACTION_GET_CONFIG_BY_FIELD;
import static service.commons.Constants.*;

public class AuthorizationCodesDBV extends DBVerticle {

    // Actions
    public static final String REGISTER = "AuthorizationCodesDBV.register";
    public static final String VALIDATE = "AuthorizationCodesDBV.validate";

    // Constants
    public static final String ID = "id";
    public static final String CODE = "code";
    public static final String VIGENCY = "vigency";
    public static final String TYPE = "type";
    public static final String SCH_ROUTE_DEST_ID = "schedule_route_destination_id";
    private static final String CODE_STATUS = "code_status";
    public static final String AUTHORIZATED_BY = "authorizated_by";
    public static final String CONTENT = "content";
    private static final String FULL_NAME = "full_name";
    private static final String EMAIL = "email";
    private static final String REQUEST = "request";
    private static final String AUTHORIZE = "authorize";
    private static final String AVAILABLE = "available";

    @Override
    public String getTableName() {
        return "authorization_codes";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case REGISTER:
                this.register(message);
                break;
            case VALIDATE:
                this.validate(message);
                break;
        }
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body();
            JsonObject authorization = body.copy();
            String code = UtilsID.generateShortID("A");
            this.checkAuthorizationCode(code).whenComplete((resultCheckCode, errorCheckCode) -> {
                try{
                    if(errorCheckCode != null){
                        this.rollback(conn, errorCheckCode, message);
                    } else if(resultCheckCode){
                        authorization.put("code", code);
                        authorization.remove(CONTENT);
                        vertx.eventBus().send(GeneralConfigDBV.class.getSimpleName(),
                                new JsonObject().put("fieldName", "authorization_codes_vigency"),
                                new DeliveryOptions().addHeader(ACTION, ACTION_GET_CONFIG_BY_FIELD), replyVigency -> {
                                    try{
                                        if (replyVigency.succeeded()){
                                            JsonObject resultVigency = (JsonObject) replyVigency.result().body();
                                            Integer hoursVigency = Integer.valueOf(resultVigency.getString("value"));
                                            authorization.put("vigency", UtilsDate.sdfDataBase(UtilsDate.summCalendar(Calendar.HOUR, hoursVigency)));

                                            GenericQuery model = this.generateGenericCreate(authorization);
                                            conn.updateWithParams(model.getQuery(), model.getParams(), (AsyncResult<UpdateResult> reply) -> {
                                                try{
                                                    if (reply.succeeded()) {

                                                        Future f1 = Future.future();
                                                        conn.queryWithParams(QUERY_GET_INFO_EMPLOYEE, new JsonArray().add(body.getInteger(AUTHORIZATED_BY)), f1);

                                                        Future f2 = Future.future();
                                                        conn.queryWithParams(QUERY_GET_INFO_EMPLOYEE, new JsonArray().add(body.getInteger(CREATED_BY)), f2);

                                                        CompositeFuture.all(f1, f2).setHandler(resultInfoEmployees -> {
                                                            try{
                                                                if (resultInfoEmployees.succeeded()) {
                                                                    JsonObject manager = resultInfoEmployees.result().<ResultSet>resultAt(0).getRows().get(0);
                                                                    JsonObject petitioner = resultInfoEmployees.result().<ResultSet>resultAt(1).getRows().get(0);
                                                                    this.sendAuthorizationCodeEmail(conn, message, manager.getString(FULL_NAME), petitioner.getString(FULL_NAME), authorization.getString(CODE), manager.getString(EMAIL), body.getString(TYPE), body.getJsonArray(CONTENT), body.getInteger(SCH_ROUTE_DEST_ID));
                                                                } else {
                                                                    this.rollback(conn, resultInfoEmployees.cause(), message);
                                                                }
                                                            } catch(Exception e){
                                                                this.rollback(conn, e, message);
                                                            }
                                                        });
                                                    } else {
                                                        this.rollback(conn, reply.cause(), message);
                                                    }
                                                } catch(Exception e){
                                                    this.rollback(conn, e, message);
                                                }
                                            });
                                        } else {
                                            this.rollback(conn, replyVigency.cause(), message);
                                        }
                                    } catch(Exception e){
                                        this.rollback(conn, e, message);
                                    }
                                });
                    } else {
                        this.register(message);
                    }
                } catch (Exception e){
                    this.rollback(conn, e, message);
                }
            });
        });
    }

    private CompletableFuture<Boolean> checkAuthorizationCode(String code){
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        dbClient.queryWithParams(QUERY_CHECK_AUTHORIZATION_CODE, new JsonArray().add(code), reply -> {
            try{
                if (reply.succeeded()){
                    if (reply.result().getRows().isEmpty()){
                        future.complete(true);
                    } else {
                        future.complete(false);
                    }
                } else {
                    future.completeExceptionally(reply.cause());
                }
            } catch (Exception e){
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    protected CompletableFuture<String> getBoardingContent(SQLConnection con, JsonArray content, Integer schRouteDestId){
        CompletableFuture<String> future = new CompletableFuture<>();
        con.queryWithParams(QUERY_GET_TRAVEL_DETAIL_EMAIL, new JsonArray().add(schRouteDestId), reply -> {
            if(reply.succeeded()){
                JsonObject result = reply.result().getRows().get(0);
                String append = "<b class='text-center'>" +
                        result.getString("vehicle_brand") + " " + result.getString("vehicle_name") + " " + result.getString("vehicle_economic_number") +
                        "</b><br>" +
                        "<b class='text-center'>" +
                        result.getString("origin_prefix") + " | " + result.getString("destiny_prefix") +
                        "</b><br>" +
                        "<b class='text-center'>" +
                        result.getString("travel_date") + " | " + result.getString("arrival_date") +
                        "</b><br>" +
                        "<b class='text-center'>" +
                        result.getString("travel_time") + " | " + result.getString("arrival_time") +
                        "</b><br><br>";
                append += "<table style='width:70%'>\n" +
                        "  <tr>\n" +
                        "    <th align='left'>Tipo</th>\n" +
                        "    <th>Solicitados</th> \n" +
                        "    <th>Disponibles</th>\n" +
                        "    <th>Por autorizar</th>\n" +
                        "  </tr>";
                for(int i = 0; i < content.size(); i++){
                    JsonObject ticket = content.getJsonObject(i);
                    String type = ticket.getString(TYPE);
                    Integer request = ticket.getInteger(REQUEST);
                    Integer available = ticket.getInteger(AVAILABLE);
                    Integer authorize = request-available;
                    append += "<tr>\n" +
                            "    <td>"+type+"</td>\n" +
                            "    <td align='center'>"+request+"</td> \n" +
                            "    <td align='center'>"+available+"</td> \n" +
                            "    <td align='center'>"+authorize+"</td>\n" +
                            "  </tr>";
                }
                append += "</table>";
                future.complete(append);
            } else {
                future.completeExceptionally(reply.cause());
            }
        });
        return future;
    }

    protected String getParcelContent(){
        return "";
    }

    private void sendAuthorizationCodeEmail(SQLConnection con, Message<JsonObject> message,
                                            String manager, String petitioner, String code, String email, String type, JsonArray content, Integer schRouteDestId){
        JsonObject dataEmail = new JsonObject()
                .put("manager", manager)
                .put("petitioner", petitioner)
                .put("code", code);
        if((type != null && type.equals("boarding")) || type == null){
            this.getBoardingContent(con, content, schRouteDestId).whenComplete((resultBC, errorBC) -> {
                if (errorBC != null){
                    this.rollback(con, errorBC, message);
                } else {
                    dataEmail.put("type_service", "viajes")
                            .put("content", resultBC);
                    this.sendEmail(con, message, email, dataEmail);
                }
            });
        } else if(type != null && type.equals("parcel")){
            dataEmail.put("type_service", "paquetería")
                    .put("content", this.getParcelContent());
            this.sendEmail(con, message, email, dataEmail);
        }
    }

    private void sendEmail(SQLConnection con, Message<JsonObject> message, String email, JsonObject dataEmail){
        DeliveryOptions options = new DeliveryOptions()
                .addHeader(ACTION, MailVerticle.ACTION_SEND_HTML_TEMPLATE_MAIL_MONGODB);
                this.vertx.eventBus().send(MailVerticle.class.getSimpleName(),  new JsonObject()
                        .put("template", "authorizationcodeemail.html")
                        .put("to", email)
                        .put("subject", "Solicitud de autorización")
                        .put("body", dataEmail), options, replySend -> {
                                try{
                                    if (replySend.succeeded()){
                                        this.commit(con, message, new JsonObject()
                                                .put("email_status", replySend.succeeded()));
                                    } else {
                                        this.rollback(con, replySend.cause(), message);
                                    }
                                } catch(Exception e){
                                    this.rollback(con, e, message);
                                }
                });
    }

    private void validate(Message<JsonObject> message) {
        this.startTransaction(message, (SQLConnection conn) -> {
            JsonObject body = message.body();
            String code = body.getString(CODE);

            dbClient.queryWithParams(QUERY_GET_AUTHORIZATION_CODE, new JsonArray().add(code), replyCode -> {
                try{
                    if(replyCode.failed()){
                        throw  new Exception(replyCode.cause());
                    }
                    if (replyCode.result().getRows().isEmpty()){
                        this.rollback(conn, new Throwable("Authorization code not found"), message);
                    } else {
                        JsonObject resultCode = replyCode.result().getRows().get(0);
                        String codeStatus = resultCode.getString(CODE_STATUS);
                        if (codeStatus.equals("active")){

                            DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            Date vigency = null;
                            try {
                                vigency = format.parse(resultCode.getString(VIGENCY));
                            } catch (ParseException e) {
                                this.rollback(conn, e, message);
                            }
                            if(vigency.after(new Date())){
                                resultCode.put(CODE_STATUS, "used");
                                resultCode.put(UPDATED_BY, body.getInteger(UPDATED_BY));
                                resultCode.put(UPDATED_AT, body.getString(UPDATED_AT));
                                GenericQuery model = this.generateGenericUpdate(getTableName(), resultCode);
                                conn.updateWithParams(model.getQuery(), model.getParams(), (AsyncResult<UpdateResult> reply) -> {
                                    if (reply.succeeded()) {
                                        this.commit(conn, message, new JsonObject().put("valid", true).put("message", "The authorization code has been validated successfully"));
                                    } else {
                                        this.rollback(conn, reply.cause(), message);
                                    }
                                });
                            } else {
                                this.setExpireAuthorizationCode(conn, message, resultCode);
                            }
                        } else if(codeStatus.equals("used")){
                            this.commit(conn, message, new JsonObject().put("valid", false).put("message", "The authorization code has already been used"));
                        } else if(codeStatus.equals("expired")){
                            this.commit(conn, message, new JsonObject().put("valid", false).put("message", "The authorization code has expired"));
                        }
                    }

                }catch(Exception e){
                    this.rollback(conn, replyCode.cause(), message);

                }

            });
        });
    }

    private void setExpireAuthorizationCode(SQLConnection conn, Message<JsonObject> message, JsonObject body){
        body.put(CODE_STATUS, "expired");
        GenericQuery model = this.generateGenericUpdate(getTableName(), body);
        conn.updateWithParams(model.getQuery(), model.getParams(), (AsyncResult<UpdateResult> reply) -> {
            try{
                if(reply.failed()){
                    throw  new Exception(reply.cause());
                }
                this.commit(conn, message, new JsonObject().put("valid", false).put("message", "The authorization code has expired"));


            }catch(Exception e){
                this.rollback(conn, reply.cause(), message);

            }

        });
    }

    private static String QUERY_CHECK_AUTHORIZATION_CODE = "SELECT id FROM authorization_codes WHERE code = ? AND status = 1 AND code_status = 'active'";

    private static String QUERY_GET_INFO_EMPLOYEE = "SELECT CONCAT(name, ' ', last_name) AS full_name, email FROM employee WHERE id = ?;";

    private static String QUERY_GET_AUTHORIZATION_CODE = "SELECT id, code, code_status, vigency, status  FROM authorization_codes WHERE code = ? AND status = 1;";

    private static String QUERY_GET_TRAVEL_DETAIL_EMAIL = "SELECT \n" +
            " DATE_FORMAT(srd.travel_date, \"%d/%m/%Y\") AS travel_date,\n" +
            " TIME_FORMAT(srd.travel_date, \"%H:%i %p\") AS travel_time,\n" +
            " DATE_FORMAT(srd.arrival_date, \"%d/%m/%Y\") AS arrival_date,\n" +
            " TIME_FORMAT(srd.arrival_date, \"%H:%i %p\") AS arrival_time,\n" +
            " bo.prefix AS origin_prefix,\n" +
            " bd.prefix AS destiny_prefix,\n" +
            " v.name AS vehicle_name,\n" +
            " v.economic_number AS vehicle_economic_number,\n" +
            " v.brand AS vehicle_brand\n" +
            " FROM schedule_route_destination srd \n" +
            " LEFT JOIN branchoffice bo ON srd.terminal_origin_id = bo.id \n" +
            " LEFT JOIN branchoffice bd ON srd.terminal_destiny_id = bd.id \n" +
            " LEFT JOIN schedule_route sr ON srd.schedule_route_id = sr.id \n" +
            " LEFT JOIN boarding_pass_route bpr ON srd.id = bpr.schedule_route_destination_id \n" +
            " LEFT JOIN vehicle v ON sr.vehicle_id = v.id \n" +
            " WHERE srd.id = ?;";
}
