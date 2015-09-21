package org.pac4j.vertx.flow;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.VertxWebContext;

import java.util.function.Consumer;

/**
 * @author jez
 */
public class IndirectClientAuthenticationFlow extends BaseAuthenticationFlow<IndirectClient> {


  private static final Logger LOG = LoggerFactory.getLogger(IndirectClientAuthenticationFlow.class);

  public IndirectClientAuthenticationFlow(final Vertx vertx, final IndirectClient client) {
    super(vertx, client);
  }

  @Override
  public void initiate(final VertxWebContext webContext,
                       final Consumer<UserProfile> authResultHandler) {
    // To initiate the flow for an indirect client, we need to redirect the browser, we don't
    // care about the handler as the callback part must take care of authorization
    webContext.setSessionAttribute(Pac4jConstants.REQUESTED_URL, webContext.getFullRequestURL());
    try {
      final RedirectAction redirectAction = client.getRedirectAction(webContext, true);
      httpActionHandler.handleRedirect(redirectAction, webContext);
    } catch (RequiresHttpAction requiresHttpAction) {
      requiresHttpAction.printStackTrace();
    }
  }

  @Override
  public boolean useSession(WebContext context) {
    return true;
  }

}
