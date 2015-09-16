package org.pac4j.vertx.handler.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.handler.HttpActionHandler;

/**
 * @author jez
 */
public class DefaultHttpActionHandler implements HttpActionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultHttpActionHandler.class);

  @Override
  public void handle(int code, VertxWebContext context) {

  }

  @Override
  public void handleRedirect(final RedirectAction action, final VertxWebContext webContext)
  {
    switch (action.getType()) {

      case REDIRECT:
        // Send a redirect response
        webContext.setResponseStatus(302);
        webContext.setResponseHeader("location", action.getLocation());
        webContext.completeResponse();
        break;

      case SUCCESS:
        break;

      default:
        throw new TechnicalException("Unsupported RedirectAction type: " + action.getType());
    }
  }
}
