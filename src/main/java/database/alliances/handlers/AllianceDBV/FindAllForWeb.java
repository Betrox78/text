package database.alliances.handlers.AllianceDBV;

import database.alliances.AllianceDBV;
import database.commons.DBHandler;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

import static database.alliances.AllianceDBV.*;

public class FindAllForWeb extends DBHandler<AllianceDBV> {

  private static final Integer MAX_LIMIT = 30;

  public FindAllForWeb(AllianceDBV dbVerticle) {
    super(dbVerticle);
  }

  @Override
  public void handle(Message<JsonObject> message) {
    try {
      JsonObject body = message.body();

      // Initialize query and params
      String query = QUERY_FIND_ALL_FOR_WEB;
      JsonArray params = new JsonArray();

      // Add where clauses
      String categoryID = body.getString(ALLIANCE_CATEGORY_ID);
      String cityID = body.getString(CITY_ID);
      String stateID = body.getString(STATE_ID);
      if (cityID != null) {
        query = QUERY_FIND_ALL_FOR_WEB_BY_CITY;
        query = query.concat(" AND alliance_city.city_id=? ");
        params.add(cityID);
      } else if (stateID != null) {
        query = QUERY_FIND_ALL_FOR_WEB_BY_STATE;
        query = query.concat(" AND state.id=? ");
        params.add(stateID);
      }
      if (categoryID != null) {
        query = query.concat(" AND alliance.alliance_category_id=? ");
        params.add(categoryID);
      }
      // Copy query to count
      String queryCount = "SELECT COUNT(DISTINCT alliance.id) AS items ".concat(query);
      JsonArray paramsCount = params.copy();

      String queryItems = "SELECT DISTINCT alliance.* ".concat(query);

      // Add order by
      String orderBy = body.getString(ORDER_BY);
      if (orderBy != null) {
        queryItems = queryItems.concat(" ORDER BY ? ");
        params.add(this.orderBy(orderBy));
      }

      // Add the limit for pagination
      int page = Integer.parseInt(body.getString(PAGE, "1"));
      int limit = Integer.parseInt(body.getString(LIMIT, MAX_LIMIT.toString()));
      if (limit > MAX_LIMIT) {
        limit = MAX_LIMIT;
      }
      int skip = limit * (page-1);
      queryItems = queryItems.concat(" LIMIT ?,? ");
      params.add(skip).add(limit);

      String finalQueryItems = queryItems;
      dbClient.queryWithParams(queryCount, paramsCount, replyCount -> {
        try {
          if (replyCount.failed()) {
            throw replyCount.cause();
          }

          dbClient.queryWithParams(finalQueryItems, params, reply -> {
            try {
              if (reply.failed()) {
                throw reply.cause();
              }

              Integer count = replyCount.result().getRows().get(0).getInteger("items", 0);
              List<JsonObject> items = reply.result().getRows();

              CompositeFuture.all(items.stream().map(this::populateServices).collect(Collectors.toList()))
                  .setHandler(replyAll  -> {
                    try {
                      List<JsonObject> objects = replyAll.result().list();
                      if (replyAll.failed()) {
                        throw replyAll.cause();
                      }

                      JsonObject result = new JsonObject()
                          .put("count", count)
                          .put("items", objects.size())
                          .put("results", objects);

                      replyResult(message, result);

                    } catch (Throwable e) {
                      replyError(message, e);
                    }

                  });

            } catch (Throwable ex) {
              replyError(message, ex);
            }
          });

        } catch (Throwable ex) {
          replyError(message, ex);
        }
      });

    } catch (Exception e) {
      replyError(message, e);
    }
  }


  private Future<JsonObject> populateServices(JsonObject alliance) {
    Future<JsonObject> future = Future.future();

    try {
      Integer allianceID = alliance.getInteger(ID);
      JsonArray params = new JsonArray().add(allianceID);

      this.dbClient.queryWithParams(QUERY_ALLIANCE_SERVICES, params, reply -> {
        try {
          if (reply.failed()) {
            throw reply.cause();
          }

          alliance.put(SERVICES, reply.result().getRows());
          future.complete(alliance);

        } catch (Throwable e) {
          future.fail(e);
        }
      });

    } catch (Exception e) {
      future.fail(e);
    }

    return future;
  }

  private static final String QUERY_FIND_ALL_FOR_WEB = "FROM alliance \n" +
      "WHERE alliance.status = 1 \n" +
      "AND alliance.init_at <= CURRENT_TIMESTAMP \n" +
      "AND alliance.valid_at >= CURRENT_TIMESTAMP";

  private static final String QUERY_FIND_ALL_FOR_WEB_BY_CITY = "FROM alliance_city " +
      "LEFT JOIN alliance on alliance_city.alliance_id=alliance.id " +
      "WHERE alliance.status = 1 " +
      "AND alliance.init_at <= CURRENT_TIMESTAMP " +
      "AND alliance.valid_at >= CURRENT_TIMESTAMP";
  private static final String QUERY_FIND_ALL_FOR_WEB_BY_STATE = "FROM state " +
      "INNER JOIN county ON county.state_id=state.id " +
      "INNER JOIN city ON city.county_id=county.id " +
      "INNER JOIN alliance_city ON city.id=alliance_city.city_id " +
      "INNER JOIN alliance ON alliance_city.alliance_id=alliance.id " +
      "WHERE alliance.status = 1 " +
      "AND alliance.init_at <= CURRENT_TIMESTAMP " +
      "AND alliance.valid_at >= CURRENT_TIMESTAMP";

  private static final String QUERY_ALLIANCE_SERVICES = "SELECT id, name, description, unit_price " +
      "FROM alliance_service " +
      "WHERE alliance_id = ? AND status != 3 AND status != 2;";
}
