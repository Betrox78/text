package database.users.handlers;

import database.commons.DBHandler;
import database.commons.GenericQuery;
import database.users.UsersDBV;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import utils.UtilsJWT;

import java.util.List;
import java.util.Objects;

import static service.commons.Constants.ID;

public class PartnerLogin extends DBHandler<UsersDBV> {

    private final static String API_KEY = "api_key";
    private final static String KEY_SECRET = "key_secret";
    private final static String INTEGRATION_PARTNER_ID = "integration_partner_id";
    private final static String TOKEN = "token";
    private final static String TOKEN_ID = "token_id";
    private final static String CREATED_BY = "created_by";
    private final static String INTEGRATION_PARTNER_SESSION = "integration_partner_session";

    private final static String INTEGRATION_PARTNER_API_KEY_ID = "integration_partner_api_key_id";

    private final static String ACCESS_TOKEN = "accessToken";
    private final static String REFRESH_TOKEN = "refreshToken";
    public PartnerLogin(UsersDBV dbVerticle) {
        super(dbVerticle);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        this.startTransaction(message, conn -> {
            try {
                JsonObject body = message.body();
                String apiKey = body.getString(API_KEY);
                String keySecret = body.getString(KEY_SECRET);
                String token = body.getString(TOKEN);

                this.searchPartner(conn, apiKey, keySecret).setHandler(partnerReply -> {
                    try {

                        if (partnerReply.failed()) {
                            rollbackTransaction(message, conn, partnerReply.cause());
                            return;
                        }

                        JsonObject partnerResult = partnerReply.result();
                        if (Objects.isNull(partnerResult)) {
                            commitTransaction(message, conn, null);
                            return;
                        }

                        Integer integrationPartnerId = partnerResult.getInteger(INTEGRATION_PARTNER_ID);
                        Integer integrationPartnerApiKeyId = partnerResult.getInteger(ID);
                        saveSession(conn, integrationPartnerId, integrationPartnerApiKeyId, token).setHandler(sessionReply -> {
                            try {
                                if (sessionReply.failed()) {
                                    rollbackTransaction(message, conn, sessionReply.cause());
                                    return;
                                }

                                Integer sessionResult = sessionReply.result();
                                partnerResult.put(TOKEN_ID, sessionResult);
                                commitTransaction(message, conn, partnerResult);

                            } catch (Throwable throwable) {
                                rollbackTransaction(message, conn, throwable);
                            }

                        });

                    } catch (Throwable throwable) {
                        rollbackTransaction(message, conn, throwable);
                    }
                });

            } catch (Throwable throwable) {
                rollbackTransaction(message, conn, throwable);
            }
        });
    }

    private Future<Integer> saveSession(SQLConnection dbClient, Integer integrationPartnerId, Integer integrationPartnerApiKeyId, String token) {
        Future<Integer> future = Future.future();
        try {
            JsonObject params = new JsonObject()
                    .put(INTEGRATION_PARTNER_ID, integrationPartnerId)
                    .put(INTEGRATION_PARTNER_API_KEY_ID, integrationPartnerApiKeyId)
                    .put(TOKEN, token)
                    .put(CREATED_BY, 1);
            GenericQuery insert = generateGenericCreate(INTEGRATION_PARTNER_SESSION, params);
            dbClient.updateWithParams(insert.getQuery(), insert.getParams(), reply -> {
                try {
                    if (reply.failed()) {
                        future.fail(reply.cause());
                        return;
                    }

                    Integer id = reply.result().getKeys().getInteger(0);

                    future.complete(id);

                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    future.fail(throwable);
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            future.fail(throwable);
        }

        return  future;
    }

    private Future<JsonObject> searchPartner(SQLConnection dbClient, String apiKey, String keySecret) {
        Future<JsonObject> future = Future.future();
        try {
            JsonArray params = new JsonArray()
                    .add(apiKey)
                    .add(keySecret);
            dbClient.queryWithParams(QUERY_LOGIN, params, reply -> {
                try {
                    if (reply.failed()) {
                        future.fail(reply.cause());
                        return;
                    }

                    List<JsonObject> result = reply.result().getRows();
                    if (result.size() == 0) {
                        future.complete(null);
                        return;
                    }

                    JsonObject data = result.get(0);
                    future.complete(data);

                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    future.fail(throwable);
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            future.fail(throwable);
        }

        return  future;
    }

    private static final String QUERY_LOGIN = "SELECT\n" +
            " IPAK.id,\n" +
            " IP.name,\n" +
            " IPAK.api_key,\n" +
            " IPAK.key_secret,\n" +
            " IPAK.integration_partner_id,\n" +
            " IP.customer_id,\n" +
            " IP.customer_addresses_id,\n" +
            " IP.customer_billing_information_id,\n" +
            " IP.user_id,\n" +
            " EMP.id as employee_id,\n" +
            " IP.branchoffice_id,\n" +
            " BO.branch_office_type,\n" +
            " USR.email,\n" +
            " USR.phone,\n" +
            " USR.profile_id\n" +
            " FROM integration_partner_api_key as IPAK\n" +
            " INNER JOIN integration_partner as IP ON IP.id = IPAK.integration_partner_id AND IP.status != 3\n" +
            " LEFT JOIN users as USR ON IP.user_id = USR.id AND USR.status != 3\n" +
            " LEFT JOIN employee as EMP ON EMP.user_id = USR.id AND EMP.status != 3\n" +
            " LEFT JOIN branchoffice as BO ON BO.id = IP.branchoffice_id AND BO.status != 3\n" +
            " WHERE\n" +
            " IPAK.api_key = ?\n" +
            " AND IPAK.key_secret = ?\n" +
            " AND IPAK.status = 1;";
}
