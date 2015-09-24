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
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.http.DefaultHttpActionAdapter;
import org.pac4j.vertx.http.HttpActionAdapter;

import java.util.Optional;

/**
 * Callback handler for Vert.x pac4j binding. This handler finishes the stateful authentication process.
 *
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class CallbackHandler implements Handler<RoutingContext> {

    protected static final Logger LOG = LoggerFactory.getLogger(CallbackHandler.class);

    private final HttpActionAdapter httpActionHandler = new DefaultHttpActionAdapter();
    private final Vertx vertx;
    private final Config config;

    protected String defaultUrl = Pac4jConstants.DEFAULT_URL_VALUE;

    public CallbackHandler(final Vertx vertx,
                           final Config config) {
        this.vertx = vertx;
        this.config = config;
    }

    @Override
    public void handle(RoutingContext event) {

        // Can we complete the authentication process here?
        final VertxWebContext webContext = new VertxWebContext(event);
        final ProfileManager profileManager = new ProfileManager(webContext);
        final Client client = config.getClients().findClient(webContext);

        CommonHelper.assertNotNull("client", client);
        CommonHelper.assertTrue(client instanceof IndirectClient, "only indirect clients are allowed on the callback url");

        final Credentials credentials;
        try {
            credentials = client.getCredentials(webContext);
        } catch (final RequiresHttpAction e) {
            LOG.warn("Requires http action: " + e.getCode());
            httpActionHandler.handle(e.getCode(), webContext);
            return;
        }
        vertx.<CommonProfile>executeBlocking(future -> {
            final CommonProfile profile = (CommonProfile) client.getUserProfile(credentials, webContext);
            future.complete(profile);
        }, result -> {
            if (result.succeeded()) {
                Optional.ofNullable(result.result()).ifPresent(commonProfile -> {
                    profileManager.save(true, commonProfile);
                });
                redirectToOriginallyRequestedUrl(webContext);
            } else {
                event.fail(new TechnicalException("Failed to retrieve user profile"));
            }
        });
    }

    private void redirectToOriginallyRequestedUrl(final VertxWebContext webContext) {

        String redirectToUrl = (String) webContext.getSessionAttribute(Pac4jConstants.REQUESTED_URL);
        LOG.debug("redirectToUrl: " + redirectToUrl);
        if (CommonHelper.isNotBlank(redirectToUrl)) {
            webContext.setSessionAttribute(Pac4jConstants.REQUESTED_URL, null);
        } else {
            redirectToUrl = this.defaultUrl;
        }
        webContext.setResponseStatus(302);
        webContext.setResponseHeader("location", redirectToUrl);
        webContext.completeResponse();

    }

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public void setDefaultUrl(String defaultUrl) {
        this.defaultUrl = defaultUrl;
    }

}
