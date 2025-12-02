package database.alliances.handlers.AllianceDBV;

import database.alliances.AllianceDBV;
import database.commons.DBHandler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class StateListForWeb extends DBHandler<AllianceDBV> {

  public StateListForWeb(AllianceDBV dbVerticle) {
    super(dbVerticle);
  }

  @Override
  public void handle(Message<JsonObject> message) {
    try {
      // Initialize query and params
      JsonArray params = new JsonArray();

      String queryItems = "SELECT DISTINCT(state.id) AS id, state.name ".concat(QUERY_STATE_LIST_FOR_WEB);

      dbClient.queryWithParams(queryItems, params, reply -> {
        try {
          if (reply.failed()) {
            throw reply.cause();
          }

          replyResult(message, new JsonArray(reply.result().getRows()));

        } catch (Throwable ex) {
          replyError(message, ex);
        }
      });

    } catch (Exception e) {
      replyError(message, e);
    }
  }

  private static final String QUERY_STATE_LIST_FOR_WEB = // "SELECT DISTINCT(state.id) AS id, state.name\n" +
      "FROM alliance_city \n" +
          "INNER JOIN alliance ON alliance.id=alliance_city.alliance_id AND alliance.status = 1\n" +
          "INNER JOIN city ON city.id=alliance_city.city_id\n" +
          "INNER JOIN county ON city.county_id=county.id\n" +
          "INNER JOIN state ON state.id=county.state_id " +
          "WHERE alliance_city.status = 1\n " +
          "AND alliance.init_at <= CURRENT_TIMESTAMP \n" +
          "AND alliance.valid_at >= CURRENT_TIMESTAMP ";
}
