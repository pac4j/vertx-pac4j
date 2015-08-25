package org.pac4j.vertx.handler.impl;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.vertx.Pac4jAuthenticationResponse;
import org.pac4j.vertx.Pac4jSessionAttributes;
import org.pac4j.vertx.Pac4jWrapper;
import org.pac4j.vertx.auth.Pac4jAuthProvider;

// Port of stateless Pac4J auth to a handler in vert.x 3. Note that a review of the stateful scenario is
// likely to move some code into the AuthProvider, I just want to make a start on this to see which elements
// relate really to the authprovider and which to the handler, so will have this encapsulate everything and
// then move things out to the provider
public class StatelessPac4jAuthHandler extends BasePac4JAuthHandler {

  public StatelessPac4jAuthHandler(final Pac4jWrapper pac4jWrapper,
                                   final Pac4jAuthProvider authProvider,
                                   final Pac4jAuthHandlerOptions options) {
    super(pac4jWrapper, authProvider, options);

  }

  @Override
  public void handle(final RoutingContext routingContext) {
    super.handle(routingContext);
  }

  @Override
  protected void retrieveUserProfile(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes, Handler<Pac4jAuthenticationResponse> handler) {
    wrapper.authenticate(routingContext, clientName, sessionAttributes, handler);
  }

}
