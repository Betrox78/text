package database.commons;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import utils.UtilsValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static service.commons.Constants.*;

public abstract class DBHandler<D extends DBVerticle> implements IDBHandler<D> {

  protected final D dbVerticle;
  protected final SQLClient dbClient;

  public DBHandler(D dbVerticle) {
    this.dbVerticle = dbVerticle;
    this.dbClient = dbVerticle.dbClient;
  }

  protected Vertx getVertx() {
    return dbVerticle.getVertx();
  }

  protected final void replyError(Message<JsonObject> message, Throwable cause) {
    dbVerticle.reportQueryError(message, cause);
  }

  protected final void replyResult(Message<JsonObject> message, Object result) {
    message.reply(result);
  }

  protected final void replyGeneric(Message<JsonObject> message, AsyncResult<ResultSet> reply) {
    dbVerticle.genericResponse(message, reply);
  }

  protected final GenericQuery generateGenericCreate(JsonObject body) {
    return dbVerticle.generateGenericCreate(body);
  }

  public final GenericQuery generateGenericCreate(String tableName, JsonObject body) {
    return dbVerticle.generateGenericCreateSendTableName(tableName, body);
  }

  protected final GenericQuery generateGenericUpdate(JsonObject body) {
    return dbVerticle.generateGenericUpdate(dbVerticle.getTableName(), body);
  }

  public final GenericQuery generateGenericUpdate(String tableName, JsonObject body) {
    return dbVerticle.generateGenericUpdate(tableName, body);
  }

  protected final GenericQuery generateGenericUpdate(JsonObject body, boolean persistNull) {
    return dbVerticle.generateGenericUpdate(dbVerticle.getTableName(), body, persistNull);
  }

  protected final GenericQuery generateGenericUpdate(String tableName, JsonObject body, boolean persistNull) {
    return dbVerticle.generateGenericUpdate(tableName, body, persistNull);
  }

  protected final GenericQuery generateGenericDelete(String tableName, JsonObject body) {
    return dbVerticle.generateGenericDelete(tableName, body);
  }

  protected final String orderBy(String orderBy) {
    return dbVerticle.orderBy(orderBy);
  }

  protected final void startTransaction(Message<JsonObject> message, Handler<SQLConnection> handler) {
    dbVerticle.startTransaction(message, handler);
  }

  protected final void rollbackTransaction(Message<JsonObject> message, SQLConnection connection, Throwable throwable) {
    dbVerticle.rollback(connection, throwable, message);
  }

  /**
   * Roll back the actual connection in transaction with a generic invalid exception message type to the messager
   *
   * @param con connection in actual transaction
   * @param t cause of the fail in an operation in the transaction
   * @param message message of the serder
   */
  protected final void rollback(SQLConnection con, Throwable t, Message<JsonObject> message) {
    t.printStackTrace();
    con.rollback(h -> {
      con.close();
      reportQueryError(message, t);
    });
  }

  protected final void commitTransaction(Message<JsonObject> message, SQLConnection connection, JsonObject jsonObject) {
    dbVerticle.commit(connection, message, jsonObject);
  }

  /**
   * Commit the actual transaction and replays to the sender the object provided
   *
   * @param con connection in actual transaction
   * @param message message of the sender
   * @param jsonObject object to reply
   */
  protected final void commit(SQLConnection con, Message<JsonObject> message, JsonObject jsonObject) {
    con.commit(h -> {
      con.close();
      message.reply(jsonObject);
    });
  }

  protected final Future<List<JsonObject>> getList(SQLConnection conn, String query, JsonArray params) {
    Future<List<JsonObject>> future = Future.future();

    conn.queryWithParams(query, params, replySearch -> {
      try {
        if (replySearch.failed()) {
          future.fail(replySearch.cause());
          return;
        }

        List<JsonObject> result = replySearch.result().getRows();
        future.complete(result);

      } catch (Throwable throwable) {
        future.fail(throwable);
      }
    });

    return future;
  }

  protected final Future<JsonObject> getOne(SQLConnection conn, String query, JsonArray params) {
    Future<JsonObject> future = Future.future();

    conn.queryWithParams(query, params, replySearch -> {
      try {
        if (replySearch.failed()) {
          future.fail(replySearch.cause());
          return;
        }

        Optional<JsonObject> result = replySearch.result().getRows().stream().findFirst();
        if (!result.isPresent()) {
          future.complete();
          return;
        }

        JsonObject found = result.get();

        future.complete(found);

      } catch (Throwable throwable) {
        future.fail(throwable);
      }
    });

    return future;
  }

