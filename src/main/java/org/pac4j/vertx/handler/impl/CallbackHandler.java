/*
  Copyright 2014 - 2014 pac4j organization

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.pac4j.vertx.handler.impl;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.context.BaseConfig;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.vertx.HttpResponseHelper;
import org.pac4j.vertx.Pac4jAuthenticationResponse;
import org.pac4j.vertx.Pac4jSessionAttributes;
import org.pac4j.vertx.Pac4jWrapper;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callback handler for Vert.x pac4j binding. This handler finishes the stateful authentication process.
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class CallbackHandler extends BasePac4JAuthHandler {

  protected static final Logger logger = LoggerFactory.getLogger(CallbackHandler.class);

    public CallbackHandler(final Pac4jWrapper wrapper, final Pac4jAuthProvider authProvider, final Pac4jAuthHandlerOptions options) {
        super(wrapper, authProvider, options);
    }

  /**
   * Authenticates the given request.
   *
   * @param routingContext the vertx-web routing context for the request
   * @param sessionAttributes the session attributes
   * @param handler the handler
   */
  protected void authenticate(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes,
                              final Handler<Pac4jAuthenticationResponse> handler) {

    wrapper.authenticate(routingContext, clientName, sessionAttributes, handler);

  }

  @Override
  protected void retrieveUserProfile(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes,
                                     final Handler<Pac4jAuthenticationResponse> handler) {
    authenticate(routingContext, sessionAttributes, handler);
  }

  @Override
    protected void authenticationSuccess(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes) {

        redirectToTarget(routingContext, sessionAttributes);
    }

    protected void authenticationFailure(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes) {

        redirectToTarget(routingContext, sessionAttributes);
    }

    private void redirectToTarget(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes) {

      String requestedUrl = retrieveOriginalUrl(sessionAttributes);
      saveUrl(null, sessionAttributes);
      final String redirectUrl = defaultUrl(requestedUrl, BaseConfig.getDefaultSuccessUrl());
      pac4jAuthProvider.saveSessionAttributes(routingContext, sessionAttributes);
      HttpResponseHelper.redirect(routingContext.request(), redirectUrl);

    }

  /**
   * This method returns the default url from a specified url compared with a default url.
   *
   * @param url a specific url
   * @param defaultUrl the default url
   * @return the default url
   */
  public static String defaultUrl(final String url, final String defaultUrl) {
    String redirectUrl = defaultUrl;
    if (url != null && !"".equals(url)) {
      redirectUrl = url;
    }
    logger.debug("defaultUrl : {}", redirectUrl);
    return redirectUrl;
  }

  protected String retrieveOriginalUrl(final Pac4jSessionAttributes sessionAttributes) {
    return (String) sessionAttributes.getCustomAttributes().get(Pac4jConstants.REQUESTED_URL);
  }



}
