package database.alliances.handlers.AllianceDBV;

import database.alliances.AllianceDBV;
import database.commons.DBHandler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static database.alliances.AllianceDBV.STATE_ID;

public class CityListByIdForWeb extends DBHandler<AllianceDBV> {

  public CityListByIdForWeb(AllianceDBV dbVerticle) {
    super(dbVerticle);
  }

  @Override
  public void handle(Message<JsonObject> message) {
    try {
      JsonObject body = message.body();

      // Initialize query and params
      JsonArray params = new JsonArray();

      // Add where clauses
      String stateID = body.getString(STATE_ID);
      if (stateID == null) {
        throw new Exception("State ID is required");
      }
      params.add(stateID);

      String queryItems = "SELECT DISTINCT(city_id) AS id, city.name ".concat(QUERY_CITY_LIST_BY_ID_FOR_WEB);

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

  private static final String QUERY_CITY_LIST_BY_ID_FOR_WEB = // "SELECT DISTINCT(city_id) AS id, city.name, county.state_id, state.name\n" +
      "FROM alliance_city \n" +
          "INNER JOIN alliance ON alliance.id=alliance_city.alliance_id AND alliance.status = 1\n" +
          "INNER JOIN city ON city.id=alliance_city.city_id\n" +
          "INNER JOIN county ON city.county_id=county.id\n" +
          "INNER JOIN state ON state.id=county.state_id\n" +
          "WHERE state.id=? AND alliance_city.status = 1 \n" +
          "AND alliance.init_at <= CURRENT_TIMESTAMP \n" +
          "AND alliance.valid_at >= CURRENT_TIMESTAMP ";
}
