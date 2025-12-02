/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.profile;

import database.commons.DBVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import service.commons.Constants;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Kriblet
 */
public class ProfileDBV extends DBVerticle {

    public static final String ACTION_GET_LIST = "ProfileDBV.getList";

    @Override
    public String getTableName() {
        return "profile";
    }

    @Override
    protected void onMessage(Message<JsonObject> message) {
        super.onMessage(message);
        String action = message.headers().get(Constants.ACTION);
        switch (action) {
            case ACTION_GET_LIST:
                this.getList(message);
                break;
        }
    }

    private void getList(Message<JsonObject> message) {
        Future<ResultSet> f1 = Future.future();
        Future<ResultSet> f2 = Future.future();

        this.dbClient.query(QUERY_GET_LIST_PROFILES, f1.completer());
        this.dbClient.query(QUERY_GET_LIST_MODULES_BY_PROFILE_ID, f2.completer());

        CompositeFuture.all(f1, f2).setHandler(reply -> {
            try{
                if (reply.succeeded()) {
                    List<JsonObject> resultProfiles = reply.result().<ResultSet>resultAt(0).getRows();
                    List<JsonObject> resultModules = reply.result().<ResultSet>resultAt(1).getRows();
                    if(resultProfiles.isEmpty()){
                        message.reply(new JsonArray());
                    } else {
                        for(int i=0; i<resultProfiles.size(); i++){
                            JsonArray moduleFound = new JsonArray();
                            JsonObject profile = resultProfiles.get(i);
                            Integer profileId = profile.getInteger("id");
                            for(int j=0; j<resultModules.size(); j++){
                                JsonObject profileModule = resultModules.get(j);
                                Integer profileIdModule = profileModule.getInteger("profile_id");
                                if (profileId.equals(profileIdModule)){
                                    profileModule.remove("profile_id");
                                    moduleFound.add(profileModule);
                                }
                            }
                            profile.put("modules", moduleFound);
                        }
                        message.reply(new JsonArray(resultProfiles));
                    }
                } else {
                    reportQueryError(message, reply.cause());
                }
            } catch(Exception e){
                reportQueryError(message, e);
            }
        });
    }

    private static final String QUERY_GET_LIST_PROFILES = "SELECT \n" +
            " p.id, \n" +
            " p.name,\n" +
            " p.description,\n" +
            " (SELECT COUNT(id) AS users_total FROM users WHERE profile_id = p.id) as total_users,\n" +
            " p.status\n" +
            " FROM profile p;";

    private static final String QUERY_GET_LIST_MODULES_BY_PROFILE_ID = "SELECT DISTINCT\n" +
            " pp.profile_id,\n" +
            " m.id,\n" +
            " m.name\n" +
            " FROM module m\n" +
            " LEFT JOIN sub_module sm ON sm.module_id = m.id\n" +
            " LEFT JOIN permission p ON p.sub_module_id = sm.id\n" +
            " LEFT JOIN user_permission_branchoffice upb ON upb.permission_id = p.id\n" +
            " LEFT JOIN profile_permission pp ON pp.permission_id = upb.permission_id\n" +
            " WHERE pp.profile_id IS NOT NULL AND m.status = 1 AND sm.status = 1 AND p.status = 1;";

}
