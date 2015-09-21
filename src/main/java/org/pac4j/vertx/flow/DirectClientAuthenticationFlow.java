package org.pac4j.vertx.flow;

import io.vertx.core.Vertx;
import org.pac4j.core.client.DirectClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.VertxWebContext;

import java.util.function.Consumer;

/**
 * @author jez
 */
public class DirectClientAuthenticationFlow extends BaseAuthenticationFlow<DirectClient> {

  public DirectClientAuthenticationFlow(Vertx vertx, DirectClient client) {
    super(vertx, client);
  }

  @Override
  public void initiate(final VertxWebContext webContext,
                       final Consumer<UserProfile> authResultHandler) {

    final Credentials credentials;
    try {
      credentials = client.getCredentials(webContext);
    } catch (final RequiresHttpAction e) {
      throw new TechnicalException("Unexpected HTTP action", e);
    }

    // Do we now have credentials?
    vertx.<UserProfile>executeBlocking(
      future -> {
        final UserProfile profile = client.getUserProfile(credentials, webContext);
        future.complete(profile);
      },
      result -> {
        if (result.succeeded()) {
          final UserProfile profile = result.result();
          authResultHandler.accept(profile);
        } else {
          throw new TechnicalException("Error retrieving user profile from client");
        }
      }
    );
  }

  @Override
  public boolean useSession(WebContext context) {
    return false;
  }

}
