/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.permission;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import database.commons.DBVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.json.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static service.commons.Constants.ACTION;

/**
 *
 * Kriblet
 */
public class PermissionDBV extends DBVerticle {

    public static final String ACTION_PROFILE_PERMISSIONS = "PermissionDBV.profilePermissions";
    public static final String ACTION_USER_PERMISSIONS = "PermissionDBV.userPermissions";
    public static final String ACTION_ASSIGN_PROFILE_PERMISSIONS = "PermissionDBV.assignProfilePermissions";
    public static final String ACTION_ASSIGN_USER_PERMISSIONS = "PermissionDBV.assignUserPermissions";
    public static final String ACTION_USER_PLUS_PROFILES_PERMISSIONS = "PermissionDBV.userPlusProfiles";
    public static final String ACTION_GET_MENU_USER = "PermissionDBV.getMenuUser";
    public static final String ACTION_SET_PERMISSION_SERVICES = "PermissionDBV.setPermissionServices";
    public static final String ACTION_PROFILE_DETAIL = "PermissionDBV.getProfileDetail";

    public static final String SERVICES = "services";
    public static final String PERMISSION_ID = "permission_id";
    public static final String AUTH_SERVICE_ID = "auth_service_id";
    public static final String AUTH_SUB_SERVICE_ID = "auth_sub_service_id";
    public static final String HTTP_METHOD = "http_method";

    @Override
    public String getTableName() {
        return "permission";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(ACTION);
        switch (action) {
            case ACTION_PROFILE_PERMISSIONS:
                this.profilePermissions(message);
                break;
            case ACTION_USER_PERMISSIONS:
                this.userPermissions(message);
                break;
            case ACTION_ASSIGN_PROFILE_PERMISSIONS:
                this.assignProfilePermission(message);
                break;
            case ACTION_ASSIGN_USER_PERMISSIONS:
                this.assignUserPermission(message);
                break;
            case ACTION_USER_PLUS_PROFILES_PERMISSIONS:
                this.userPlusProfilesPermissions(message);
                break;
            case ACTION_GET_MENU_USER:
                this.getMenuUser(message);
                break;
            case ACTION_SET_PERMISSION_SERVICES:
                this.setPermissionServices(message);
                break;
            case ACTION_PROFILE_DETAIL:
                this.getProfileDetail(message);
                break;
        }
    }

