package database.alliances.handlers.AllianceDBV;

import database.alliances.AllianceDBV;
import database.commons.DBHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;

import java.util.List;

import static database.alliances.AllianceDBV.*;

public class FindByIdWithDetails extends DBHandler<AllianceDBV> {

  public FindByIdWithDetails(AllianceDBV dbVerticle) {
    super(dbVerticle);
  }

  @Override
  public void handle(Message<JsonObject> message) {
    try {
      Integer allianceID = message.body().getInteger(ID);
      JsonArray params = new JsonArray().add(allianceID);

      Future<ResultSet> fAlliance = Future.future();
      Future<ResultSet> fCities = Future.future();
      Future<ResultSet> fServices = Future.future();

      this.dbClient.queryWithParams(QUERY_FIND_ALLIANCE, params, fAlliance.completer());
      this.dbClient.queryWithParams(QUERY_ALLIANCE_CITIES, params, fCities.completer());
      this.dbClient.queryWithParams(QUERY_ALLIANCE_SERVICES, params, fServices.completer());

      CompositeFuture.all(fAlliance, fCities, fServices).setHandler((AsyncResult<CompositeFuture> reply) -> {
        try {
          if (reply.failed()) {
            throw reply.cause();
          }

          List<JsonObject> alliances = reply.result().<ResultSet>resultAt(0).getRows();
          if (alliances.isEmpty()) {
            throw new Throwable("Alliance: Not found");
          }

          JsonObject alliance = alliances.get(0);
          List<JsonObject> cities = reply.result().<ResultSet>resultAt(1).getRows();
          List<JsonObject> services = reply.result().<ResultSet>resultAt(2).getRows();
          alliance.put(CITIES, cities);
          alliance.put(SERVICES, services);

          replyResult(message, alliance);

        } catch (Throwable e) {
          replyError(message, e);
        }

      });
    } catch (Exception ex) {
        replyError(message, ex);
    }
  }

  private static final String QUERY_FIND_ALLIANCE = "SELECT al.*, ac.name AS category_name \n" +
      "FROM alliance AS al \n" +
      "LEFT JOIN alliance_category AS ac ON ac.id=al.alliance_category_id \n" +
      "WHERE al.id = ? AND al.status != 3 AND al.status != 2;";
  private static final String QUERY_ALLIANCE_CITIES = "SELECT alliance_city.id AS id, " +
      "city.id AS city_id, " +
      "city.name AS city_name " +
      "FROM alliance_city " +
      "INNER JOIN city ON city.id=alliance_city.city_id " +
      "WHERE alliance_city.alliance_id = ? AND alliance_city.status != 3 AND alliance_city.status != 2;";
  private static final String QUERY_ALLIANCE_SERVICES = "SELECT id, name, description, unit_price " +
      "FROM alliance_service " +
      "WHERE alliance_id = ? AND status != 3 AND status != 2;";
}
