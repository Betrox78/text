/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.users;

import database.commons.DBVerticle;
import database.commons.GenericQuery;
import database.permission.PermissionDBV;
import database.users.handlers.PartnerLogin;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import utils.UtilsDate;

import static service.commons.Constants.*;
import static utils.UtilsDate.*;

import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * Kriblet
 */
public class UsersDBV extends DBVerticle {

    public static final String ACTION_LOGIN = "UsersDBV.login";
    public static final String ACTION_FIND_BY_MAIL = "UsersDBV.findByMail";
    public static final String ACTION_FIND_BY_USER_ID = "UsersDBV.findByUserId";
    public static final String ACTION_UPDATE_PASSWORD = "UsersDBV.updatePassword";
    public static final String ACTION_RESET_PASSWORD = "UsersDBV.resetPassword";
    public static final String ACTION_GET_PROFILE_BY_USER_ID = "UsersDBV.getProfileByUserId";
    public static final String ACTION_PROFILES = "UsersDBV.profiles";
    public static final String ACTION_REGISTER = "UsersDBV.register";
    public static final String ACTION_IS_SUPERUSER = "UsersDBV.isSuperUser";
    public static final String ACTION_CHECK_PERMISSION = "UsersDBV.checkPermission";
    public static final String ACTION_DEATIL = "UsersDBV.getDetail";
    public static final String ACTION_BIRTHDAY = "UsersDBV.getBirthday";
    public static final String ACTION_TOKEN_THIRD_PARTY = "UsersDBV.tokenThirdParty";
    public static final String ACTION_SEARCH_BY_EMAIL = "UsersDBV.searchUser";
    public static final String ACTION_PARTNER_LOGIN = "UsersDBV.partnerLogin";
    public static final String ACTION_SEARCH_BY_JOB_NAME = "UsersDBV.getUsersByJobName";
    public static final String ACTION_USER_EXISTS = "UsersDBV.userExists";
    public static final String ACTION_DELETE_USER = "UsersDBV.deleteUser";

    private PartnerLogin partnerLogin;

    @Override
    public String getTableName() {
        return "users";
    }

    @Override
    public void start(Future<Void> future) throws Exception {
        super.start(future);
        this.partnerLogin = new PartnerLogin(this);
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        switch (message.headers().get(ACTION)) {
            case ACTION_LOGIN:
                this.login(message);
                break;
            case ACTION_FIND_BY_MAIL:
                this.findByMail(message);
                break;
            case ACTION_FIND_BY_USER_ID:
                this.findByUserId(message);
                break;
            case ACTION_UPDATE_PASSWORD:
                this.updatePassword(message);
                break;
            case ACTION_RESET_PASSWORD:
                this.resetPassword(message);
                break;
            case ACTION_PROFILES:
                this.profiles(message);
                break;
            case ACTION_REGISTER:
                this.register(message);
                break;
            case ACTION_IS_SUPERUSER:
                this.isSuperUser(message);
                break;
            case ACTION_CHECK_PERMISSION:
                this.checkPermission(message);
                break;
            case ACTION_DEATIL:
                this.getDetail(message);
                break;
            case ACTION_BIRTHDAY:
                this.getBirthday(message);
                break;
            case ACTION_TOKEN_THIRD_PARTY:
                this.tokenThirdParty(message);
                break;
            case ACTION_SEARCH_BY_EMAIL:
                this.searchUser(message);
                break;
            case ACTION_PARTNER_LOGIN:
                this.partnerLogin.handle(message);
                break;
            case ACTION_SEARCH_BY_JOB_NAME:
                this.getUsersByJobName(message);
                break;
            case ACTION_USER_EXISTS:
                this.userExists(message);
                break;
            case ACTION_DELETE_USER:
                this.deleteUser(message);
                break;
            case ACTION_GET_PROFILE_BY_USER_ID:
                this.getProfileByUserId(message);
                break;
        }
    }