    private void profilePermissions(Message<JsonObject> message) {
        try {
            int id = message.body().getInteger("id");
            this.dbClient.queryWithParams(QUERY_PROFILE_PERMISSIONS, new JsonArray().add(id), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void userPermissions(Message<JsonObject> message) {
        try {
            int id = message.body().getInteger("id");
            this.dbClient.queryWithParams(QUERY_USER_PERMISSIONS, new JsonArray().add(id), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void userPlusProfilesPermissions(Message<JsonObject> message) {
        try {
            int id = message.body().getInteger("id");
            this.dbClient.queryWithParams(QUERY_USER_PLUS_PROFILES_PERMISSIONS, new JsonArray().add(id), reply -> {
                try {
                    if (reply.failed()){
                        throw reply.cause();
                    }
                    message.reply(new JsonArray(reply.result().getRows()));
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private void assignProfilePermission(Message<JsonObject> message) {
        this.startTransaction(message, con -> {
            try {
                JsonObject body = message.body();
                int profileId = body.getInteger("profile_id");
                JsonArray permissions = body.getJsonArray("permissions");
                List<String> values = new ArrayList<>();
                for (int i = 0; i < permissions.size(); i++) {
                    JsonObject permission = permissions.getJsonObject(i);
                    values.add("(" + profileId + ", " + permission.getInteger("permission_id") + ")");
                }
                List<String> batch = new ArrayList<>();
                batch.add(QUERY_DELETE_PROFILE_PERMISSIONS + profileId);
                batch.add(QUERY_INSERT_PROFILE_PERMISSIONS + String.join(",", values));

                con.batch(batch, reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        this.commit(con, message, null);
                    } catch (Throwable t){
                        this.rollback(con, t, message);
                    }
                });
            } catch (Throwable t){
                this.rollback(con, t, message);
            }
        });
    }

    private void assignUserPermission(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body();
                int userId = body.getInteger("user_id");
                JsonArray permissions = body.getJsonArray("permissions");
                List<String> values = new ArrayList<>();
                for (int i = 0; i < permissions.size(); i++) {
                    JsonObject permission = permissions.getJsonObject(i);
                    values.add(
                            "(" + userId + ", "
                                    + permission.getInteger("permission_id") + ","
                                    + permission.getInteger("branchoffice_id") + ")"
                    );
                }
                List<String> batch = new ArrayList<>();
                batch.add(QUERY_DELETE_USER_PERMISSIONS + userId);
                batch.add(QUERY_INSERT_USER_PERMISSIONS + String.join(",", values));

                conn.batch(batch, reply -> {
                    try {
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        this.commit(conn, message, null);
                    } catch (Throwable t){
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t){
                this.rollback(conn, t, message);
            }
        });
    }

    private void getProfileDetail(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            Integer profileId = body.getInteger("profile_id");
            Boolean allModules = body.getBoolean("all_modules");
            Integer assignedTo = body.containsKey("assigned_to") ? body.getInteger("assigned_to") : null;
            Integer userId = null;
            if(body.getInteger("user_id") != null){
                userId = body.getInteger("user_id");
            }
            this.getModulesByProfileId(profileId, userId, allModules, assignedTo).whenComplete((resultModules, errorModules) -> {
                try {
                    if (errorModules != null){
                        reportQueryError(message, errorModules);
                    } else {
                        message.reply(resultModules);
                    }
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject> getModulesByProfileId(Integer profileId, Integer userId, Boolean allModules, Integer assignedTo){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            String QUERY;
            JsonArray params = new JsonArray();
            Boolean flagProfileId = profileId != null;
            Boolean flagUserId = userId != null;
            if (flagUserId){
                QUERY = QUERY_MENU_GET_MODULES_AND_USER_ID;
                params.add(profileId).add(userId);
            } else if (flagProfileId && !allModules){
                QUERY = QUERY_MENU_GET_MODULES.concat(BY_PROFILE_ID_PARAM);
                params.add(profileId);
            } else {
                QUERY = QUERY_MENU_GET_MODULES;
            }
            dbClient.queryWithParams(QUERY, params, replyModules -> {
                try{
                    if (replyModules.failed()){
                        throw replyModules.cause();
                    }
                    List<JsonObject> resultModules = replyModules.result().getRows();
                    if(resultModules.isEmpty()){
                        future.complete(new JsonObject());
                    } else {
                        JsonObject profileDetail = new JsonObject();
                        profileDetail.put("modules", resultModules);
                        this.execGetSubModulesByModuleId(new JsonArray(resultModules), profileId, userId, assignedTo).whenComplete((resultSubModules, errorSubModules) -> {
                            try {
                                if(errorSubModules != null){
                                    throw errorSubModules;
                                }
                                future.complete(profileDetail);
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
                    }
                } catch(Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonArray> execGetSubModulesByModuleId(JsonArray modules, Integer profileId, Integer userId, Integer assignedTo){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {
            CompletableFuture.allOf(modules.stream()
                    .map(module -> this.getSubModulesByModuleId((JsonObject) module, profileId, userId, assignedTo))
                    .toArray(CompletableFuture[]::new))
                    .whenComplete((result, error) -> {
                        try {
                            if (error != null) {
                                throw error;
                            }
                            future.complete(modules);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonArray> getSubModulesByModuleId(JsonObject module, Integer profileId, Integer userId, Integer assignedTo){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {
            String QUERY;
            JsonArray params = new JsonArray().add(module.getInteger("id"));
            boolean flagUserId = userId != null;
            if (flagUserId){
                QUERY = QUERY_MENU_GET_SUB_MODULES_BY_MODULE_ID_AND_USER_ID;
                params.add(profileId).add(userId);
            } else {
                QUERY = QUERY_MENU_GET_SUB_MODULES_BY_MODULE_ID;
                if(assignedTo != null){
                    QUERY = QUERY.concat(" and pp.profile_id = ? ");
                    params.add(profileId);
                }
            }
            dbClient.queryWithParams(QUERY, params, replySubModules -> {
                try{
                    if (replySubModules.failed()){
                        throw replySubModules.cause();
                    }
                    List<JsonObject> resultSubModules = replySubModules.result().getRows();
                    if(resultSubModules.isEmpty()){
                        future.complete(new JsonArray());
                    } else {
                        module.put("sub_modules", resultSubModules);
                        this.execGetPermissionsBySubmodule(new JsonArray(resultSubModules), userId, profileId, assignedTo).whenComplete((resultPermissions, errorPermissions) -> {
                            try {
                                if(errorPermissions != null){
                                    throw errorPermissions;
                                }
                                future.complete(resultPermissions);
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
                    }
                } catch(Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    private void getMenuUser(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            int profileId = body.getInteger("profile_id");
            int userId = body.getInteger("user_id");
            String moduleName = body.getString("module");
            this.getSubModules(moduleName, profileId, userId).whenComplete((resultSubModules, errorSubModules) -> {
                try {
                    if (errorSubModules != null){
                        throw errorSubModules;
                    }
                    message.reply(resultSubModules);
                } catch (Throwable t){
                    reportQueryError(message, t);
                }
            });
        } catch (Throwable t){
            reportQueryError(message, t);
        }
    }

    private CompletableFuture<JsonObject> getSubModules(String moduleName, Integer profileId, Integer userId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            dbClient.queryWithParams(QUERY_MENU_GET_SUB_MODULES, new JsonArray().add(profileId).add(userId).add(moduleName), replySubModules -> {
                try{
                    if (replySubModules.failed()){
                        throw replySubModules.cause();
                    }
                    List<JsonObject> resultSubModules = replySubModules.result().getRows();
                    if(resultSubModules.isEmpty()){
                        future.complete(new JsonObject());
                    } else {
                        JsonObject userPermissions = new JsonObject();
                        userPermissions.put("sub_modules", resultSubModules);
                        this.execGetPermissionsBySubmodule(new JsonArray(resultSubModules), userId, null, null).whenComplete((resultPermissions, errorPermissions) -> {
                            try {
                                if(errorPermissions != null){
                                    throw errorPermissions;
                                }
                                future.complete(userPermissions);
                            } catch (Throwable t){
                                future.completeExceptionally(t);
                            }
                        });
                    }
                } catch(Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonArray> execGetPermissionsBySubmodule(JsonArray submodules, Integer userId, Integer profileId, Integer assignedTo){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {
            CompletableFuture.allOf(submodules.stream()
                    .map(submodule -> this.getPermissionsBySubmodule((JsonObject) submodule, userId, profileId, assignedTo))
                    .toArray(CompletableFuture[]::new))
                    .whenComplete((result, error) -> {
                        try {
                            if (error != null) {
                                throw error;
                            }
                            future.complete(submodules);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonArray> getPermissionsBySubmodule(JsonObject subModule, Integer userId, Integer profileId, Integer assignedTo){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {
            String QUERY;
            JsonArray params = new JsonArray();
            boolean flagUserId = userId != null;
            if(flagUserId){
                QUERY = QUERY_MENU_GET_PERMISSIONS_BY_SUB_MODULE_ID_AND_USER_ID;
                if(profileId != null){
                    QUERY = QUERY.replace("{{PROF_ID}}", ",IF((SELECT COUNT(*) FROM profile_permission WHERE permission_id = p.id AND profile_id = ?) > 0, true, false) AS assigned \n");
                    params.add(profileId);
                }else{
                    QUERY = QUERY.replace("{{PROF_ID}}","");
                }
                params.add(subModule.getInteger("id")).add(userId);
            } else {
                QUERY = QUERY_MENU_GET_PERMISSIONS_BY_SUB_MODULE_ID;
                if(assignedTo != null){
                    QUERY = QUERY
                        .replace("{{ASSIGNED}}", "IF((SELECT COUNT(*) FROM user_permission_branchoffice WHERE permission_id = p.id AND user_id = ?) > 0, true, false)")
                        .replace("{{INNER}}", " inner join profile_permission pp on pp.permission_id = p.id ")
                        .replace("{{WHERE}}", " and pp.profile_id = ? ");
                    params.add(assignedTo).add(subModule.getInteger("id")).add(profileId);
                }else{
                    QUERY = QUERY.replace("{{ASSIGNED}}", "IF((SELECT COUNT(*) FROM profile_permission WHERE permission_id = p.id AND profile_id = ?) > 0, true, false)")
                        .replace("{{INNER}}", "")
                        .replace("{{WHERE}}", "");
                    params.add(profileId).add(subModule.getInteger("id"));
                }
            }
            dbClient.queryWithParams(QUERY, params, replyPermissions -> {
                try{
                    if (replyPermissions.failed()){
                        throw  replyPermissions.cause();
                    }
                    List<JsonObject> resultPermissions = replyPermissions.result().getRows();
                    if(resultPermissions.isEmpty()){
                        future.complete(new JsonArray());
                    } else {
                        subModule.put("permission", resultPermissions);
                        if (flagUserId){
                            this.execGetBranchofficesByPermission(new JsonArray(resultPermissions), userId).whenComplete((resultBranchoffices, errorBranchoffices) -> {
                                try {
                                    if (errorBranchoffices != null){
                                        throw errorBranchoffices;
                                    }
                                    future.complete(resultBranchoffices);
                                } catch (Throwable t){
                                    future.completeExceptionally(t);
                                }
                            });
                        } else {
                            future.complete(new JsonArray(resultPermissions));
                        }
                    }
                } catch(Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonArray> execGetBranchofficesByPermission(JsonArray permissions, Integer userId){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        try {
            CompletableFuture.allOf(permissions.stream()
                    .map(permission -> this.getBranchofficesByPermission((JsonObject) permission, userId))
                    .toArray(CompletableFuture[]::new))
                    .whenComplete((result, error) -> {
                        try {
                            if (error != null) {
                                throw error;
                            }
                            future.complete(permissions);
                        } catch (Throwable t){
                            future.completeExceptionally(t);
                        }
                    });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private CompletableFuture<JsonObject> getBranchofficesByPermission(JsonObject permission, Integer userId){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        try {
            dbClient.queryWithParams(QUERY_MENU_GET_BRANCHOFFICES_BY_PERMISSION_ID, new JsonArray().add(permission.getInteger("id")).add(userId), replyBranchoffices -> {
                try{
                    if (replyBranchoffices.failed()){
                        throw replyBranchoffices.cause();
                    }
                    JsonArray listBranches = new JsonArray();
                    List<JsonObject> resultBranchoffices = replyBranchoffices.result().getRows();
                    if(resultBranchoffices.isEmpty()){
                        permission.put("branchoffices", listBranches);
                    } else {
                        resultBranchoffices.forEach(branch -> {
                            listBranches.add(branch.getInteger("branchoffice_id"));
                        });
                        permission.put("branchoffices", listBranches);
                    }
                    future.complete(permission);
                } catch(Throwable t){
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t){
            future.completeExceptionally(t);
        }
        return future;
    }

    private void setPermissionServices(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body();
                int permissionId = body.getInteger(PERMISSION_ID);
                JsonArray services = body.getJsonArray(SERVICES);
                List<String> values = new ArrayList<>();
                for (int i = 0; i < services.size(); i++) {
                    JsonObject service = services.getJsonObject(i);
                    values.add(
                            "(" + permissionId + ", "
                                    + service.getInteger(AUTH_SERVICE_ID) + ","
                                    + service.getInteger(AUTH_SUB_SERVICE_ID) + ","
                                    + "'" +service.getString(HTTP_METHOD) + "')"
                    );
                }
                List<String> batch = new ArrayList<>();
                batch.add(QUERY_DELETE_PERMISSIONS_SERVICES + permissionId);
                batch.add(QUERY_INSERT_PERMISSIONS_SERVICES + String.join(",", values));

                conn.batch(batch, reply -> {
                    try{
                        if (reply.failed()){
                            throw reply.cause();
                        }
                        this.commit(conn, message, null);
                    } catch (Throwable t){
                        this.rollback(conn, t, message);
                    }
                });
            } catch (Throwable t){
                this.rollback(conn, t, message);
            }
        });
    }

    //<editor-fold defaultstate="collapsed" desc="queries">
    private static final String QUERY_MENU_GET_MODULES_AND_USER_ID = "SELECT DISTINCT\n" +
            " m.id,\n" +
            " m.name,\n" +
            " m.description\n" +
            " FROM module AS m\n" +
            " LEFT JOIN sub_module AS sm ON sm.module_id = m.id\n" +
            " LEFT JOIN permission AS p ON p.sub_module_id = sm.id\n" +
            " LEFT JOIN user_permission_branchoffice AS upb ON upb.permission_id = p.id\n" +
            " LEFT JOIN profile_permission AS pp ON pp.permission_id = upb.permission_id\n" +
            " WHERE pp.profile_id = ? AND upb.user_id = ? AND m.status = 1;";

    private static final String QUERY_MENU_GET_MODULES = "SELECT DISTINCT\n" +
            " m.id,\n" +
            " m.name,\n" +
            " m.description\n" +
            " FROM module AS m\n" +
            " LEFT JOIN sub_module AS sm ON sm.module_id = m.id\n" +
            " LEFT JOIN permission AS p ON p.sub_module_id = sm.id\n" +
            " LEFT JOIN profile_permission AS pp ON pp.permission_id = p.id\n" +
            " WHERE m.status = 1 ";

    private static final String BY_PROFILE_ID_PARAM = " AND pp.profile_id = ? ";

    private static final String QUERY_MENU_GET_SUB_MODULES_BY_MODULE_ID_AND_USER_ID = "SELECT DISTINCT \n" +
            " sm.id, \n" +
            " sm.name, \n" +
            " sm.group_type, \n" +
            " sm.menu_type \n" +
            " FROM sub_module sm\n" +
            " LEFT JOIN module AS m ON m.id = sm.module_id\n" +
            " LEFT JOIN permission AS p ON p.sub_module_id = sm.id\n" +
            " LEFT JOIN user_permission_branchoffice AS upb ON upb.permission_id = p.id\n" +
            " LEFT JOIN profile_permission AS pp ON pp.permission_id = upb.permission_id\n" +
            " WHERE m.id = ? AND pp.profile_id = ? AND upb.user_id = ? AND sm.status = 1 ";

    private static final String QUERY_MENU_GET_SUB_MODULES_BY_MODULE_ID = "SELECT DISTINCT \n" +
            " sm.id, \n" +
            " sm.name, \n" +
            " sm.group_type, \n" +
            " sm.menu_type \n" +
            " FROM sub_module sm\n" +
            " LEFT JOIN module AS m ON m.id = sm.module_id\n" +
            " LEFT JOIN permission AS p ON p.sub_module_id = sm.id\n" +
            " LEFT JOIN profile_permission AS pp ON pp.permission_id = p.id\n" +
            " WHERE m.id = ? AND sm.status = 1 ";

    private static final String QUERY_MENU_GET_SUB_MODULES = "SELECT DISTINCT\n" +
            " sm.id,\n" +
            " sm.name,\n" +
            " sm.group_type,\n" +
            " sm.menu_type\n" +
            " FROM sub_module AS sm\n" +
            " LEFT JOIN module AS m ON m.id = sm.module_id\n" +
            " LEFT JOIN permission AS p ON p.sub_module_id = sm.id\n" +
            " LEFT JOIN user_permission_branchoffice AS upb ON upb.permission_id = p.id\n" +
            " LEFT JOIN profile_permission AS pp ON pp.permission_id = upb.permission_id\n" +
            " WHERE pp.profile_id = ? AND upb.user_id = ? AND m.name = ? AND m.status = 1;";

    private static final String QUERY_MENU_GET_PERMISSIONS_BY_SUB_MODULE_ID_AND_USER_ID = "SELECT DISTINCT\n" +
            " p.id,\n" +
            " p.name,\n" +
            " p.description,\n" +
            " p.dependency_id {{PROF_ID}}\n" +
            " FROM user_permission_branchoffice AS upb\n" +
            " LEFT JOIN permission AS p ON p.id = upb.permission_id\n" +
            " WHERE p.sub_module_id = ? AND upb.user_id = ? AND p.status = 1;";

    private static final String QUERY_MENU_GET_PERMISSIONS_BY_SUB_MODULE_ID = "SELECT DISTINCT\n" +
            " p.id,\n" +
            " p.name,\n" +
            " p.description,\n" +
            " p.dependency_id,\n" +
            " {{ASSIGNED}} AS assigned\n" +
            " FROM permission p\n" +
            " {{INNER}} " +
            " WHERE p.sub_module_id = ? AND p.status = 1 {{WHERE}};";

    private static final String QUERY_MENU_GET_BRANCHOFFICES_BY_PERMISSION_ID = "SELECT DISTINCT\n" +
            " upb.branchoffice_id\n" +
            " FROM user_permission_branchoffice upb\n" +
            " LEFT JOIN branchoffice b ON b.id = upb.branchoffice_id\n" +
            " WHERE upb.permission_id = ? and upb.user_id = ? AND b.status = 1;";

    private static final String QUERY_PROFILE_PERMISSIONS = "SELECT\n"
            + "	permission_id\n"
            //+ "	branchoffice_id\n"
            + "FROM\n"
            + "	profile_permission\n"
            + "WHERE\n"
            + "	profile_id = ?;";

    private static final String QUERY_USER_PERMISSIONS = "SELECT\n"
            + "	permission_id,\n"
            + "	branchoffice_id\n"
            + "FROM\n"
            + "	user_permission_branchoffice\n"
            + "WHERE\n"
            + "	user_id = ?;";

    private static final String QUERY_DELETE_PROFILE_PERMISSIONS = "DELETE\n"
            + "FROM\n"
            + "	profile_permission\n"
            + "WHERE\n"
            + "	profile_id = ";

    private static final String QUERY_INSERT_PROFILE_PERMISSIONS = "INSERT INTO\n"
            + "	profile_permission ( \n"
            + "	profile_id,\n"
            + "	permission_id\n"
            + ") VALUES ";

    private static final String QUERY_INSERT_USER_PERMISSIONS = "INSERT INTO\n"
            + "	user_permission_branchoffice ( \n"
            + "		user_id,\n"
            + "		permission_id,\n"
            + "		branchoffice_id \n"
            + ")VALUES";

    private static final String QUERY_DELETE_USER_PERMISSIONS = "DELETE\n"
            + "FROM\n"
            + "	user_permission_branchoffice\n"
            + "WHERE\n"
            + "	user_id = ";

    private static final String QUERY_INSERT_PERMISSIONS_SERVICES = "INSERT INTO\n"
            + "	permission_services ( \n"
            + "		permission_id,\n"
            + "		auth_service_id,\n"
            + "		auth_sub_service_id,\n"
            + "		http_method \n"
            + ")VALUES";

    private static final String QUERY_DELETE_PERMISSIONS_SERVICES = "DELETE\n"
            + "FROM\n"
            + "	permission_services\n"
            + "WHERE\n"
            + "	permission_id = ";

    private static final String QUERY_USER_PLUS_PROFILES_PERMISSIONS = "SELECT \n" +
            " upb.permission_id,\n" +
            " upb.branchoffice_id,\n" +
            " pp.profile_id\n" +
            " FROM user_permission_branchoffice AS upb\n" +
            " LEFT JOIN profile_permission AS pp ON pp.permission_id = upb.permission_id\n" +
            " WHERE upb.user_id = ?;";

//</editor-fold>

}