  protected final Future<JsonObject> event(String dbAddress, String action, JsonObject body) {
    Future<JsonObject> future = Future.future();

    try {
      DeliveryOptions options = new DeliveryOptions().addHeader(ACTION, action);

      getVertx().eventBus().send(dbAddress, body, options, (AsyncResult<Message<JsonObject>> reply) -> {
        try{
          if (reply.failed()) {
            throw reply.cause();
          }

          Message<JsonObject> result = reply.result();

          if (result.headers().contains(ErrorCodes.DB_ERROR.toString())) {
            future.fail(String.valueOf(result.body()));
          } else {
            future.complete(result.body());
          }

        } catch (Throwable ex) {
          future.fail(ex);
        }

      });
    } catch (Exception ex) {
        future.fail(ex);
    }

    return future;
  }

  protected final void reportQueryError(Message<JsonObject> message, Throwable cause) {
    try {
      cause.printStackTrace();
      JsonObject catchedError = null;
      if (cause.getMessage().contains("foreign key")) { //a foreign key constraint fails
        Pattern p = Pattern.compile("`(.+?)`");
        Matcher m = p.matcher(cause.getMessage());
        List<String> incidencias = new ArrayList<>(5);
        while (m.find()) {
          incidencias.add(m.group(1));
        }
        catchedError = new JsonObject().put("name", incidencias.get(incidencias.size() - 3)).put("error", "does not exist in the catalog");
      }
      if (cause.getMessage().contains("Data too long")) { //data to long for column
        Pattern p = Pattern.compile("'(.+?)'");
        Matcher m = p.matcher(cause.getMessage());
        m.find();
        String propertyName = m.group(1);
        catchedError = new JsonObject().put("name", propertyName).put("error", "to long data");
      }
      if (cause.getMessage().contains("Unknown column")) {//unkown column
        Pattern p = Pattern.compile("'(.+?)'");
        Matcher m = p.matcher(cause.getMessage());
        m.find();
        String propertyName = m.group(1);
        catchedError = new JsonObject().put("name", propertyName).put("error", UtilsValidation.PARAMETER_DOES_NOT_EXIST);
      }
      if (cause.getMessage().contains("doesn't have a default")) { //not default value in not null
        Pattern p = Pattern.compile("'(.+?)'");
        Matcher m = p.matcher(cause.getMessage());
        m.find();
        String propertyName = m.group(1);
        catchedError = new JsonObject().put("name", propertyName).put("error", UtilsValidation.MISSING_REQUIRED_VALUE);
      }
      if (cause.getMessage().contains("Duplicate entry")) { //already exist (duplicate key for unique values)
        Pattern p = Pattern.compile("'(.+?)'");
        Matcher m = p.matcher(cause.getMessage());
        m.find();
        String value = m.group(1);
        m.find();
        String propertyName = m.group(1);
        catchedError = new JsonObject().put("name", propertyName).put("error", "value: " + value + " in " + UtilsValidation.ALREADY_EXISTS);
      }
      if (cause.getMessage().contains("Data truncation")) {
        Pattern p = Pattern.compile("'(.+?)'");
        Matcher m = p.matcher(cause.getMessage());
        m.find();
        String propertyName = m.group(1);
        catchedError = new JsonObject().put("name", propertyName).put("error", "to long data");
      }
      if (catchedError != null) {
        message.reply(catchedError, new DeliveryOptions().addHeader(ErrorCodes.DB_ERROR.toString(), catchedError.getString("error")));
      } else {
        message.fail(ErrorCodes.DB_ERROR.ordinal(), cause.getMessage());
      }
    } catch(Exception ex) {
      ex.printStackTrace();
      message.fail(500, ex.getMessage());
    }

  }

  public CompletableFuture<Integer> execGenericQueryCreate(SQLConnection conn, GenericQuery genericQuery) {
    CompletableFuture<Integer> future = new CompletableFuture<>();
    try {
      conn.updateWithParams(genericQuery.getQuery(), genericQuery.getParams(), reply -> {
        try {
          if (reply.failed()) {
            throw reply.cause();
          }
          future.complete(reply.result().getKeys().getInteger(0));
        } catch (Throwable t) {
          future.completeExceptionally(t);
        }
      });
    } catch (Throwable t) {
      future.completeExceptionally(t);
    }
    return future;
  }

  public CompletableFuture<Boolean> execGenericQueryUpdate(SQLConnection conn, GenericQuery genericQuery) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    try {
      conn.updateWithParams(genericQuery.getQuery(), genericQuery.getParams(), reply -> {
        try {
          if (reply.failed()) {
            throw reply.cause();
          }
          future.complete(true);
        } catch (Throwable t) {
          future.completeExceptionally(t);
        }
      });
    } catch (Throwable t) {
      future.completeExceptionally(t);
    }
    return future;
  }

}
