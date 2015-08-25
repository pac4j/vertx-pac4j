package org.pac4j.vertx.auth.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.vertx.Pac4jSessionAttributes;
import org.pac4j.vertx.auth.Pac4jAuthProvider;

/**
 * @author jez
 */
public class StatelessPac4jAuthProviderImpl implements Pac4jAuthProvider {

  @Override
  public Pac4jSessionAttributes getSessionAttributes(final RoutingContext ctx) {
    return new Pac4jSessionAttributes();
  }

  @Override
  public JsonObject getAuthInfo(final RoutingContext routingContext) {
    return new JsonObject();
  }

  @Override
  public void checkPrerequisites(final RoutingContext ctx) {
    // intentional noop - there are no prerequisites for stateless authentication
  }

  @Override
  public void saveSessionAttributes(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes) {
    // Intentional noop - we won't persist the "session attributes"
  }

  @Override
  public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
    // We'll just let the handler delegate back to pac4j, we don't need to do any more than that
    resultHandler.handle(Future.failedFuture("Delegate to pac4j subsystem for authentication"));
  }
}
