package org.pac4j.vertx.handler.impl;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import org.pac4j.vertx.Pac4jAuthenticationResponse;
import org.pac4j.vertx.Pac4jSessionAttributes;
import org.pac4j.vertx.Pac4jWrapper;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jAuthProvider;

import java.util.Set;

/**
 * @author jez
 */
public class StatefulPac4jAuthHandler extends BasePac4JAuthHandler {

  // Consider coalescing the manager options into the handler options and then generating the manageroptions from them
  public StatefulPac4jAuthHandler(final Router router,
                                  final Pac4jWrapper wrapper,
                                  final Pac4jAuthProvider authProvider,
                                  final Pac4jAuthHandlerOptions options) {
    super(wrapper, authProvider, options);

    // Start manager verticle
    router.route(HttpMethod.GET, "/authResult").handler(authResultHandler(options));
  }

  private Handler<RoutingContext> authResultHandler(Pac4jAuthHandlerOptions options) {
    return new CallbackHandler(this.wrapper, this.pac4jAuthProvider, options);
  }

  @Override
  public AuthHandler addAuthority(String authority) {
    return this;
  }

  @Override
  public AuthHandler addAuthorities(Set<String> authorities) {
    return this;
  }

  @Override
  protected void retrieveUserProfile(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes, Handler<Pac4jAuthenticationResponse> handler) {
    final VertxWebContext context = new VertxWebContext(routingContext, sessionAttributes);
    handler.handle(new Pac4jAuthenticationResponse(context));
  }
}