    private void login(Message<JsonObject> message) {
        JsonObject body = message.body();
        String userType = body.getString("user_type");
        JsonArray params = new JsonArray()
                .add(body.getString("email"))
                .add(body.getString("pass"));
        Boolean socialAuth = body.getBoolean("social_auth");

        this.dbClient.queryWithParams(QUERY_FIND_BY_MAIL, new JsonArray().add(body.getString("email")), userReply -> {
            if (userReply.result().getNumRows() == 0) {
                message.reply(null);
            } else {
                JsonObject userResult = userReply.result().getRows().get(0);
                if (!userResult.getBoolean("social_auth").equals(socialAuth)) {
                    reportQueryError(message, new Throwable("Incorrect login type"));
                } else {
                    this.dbClient.queryWithParams(QUERY_LOGIN, params, reply -> {
                        if (reply.succeeded()) {
                            if (reply.result().getNumRows() == 0) {
                                message.reply(null);
                            } else {
                                JsonObject result = reply.result().getRows().get(0);
                                if (userType.equals("C")){
                                    if (result.getString("user_type").equals("C")){
                                        message.reply(result);
                                    } else {
                                        reportQueryError(message, new Throwable("Incorrect user type"));
                                    }
                                } else {
                                    if (result.getString("user_type").equals("C")){
                                        reportQueryError(message, new Throwable("Incorrect user type"));
                                    } else {
                                        message.reply(result);
                                    }
                                }
                            }
                        } else {
                            reportQueryError(message, reply.cause());
                        }
                    });
                }
            }
        });
    }

