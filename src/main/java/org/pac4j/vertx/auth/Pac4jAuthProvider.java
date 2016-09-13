package org.pac4j.vertx.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

/**
 * Vert.x authprovider for pac4j libraries. In the case of this auth provider we will always
 * just delegate to pac4j via the handler, so this should be sufficient
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class Pac4jAuthProvider implements AuthProvider {
  @Override
  public void authenticate(JsonObject jsonObject, Handler<AsyncResult<User>> handler) {
    // We'll just let the handler delegate back to pac4j, we don't need to do any more than that
    handler.handle(Future.failedFuture("Delegate to pac4j subsystem for authentication"));
  }
}
