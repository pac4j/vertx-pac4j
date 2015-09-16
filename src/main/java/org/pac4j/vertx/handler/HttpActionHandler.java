package org.pac4j.vertx.handler;

import org.pac4j.core.client.RedirectAction;
import org.pac4j.vertx.VertxWebContext;

/**
 * @author jez
 */
public interface HttpActionHandler {

  /**
   * Handle HTTP action.
   *
   * @param code the HTTP status code
   * @param context the web context
   * @return void
   */
  void handle(final int code, final VertxWebContext context);

  /**
   * Handle HTTP action for redirection use cases.
   *
   * @param action the pac4j action to perform
   * @return void
   */
  void handleRedirect(final RedirectAction action, final VertxWebContext webContext);
}