    private void findByMail(Message<JsonObject> message) {
        JsonArray params = new JsonArray()
                .add(message.body().getString("email"));
        this.dbClient.queryWithParams(QUERY_FIND_BY_MAIL, params, reply -> {
            if (reply.succeeded()) {
                if (reply.result().getNumRows() == 0) {
                    message.reply(null);
                } else {
                    message.reply(reply.result().getRows().get(0));
                }
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void findByUserId(Message<JsonObject> message){
        JsonArray params = new JsonArray()
                .add(message.body().getInteger("user_id"))
                .add(message.body().getString("password"));
        this.dbClient.queryWithParams(QUERY_FIND_BY_EMPLOYEE_ID, params , reply ->{
           try{
               if (reply.failed()){
                   throw new Exception(reply.cause());
               }
               if(reply.result().getNumRows() == 0){
                   message.reply(null);
               }else{
                   message.reply("ok");
               }
           }catch (Exception e){
               e.printStackTrace();
               reportQueryError(message , reply.cause());
           }
        });
    }

    private void updatePassword(Message<JsonObject> message) {
        JsonArray params = new JsonArray()
                .add(message.body().getString("new_password"))
                .add(message.body().getString("user_email"));
        this.dbClient.updateWithParams(QUERY_UPDATE_PASSWORD, params, reply -> {
            if (reply.succeeded()) {
                message.reply(null);
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void resetPassword(Message<JsonObject> message){
        JsonArray params = new JsonArray()
                .add(message.body().getString("new_password"))
                .add(message.body().getInteger("user_id"));
        this.dbClient.updateWithParams(QUERY_RESET_PASSWORD, params, reply ->{
            try{
                if (reply.failed()){
                    throw new Exception(reply.cause());
                }
                message.reply("Ok");
            }catch (Exception e ){
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void profiles(Message<JsonObject> message) {
        int id = message.body().getInteger("id");
        this.dbClient.queryWithParams(QUERY_USER_PROFILES, new JsonArray().add(id), r -> {
            if (r.succeeded()) {
                if(r.result().getRows().isEmpty()){
                    reportQueryError(message, new Throwable("Not found"));
                } else {
                    message.reply(r.result().getRows().get(0));
                }
            } else {
                reportQueryError(message, r.cause());
            }
        });
    }

    private void isSuperUser(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer userId = body.getInteger("user_id");
        dbClient.queryWithParams("SELECT id, user_type FROM users WHERE id = ?;", new JsonArray().add(userId), reply -> {
            try{
                if (reply.succeeded()){
                    List<JsonObject> result = reply.result().getRows();
                    if(result.get(0).getString("user_type").equals("S")){
                        message.reply(true);
                    } else {
                        message.reply(false);
                    }
                }
            } catch(Exception e){
                reportQueryError(message, e);
            }
        });
    }

    private void checkPermission(Message<JsonObject> message){
        JsonObject body = message.body();
        Integer userId = body.getInteger(USER_ID);
        String subModuleName = body.getString("sub_module_name");
        String QUERY;
        JsonArray params = new JsonArray().add(subModuleName);

        if(Objects.nonNull(userId)) {
            params.add(userId);
            QUERY = "SELECT\n" +
                    "   p.*\n" +
                    "FROM permission p\n" +
                    "INNER JOIN sub_module sm ON sm.id = p.sub_module_id\n" +
                    "INNER JOIN profile_permission pp ON pp.permission_id = p.id\n" +
                    "INNER JOIN user_permission_branchoffice upb ON upb.permission_id = p.id\n" +
                    "WHERE sm.name = ?\n" +
                    "   AND sm.status = 1\n" +
                    "    AND p.status = 1\n" +
                    "    AND upb.user_id = ?\n" +
                    "GROUP BY p.id;";
        } else {
            QUERY = "SELECT\n" +
                    "   p.*\n" +
                    "FROM permission p\n" +
                    "INNER JOIN sub_module sm ON sm.id = p.sub_module_id\n" +
                    "WHERE sm.name = ?\n" +
                    "   AND sm.status = 1\n" +
                    "    AND p.status = 1\n" +
                    "GROUP BY p.id;";
        }

        dbClient.queryWithParams(QUERY, params, reply -> {
            try{
                if (reply.succeeded()){
                    List<JsonObject> result = reply.result().getRows();
                    message.reply(new JsonArray(result));
                }
            } catch(Exception e){
                reportQueryError(message, e);
            }
        });
    }

    private void getDetail(Message<JsonObject> message) {
        JsonObject body = message.body();
        Integer userId = body.getInteger("id");

        this.dbClient.queryWithParams("SELECT profile_id FROM users WHERE id = ?;", new JsonArray().add(userId), reply -> {
            if (reply.succeeded()) {
                List<JsonObject> resultUser = reply.result().getRows();
                if(resultUser.isEmpty()){
                    reportQueryError(message, new Throwable("The user haven't profile assigned"));
                } else {
                    body.put("profile_id", resultUser.get(0).getInteger("profile_id"));
                    getProfileDetail(body).whenComplete((resultProfile, errorProfile) -> {
                       if (errorProfile != null){
                           reportQueryError(message, errorProfile);
                       } else {
                           message.reply(resultProfile);
                       }
                    });
                }
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private CompletableFuture<Object> getProfileDetail(JsonObject body){
        CompletableFuture<Object> future = new CompletableFuture<>();
        vertx.eventBus().send(PermissionDBV.class.getSimpleName(), body,
                new DeliveryOptions().addHeader(ACTION, PermissionDBV.ACTION_PROFILE_DETAIL), replyDetail -> {
                    try{
                        if(replyDetail.succeeded()){
                            future.complete(replyDetail.result().body());
                        } else {
                            future.completeExceptionally(replyDetail.cause());
                        }
                    } catch (Exception e){
                        future.completeExceptionally(e);
                    }
                });
        return future;
    }

    private void register(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {

                JsonObject user = message.body();
                Boolean dissociate = user.getBoolean("dissociate", false);
                user.remove("dissociate");

                conn.queryWithParams("SELECT * from users where email= ? ".concat(dissociate ? "AND status <> 3" : ""), new JsonArray().add(user.getString("email")), userResult -> {
                    try {
                        if(userResult.failed()){
                            throw userResult.cause();
                        }

                        List<JsonObject> customers = userResult.result().getRows();
                        if(customers.size() > 0){
                            throw new Throwable(customers.get(0).getInteger("status") == 3 ? "Email duplicated. dissociate it." : "Email duplicated.");
                        }

                        conn.updateWithParams("update users u\n" +
                                "SET u.email = IF(u.id IS NOT NULL, CONCAT(u.email, 'DELETED', u.id), u.email)\n" +
                                "where u.email = ? AND u.status = 3", new JsonArray().add(user.getString("email")), updateHandler -> {
                            try {
                                if(updateHandler.failed()){
                                    throw updateHandler.cause();
                                }

                                GenericQuery create = this.generateGenericCreate(user);
                                conn.updateWithParams(create.getQuery(), create.getParams(), createHandler -> {
                                    try {
                                        if(createHandler.failed()){
                                            throw createHandler.cause();
                                        }

                                        final int id = createHandler.result().getKeys().getInteger(0);
                                        this.commit(conn, message, new JsonObject().put("id", id));

                                    }catch (Throwable t){
                                        t.printStackTrace();
                                        this.rollback(conn, t, message);
                                    }
                                });

                            }catch (Throwable t){
                                t.printStackTrace();
                                this.rollback(conn, t, message);
                            }
                        });

                    }catch (Throwable t){
                        t.printStackTrace();
                        this.rollback(conn, t, message);
                    }
                });

            }catch (Throwable t){
                t.printStackTrace();
                this.rollback(conn, t, message);
            }
        });

    }
    private void getBirthday(Message<JsonObject> message) {

        JsonObject body = message.body();
        String  query = QUERY_USER_BIRTHDAY+body.getInteger("id");
        this.dbClient.query(query , reply -> {
            if (reply.succeeded()) {
                List<JsonObject> rows = reply.result().getRows();
                JsonObject result = rows.get(0);
                String birthday="";
                String fechaActual="";
                try{
                    fechaActual=UtilsDate.format_d_MM(UtilsDate.getLocalDate());
                    birthday=UtilsDate.format_d_MM(UtilsDate.parse_yyyy_MM_dd(result.getString("birthday")));
                }
                catch (ParseException err){
                    message.reply(new JsonObject().put("result","error al parsear la fecha"));
                }

                if(birthday.equals(fechaActual))
                    message.reply(new JsonObject().put("result",true).put("fechaActual",fechaActual));
                else
                    message.reply(new JsonObject().put("result",false).put("fechaActual",fechaActual));

            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void tokenThirdParty(Message<JsonObject> message) {
        JsonObject body = message.body();
        String userType = body.getString("user_type");
        JsonArray params = new JsonArray()
                .add(body.getString("user"))
                .add(body.getString("pass"));
        this.dbClient.queryWithParams(QUERY_TOKEN_LOGING, params, reply -> {
            if (reply.succeeded()) {
                if (reply.result().getNumRows() == 0) {
                    message.reply(null);
                } else {
                    JsonObject result = reply.result().getRows().get(0);
                    if (userType.equals("M")){
                        message.reply(result);
                    } else {
                        reportQueryError(message, new Throwable("Incorrect user type"));
                    }
                }
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void searchUser(Message<JsonObject> message) {
        try {
            String QUERY = QUERY_SEARCH_ADVANCED;
            String searchTerm = message.body().getString("searchTerm");
            if (!searchTerm.isEmpty()) {
                QUERY = QUERY.concat(" AND CONCAT_WS(' ', u.name, u.email, u.phone) LIKE '%").concat(searchTerm).concat("%' ");
            }
            Integer terminalId = message.body().getInteger(_TERMINAL_ID);
            if(terminalId != null) {
                QUERY = QUERY.concat(" AND e.branchoffice_id = " + terminalId);
            }

            Integer limit = message.body().getInteger("limit");
            QUERY = QUERY.concat(" ORDER BY u.name, u.email ");
            if (limit > 0) QUERY += " LIMIT " + limit;
            dbClient.query(QUERY, reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> resultCustomers = reply.result().getRows();
                    if (resultCustomers.isEmpty()) {
                        message.reply(new JsonArray());
                    } else {
                        message.reply(new JsonArray(resultCustomers));
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void userExists(Message<JsonObject> message) {
        try {
            String email = message.body().getString("email");

            dbClient.queryWithParams(QUERY_USER_EXISTS, new JsonArray().add(email), reply -> {
                try {
                    if (reply.failed()) {
                        throw reply.cause();
                    }
                    List<JsonObject> resultUsers = reply.result().getRows();
                    if (!resultUsers.isEmpty()) {
                        message.reply(true);
                    } else {
                        message.reply(false);
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    reportQueryError(message, ex);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void deleteUser(Message<JsonObject> message) {
        JsonArray params = new JsonArray()
                .add(message.body().getInteger("user_id"));
        this.dbClient.updateWithParams(QUERY_DELETE_USER, params, reply -> {
            if (reply.succeeded()) {
                message.reply("deleted");
            } else {
                reportQueryError(message, reply.cause());
            }
        });
    }

    private void getUsersByJobName(Message<JsonObject> message){
        try{
            String jobName = message.body().getString("job_name");
            this.dbClient.queryWithParams(QUERY_GET_USERS_BY_JOB_NAME, new JsonArray().add(jobName), reply->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("Users not found");
                    }
                    message.reply(new JsonArray(result));
                } catch (Exception e){
                    reportQueryError(message,e);
                }
            });
        }catch (Exception ex){
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    private void getProfileByUserId(Message<JsonObject> message){
        try{
            Integer userId = message.body().getInteger(USER_ID);
            this.dbClient.queryWithParams(QUERY_GET_PROFILE_BY_USER_ID, new JsonArray().add(userId), reply->{
                try{
                    if(reply.failed()){
                        throw new Exception(reply.cause());
                    }
                    List<JsonObject> result = reply.result().getRows();
                    if(result.isEmpty()){
                        throw new Exception("User not found");
                    }
                    message.reply(result.get(0));
                } catch (Exception e){
                    reportQueryError(message,e);
                }
            });
        }catch (Exception ex){
            ex.printStackTrace();
            reportQueryError(message, ex);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="queries">
    private static final String QUERY_LOGIN = "SELECT \n" +
        " u.id,\n" +
        " u.name,\n" +
        " u.phone,\n" +
        " u.email,\n" +
        " u.profile_id,\n" +
        " u.user_type,\n" +
        " e.id AS employee_id,\n" +
        " e.branchoffice_id,\n" +
        " b.branch_office_type,\n" +
        " c.id AS customer_id,\n" +
        " u.social_auth,\n" +
        " j.name as job_name,\n" +
        " p.name as profile_name\n" +
        " FROM users u\n" +
        " LEFT JOIN employee e ON e.user_id = u.id AND e.status != 3\n" +
        " LEFT JOIN profile p ON p.id = u.profile_id\n" +
        " LEFT JOIN customer c ON c.user_id = u.id AND c.status != 3\n" +
        " LEFT JOIN branchoffice b ON b.id = e.branchoffice_id AND b.status != 3\n" +
        " LEFT JOIN job j ON j.id = e.job_id\n" +
        " WHERE\n" +
        " u.email = ?\n" +
        " AND u.pass = ?\n" +
        " AND u.status = 1;";

    private static final String QUERY_FIND_BY_MAIL = "SELECT\n"
            + "	id, name, email, social_auth \n"
            + "FROM\n"
            + "	users\n"
            + "WHERE\n"
            + "	email = ?";
    private static final String QUERY_FIND_BY_EMPLOYEE_ID = "SELECT \n"+
            " * FROM users WHERE id = ? and pass = ?\n";

    private static final String QUERY_UPDATE_PASSWORD = "UPDATE\n"
            + "	users\n"
            + "SET\n"
            + "	pass = ?\n"
            + "WHERE\n"
            + "	email = ?";
    private static final String QUERY_RESET_PASSWORD = "UPDATE users AS u\n" +
            " SET u.pass = ?\n" +
            " WHERE u.id = ?;"
            ;
    private static final String QUERY_USER_PROFILES = "SELECT "
            + "u.profile_id, "
            + "p.name, "
            + "p.description "
            + "FROM users u\n"
            + "JOIN profile p ON p.id = u.profile_id\n"
            + "WHERE u.id = ?";
    private static final String QUERY_USER_BIRTHDAY = "SELECT u.id ,c.birthday FROM users as u INNER JOIN customer AS c ON u.id=c.user_id where c.id =";
    private static final String QUERY_TOKEN_LOGING = "SELECT \n" +
            " u.id,\n" +
            " u.name,\n" +
            " u.phone,\n" +
            " u.email,\n" +
            " u.profile_id,\n" +
            " u.user_type,\n" +
            //" e.id AS employee_id,\n" +
            //" e.branchoffice_id,\n" +
            //" b.branch_office_type\n" +
            " u.third_party\n" +
            " FROM users u\n" +
            //" LEFT JOIN employee e ON e.user_id = u.id AND e.status != 3\n" +
            //" LEFT JOIN branchoffice b ON b.id = e.branchoffice_id AND b.status != 3\n" +
            " WHERE\n" +
            " u.email = ?\n" +
            " AND u.pass = ?\n" +
            " AND u.status = 1;";
    private static final String QUERY_SEARCH_ADVANCED = "SELECT u.id, u.name, u.email, u.phone FROM users u\n" +
            "INNER JOIN employee e ON e.user_id = u.id \n" +
            "WHERE u.status = 1 \n";

    private static final String QUERY_USER_EXISTS = "SELECT * FROM users \n" +
            "WHERE email = ?;";

    private static final String QUERY_GET_USERS_BY_JOB_NAME = "select \n" +
            "u.id,\n" +
            "u.name ,\n" +
            "u.phone,\n" +
            "u.email,\n" +
            "u.status\n" +
            "from users u \n" +
            "WHERE u.status != 3 AND u.id IN (select e.user_id from employee e where e.job_id in (select j.id from job j where j.name = ?))";

    private static final String QUERY_DELETE_USER = "UPDATE\n"
            + "	users\n"
            + "SET\n"
            + "	status = 3\n"
            + "WHERE\n"
            + "	id = ?";

    private static final String QUERY_GET_PROFILE_BY_USER_ID = "\n" +
            "SELECT\n" +
            "   p.*,\n" +
            "   IF(p.id = (SELECT value FROM general_setting WHERE FIELD = 'external_document_profile_id'), TRUE, FALSE) is_external_document\n" +
            "FROM users AS u\n" +
            "LEFT JOIN profile p ON p.id = u.profile_id\n" +
            "WHERE u.id = ?\n" +
            "LIMIT 1;";
//</editor-fold>
}
