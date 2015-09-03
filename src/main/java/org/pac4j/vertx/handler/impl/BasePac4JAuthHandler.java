package org.pac4j.vertx.handler.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.BaseConfig;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.AuthHttpServerRequest;
import org.pac4j.vertx.Pac4jAuthenticationResponse;
import org.pac4j.vertx.Pac4jResponse;
import org.pac4j.vertx.Pac4jSessionAttributes;
import org.pac4j.vertx.Pac4jWrapper;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.auth.Pac4jUser;
import org.pac4j.vertx.handler.Pac4JAuthHandler;

import java.util.Optional;

/**
 * @author jez
 */
public abstract class BasePac4JAuthHandler extends AuthHandlerImpl implements Pac4JAuthHandler {

  private static final Logger LOG = LoggerFactory.getLogger(BasePac4JAuthHandler.class);
  protected final Boolean isAjax;
  protected final Pac4jWrapper wrapper;
  protected final String clientName;
  protected final Pac4jAuthProvider pac4jAuthProvider;

  public BasePac4JAuthHandler(final Vertx vertx, final Config config, final Pac4jAuthProvider authProvider,
                              final Pac4jAuthHandlerOptions options) {
    this(new Pac4jWrapper(vertx, config), authProvider, options);
  }

  public BasePac4JAuthHandler(final Pac4jWrapper wrapper,
                              final Pac4jAuthProvider authProvider,
                              final Pac4jAuthHandlerOptions options) {
    super(authProvider);
    clientName = options.clientName();
    this.pac4jAuthProvider = authProvider;
    isAjax = options.isAjax();
    this.wrapper = wrapper;
  }

  // Port of stateful Pac4J auth to a handler in vert.x 3. Note that a review of the stateful scenario is
  // likely to move some code into the AuthProvider, I just want to make a start on this to see which elements
  // relate really to the authprovider and which to the handler, so will have this encapsulate everything and
  // then move things out to the provider
  @Override
  public void handle(RoutingContext routingContext) {
    // Make this handler validation of state of routing context, and throw an appropriate
    // runtime exception (suggest that documentation makes this clear)
    pac4jAuthProvider.checkPrerequisites(routingContext);

    final User user = routingContext.user();
    if (user != null) {
      // Already logged in, just authorise
      authorise(user, routingContext);
    } else {
      // Now check our authprovider to see if we already have a token
      authProvider.authenticate(pac4jAuthProvider.getAuthInfo(routingContext), res -> {
        if (res.succeeded()) {
          routingContext.setUser(res.result());
          authorise(routingContext.user(), routingContext);
        } else {

          // We're not logged in or authenticated so we have something to do
          final Pac4jSessionAttributes sessionAttributes = pac4jAuthProvider.getSessionAttributes(routingContext);
          retrieveUserProfile(routingContext, sessionAttributes, pac4jResponse -> {
            if (wrapper.requiresHttpAction(pac4jResponse)) {
              ((Pac4jAuthProvider) authProvider).saveSessionAttributes(routingContext, sessionAttributes);
              sendResponse(routingContext.request().response(), pac4jResponse);
              return;
            }

            // authentication success or failure strategy
            Optional<UserProfile> profile = pac4jResponse.getProfile();

            if (profile.isPresent()) {
              saveUserProfile(profile, routingContext, sessionAttributes);
              routingContext.setUser(new Pac4jUser(profile.get(), pac4jAuthProvider));
              authenticationSuccess(routingContext, sessionAttributes);
            } else {
              authenticationFailure(routingContext, sessionAttributes);
            }

          });
        }
      });
    }

  }

  protected void authenticationSuccess(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes) {
    authorise(routingContext.user(), routingContext);
  }

  protected void sendResponse(final HttpServerResponse response, final Pac4jResponse event) {
    int code = event.getCode();
    response.setStatusCode(code);
    for (String headerName : event.getHeaders().fieldNames()) {
      response.putHeader(headerName, event.getHeaders().getString(headerName));
    }
    String content = event.getContent();
    if (code == 401) {
      content = BaseConfig.getErrorPage401();
    } else if (code == 403) {
      content = BaseConfig.getErrorPage403();
    }
    response.end(content);
  }


  protected abstract void retrieveUserProfile(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes,
                                     final Handler<Pac4jAuthenticationResponse> handler);

  /**
   * Default authentication failure strategy; save the original url in session and redirects to the Identity Provider if stateful.
   * Sends an unauthorized response if stateless.
   *
   * @param routingContext the vertx-web routing context
   * @param sessionAttributes the session attributes
   */
  protected void authenticationFailure(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes) {
    // no authentication tried -> redirect to provider
    // keep the current url
    saveOriginalUrl(routingContext.request(), sessionAttributes);
    // compute and perform the redirection
    redirectToIdentityProvider(routingContext, sessionAttributes);
  }

  /**
   * Redirects to the configured Identity Provider.
   *
   * @param routingContext the vertx-web routing context
   * @param sessionAttributes the session attributes
   */
  protected void redirectToIdentityProvider(final RoutingContext routingContext,
                                            final Pac4jSessionAttributes sessionAttributes) {
    wrapper.redirect(routingContext, clientName, sessionAttributes, true, isAjaxRequest(),
      response -> {

        Pac4jSessionAttributes sessionAttributes1 = response.getSessionAttributes();

        ((Pac4jAuthProvider) authProvider).saveSessionAttributes(routingContext, sessionAttributes1);
        sendResponse(routingContext.request().response(), response);
      });
  }

  /**
   * Save User Profile in session if stateful. Wraps and returns the current {@link HttpServerRequest} in an {@link AuthHttpServerRequest}.
   *
   * @param profile the user profile
   * @param routingContext the current routing context
   * @param sessionAttributes the session attributes
   */
  protected void saveUserProfile(final Optional<UserProfile> profile, final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes) {
    profile.ifPresent(sessionAttributes::setUserProfile);
    ((Pac4jAuthProvider) authProvider).saveSessionAttributes(routingContext, sessionAttributes);

  }

  /**
   * Is the current request an Ajax request.
   *
   * @return whether it is an AJAX call
   */
  protected boolean isAjaxRequest() {
    return isAjax;
  }

  /**
   * Save the original url in session if the request is not Ajax.
   *
   * @param req the HTTP request
   * @param sessionAttributes the session attributes
   */
  protected void saveOriginalUrl(HttpServerRequest req, final Pac4jSessionAttributes sessionAttributes) {
    if (!isAjaxRequest()) {
      final String requestedUrlToSave = req.absoluteURI();
      saveUrl(requestedUrlToSave, sessionAttributes);
    }
  }

  protected void saveUrl(final String requestedUrlToSave, final Pac4jSessionAttributes sessionAttributes) {
    sessionAttributes.getCustomAttributes().put(Pac4jConstants.REQUESTED_URL, requestedUrlToSave);
    LOG.debug("requestedUrlToSave : " + requestedUrlToSave);
  }
}
