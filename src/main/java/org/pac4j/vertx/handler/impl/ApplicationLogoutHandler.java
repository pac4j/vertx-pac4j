/*
  Copyright 2015 - 2015 pac4j organization

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
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;

import java.util.regex.Pattern;

/**
 * Implementation of a handler for handling pac4j user logout
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class ApplicationLogoutHandler implements Handler<RoutingContext> {

    protected String defaultUrl = Pac4jConstants.DEFAULT_URL_VALUE;
    protected String logoutUrlPattern = Pac4jConstants.DEFAULT_LOGOUT_URL_PATTERN_VALUE;

    @Override
    public void handle(final RoutingContext routingContext) {

        CommonHelper.assertNotBlank(Pac4jConstants.DEFAULT_URL, this.defaultUrl);
        CommonHelper.assertNotBlank(Pac4jConstants.LOGOUT_URL_PATTERN, this.logoutUrlPattern);

        // We need to clear pac4j data from the session
        // also clear the user from the routing context (in case we're also using a UserSessionHandler which will
        // also persist the user to the session)
        // and redirect the user to an appropriate endpoint
        final VertxWebContext webContext = new VertxWebContext(routingContext);
        final ProfileManager manager = new VertxProfileManager(webContext);
        manager.logout();
        routingContext.setUser(null);

        // Now user data should be cleared out so the final redirect should be sufficient

        final String url = webContext.getRequestParameter(Pac4jConstants.URL);
        if (url == null) {
            routingContext.response().setStatusCode(HttpConstants.OK);
            routingContext.response().end();
        } else {
            if (Pattern.matches(this.logoutUrlPattern, url)) {
                redirect(webContext, url);
            } else {
                redirect(webContext, this.defaultUrl);
            }
        }

    }

    protected void redirect(final VertxWebContext webContext, final String url) {
        webContext.setResponseStatus(HttpConstants.TEMP_REDIRECT);
        webContext.setResponseHeader(HttpConstants.LOCATION_HEADER, url);
        webContext.completeResponse();
    }

}
