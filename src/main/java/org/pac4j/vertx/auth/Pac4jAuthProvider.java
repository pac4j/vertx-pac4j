package org.pac4j.vertx.auth;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.vertx.Pac4jSessionAttributes;

/**
 * Interface to be implemented by any Pac4j auth provider. The methods exposed on this interface are used by the
 * standard Pac4j handler. Implementation of these methods therefore enables creation of custom auth providers
 * as required
 */
public interface Pac4jAuthProvider extends AuthProvider {
  Pac4jSessionAttributes getSessionAttributes(final RoutingContext ctx);

  /**
   * Compile the required authentication information from the routing context
   */
  JsonObject getAuthInfo(final RoutingContext routingContext);

  /**
   * Check the prerequisites for this auth provider. For example if information is to be stored in the
   * session, then the routing context should reference a session. This is to be called at runtime to check
   * prerequisites.
   *
   * In the event of a prerequisite not being met, then throw an appropriate RuntimeException. For example
   * if a session is required but not set, throw a NullPointerException. The routing context will automatically
   * fail as a result.
   * @param ctx
   */
  void checkPrerequisites(final RoutingContext ctx);

  void saveSessionAttributes(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes);
}
