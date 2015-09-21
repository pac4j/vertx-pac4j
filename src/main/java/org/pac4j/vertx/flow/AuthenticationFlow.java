package org.pac4j.vertx.flow;

import io.vertx.core.Vertx;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.DirectClient;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.VertxWebContext;

import java.util.function.Consumer;

/**
 * Interface representing an authentication flow, effectively wrapping the differences required
 * when initiating authentication for a direct or indirect client (the former actually needs to
 * carry out authentication, the latter initiates the authentication flow by issuing a redirect
 * response
 * @author jez
 */
public interface AuthenticationFlow<T extends Client> {

  static AuthenticationFlow flowFor(final Vertx vertx, final Client client) {
    return (client instanceof IndirectClient) ?
      new IndirectClientAuthenticationFlow(vertx, (IndirectClient) client) :
      new DirectClientAuthenticationFlow(vertx, (DirectClient) client);
  }
  void initiate(final VertxWebContext webContext,
                final Consumer<UserProfile> authResultHandler);

  boolean useSession(final WebContext context);

}
