package org.pac4j.vertx.handler.impl;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.auth.Pac4jUser;
import org.pac4j.vertx.flow.AuthenticationFlow;

import java.util.function.Consumer;

/**
 * @author jez
 */
public class RequiresAuthenticationHandler extends AuthHandlerImpl {

  private static final Logger LOG = LoggerFactory.getLogger(RequiresAuthenticationHandler.class);
  protected final Config config;
  protected final String clientName;
  protected final Vertx vertx;
  private final boolean allowDynamicClientSelection;

  public RequiresAuthenticationHandler(final Vertx vertx, final Config config, final Pac4jAuthProvider authProvider,
                                       final Pac4jAuthHandlerOptions options) {
    super(authProvider);
    clientName = options.clientName();
    allowDynamicClientSelection = options.allowDynamicClientSelection();
    this.vertx = vertx;
    this.config = config;
  }

  // Port of Pac4J auth to a handler in vert.x 3.
  @Override
  public void handle(RoutingContext routingContext) {

    final User user = routingContext.user();
    if (user != null) {
      // Already logged in, just authorise
      authorise(user, routingContext);
    } else {
      final VertxWebContext webContext = new VertxWebContext(routingContext);
      final Client client = findClient(webContext, this.clientName, allowDynamicClientSelection);
      final ProfileManager profileManager = new ProfileManager(webContext);
      final AuthenticationFlow authenticationFlow = getAuthenticationFlow(client);

      UserProfile profile = profileManager.get(authenticationFlow.useSession(webContext));
      if (profile != null) {
        // We have been authenticated, are we authorized? Use vert.x subsystem for this
        authorise(new Pac4jUser(profile), routingContext);
      } else {
        // We have not been authenticated yet, so start of the authentication process
        authenticationFlow.initiate(webContext, new Consumer<UserProfile>() {
          @Override
          public void accept(UserProfile userProfile) {
            final Pac4jUser pac4jUser = new Pac4jUser(userProfile);
            if (userProfile != null) {
              profileManager.save(authenticationFlow.useSession(webContext), userProfile);
              // Writing into the routing context now means that if the UserSessionHandler is in use then nothing
              // else needs to be done to retrieve the user from the session and write to the routing context
              routingContext.setUser(pac4jUser);
            }
            RequiresAuthenticationHandler.this.authorise(pac4jUser, routingContext);
          }
        });
      }
    }

  }

  protected Client findClient(VertxWebContext webContext, String clientName, boolean allowDynamicClientSelection) {
    final Clients clients = config.getClients();
    Client client = null;
    if (allowDynamicClientSelection) {
      client = clients.findClient(webContext);
    }
    if (client == null) {
      client = clients.findClient(clientName);
    }
    return client;
  }

  protected AuthenticationFlow getAuthenticationFlow(final Client client) {
    return AuthenticationFlow.flowFor(vertx, client);
  }

}
