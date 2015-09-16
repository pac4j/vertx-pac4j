package org.pac4j.vertx.handler.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.config.Config;
import org.pac4j.vertx.auth.Pac4jAuthProvider;

/**
 * @author jez
 */
public class CallbackDeployingPac4jAuthHandler extends RequiresAuthenticationHandler {

  // Consider coalescing the manager options into the handler options and then generating the manageroptions from them
  public CallbackDeployingPac4jAuthHandler(final Vertx vertx,
                                           final Config config,
                                           final Router router,
                                           final Pac4jAuthProvider authProvider,
                                           final Pac4jAuthHandlerOptions options) {
    super(vertx, config, authProvider, options);

    // Start manager verticle
    router.route(HttpMethod.GET, "/authResult").handler(authResultHandler(vertx, config, options));
  }

  private Handler<RoutingContext> authResultHandler(final Vertx vertx, final Config config, Pac4jAuthHandlerOptions options) {
    return new CallbackHandler(vertx, config);
  }

}
