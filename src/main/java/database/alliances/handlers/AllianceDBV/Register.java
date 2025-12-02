package database.alliances.handlers.AllianceDBV;

import database.alliances.AllianceDBV;
import database.commons.DBHandler;
import database.commons.GenericQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static database.alliances.AllianceDBV.*;
import static service.commons.Constants.CREATED_BY;

public class Register extends DBHandler<AllianceDBV> {

  public Register(AllianceDBV dbVerticle) {
    super(dbVerticle);
  }

  @Override
  public void handle(Message<JsonObject> message) {
    this.startTransaction(message, (SQLConnection conn) -> {
      try {
        JsonObject body = message.body();
        JsonArray cities = (JsonArray) body.remove(CITIES);
        JsonArray services = (JsonArray) body.remove(SERVICES);
        Integer createdBy = body.getInteger(CREATED_BY);

        // Insert alliance
        GenericQuery model = this.generateGenericCreate(body);
        conn.updateWithParams(model.getQuery(), model.getParams(), (AsyncResult<UpdateResult> reply) -> {
          try {
            if(reply.failed()) {
              throw reply.cause();
            }

            Integer allianceID = reply.result().getKeys().getInteger(0);

            // Insert alliance cities
            List<GenericQuery> insertsCities = cities.stream().map(cityId ->
                this.generateGenericCreate(ALLIANCE_CITY, new JsonObject()
                    .put(ALLIANCE_ID, allianceID)
                    .put(CITY_ID, cityId)
                    .put(CREATED_BY, createdBy)
                )).collect(Collectors.toList());


            String insertCitiesQuery = insertsCities.get(0).getQuery();
            List<JsonArray> insertCitiesParams = insertsCities.stream()
                .map(GenericQuery::getParams)
                .collect(Collectors.toList());

            conn.batchWithParams(insertCitiesQuery, insertCitiesParams, (AsyncResult<List<Integer>> replyCities) -> {
              try {
                if (replyCities.failed()) {
                  throw replyCities.cause();
                }

                // Insert alliance services
                List<GenericQuery> insertsServices = new ArrayList<>();
                IntStream.range(0, services.size()).mapToObj(services::getJsonObject).forEach(service -> {
                  service.put(ALLIANCE_ID, allianceID).put(CREATED_BY, createdBy);
                  insertsServices.add(this.generateGenericCreate(ALLIANCE_SERVICE, service));
                });

                String insertServicesQuery = insertsServices.get(0).getQuery();
                List<JsonArray> insertServicesParams = insertsServices.stream()
                    .map(GenericQuery::getParams)
                    .collect(Collectors.toList());

                conn.batchWithParams(insertServicesQuery, insertServicesParams, (AsyncResult<List<Integer>> replyServices) -> {
                  try {
                    if (replyServices.failed()) {
                      throw  replyServices.cause();
                    }

                    this.commitTransaction(message, conn, new JsonObject().put(ID, allianceID));

                  } catch (Throwable ex) {
                    this.rollbackTransaction(message, conn, ex);
                  }

                });

              } catch (Throwable ex) {
                  this.rollbackTransaction(message, conn, ex);
              }
            });

          } catch (Throwable ex) {
            this.rollbackTransaction(message, conn, ex);
          }

        });

      } catch (Exception ex) {
          this.rollbackTransaction(message, conn, ex);
      }

    });
  }
}
