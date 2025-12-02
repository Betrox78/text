package database.commons;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.io.IOException;

public interface IDBHandler<D extends DBVerticle> {

  /**
   * Message handler, this must be implemented by the final class
   * @param message Message from event bus
   */
  void handle(Message<JsonObject> message) throws IOException;

}
