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

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import org.pac4j.core.client.*;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.auth.Pac4jUser;
import org.pac4j.vertx.http.DefaultHttpActionAdapter;
import org.pac4j.vertx.http.HttpActionAdapter;

import java.util.List;
import java.util.Optional;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class RequiresAuthenticationHandler extends AuthHandlerImpl {

    private static final Logger LOG = LoggerFactory.getLogger(RequiresAuthenticationHandler.class);

    protected final Config config;
    protected final String clientName;
    protected final Vertx vertx;

    protected HttpActionAdapter httpActionAdapter = new DefaultHttpActionAdapter();
    protected ClientFinder clientFinder = new DefaultClientFinder();

    public RequiresAuthenticationHandler(final Vertx vertx, final Config config, final Pac4jAuthProvider authProvider,
                                         final Pac4jAuthHandlerOptions options) {
        super(authProvider);
        clientName = options.clientName();
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
            final List<Client> currentClients = clientFinder.find(config.getClients(), webContext, this.clientName);
            final ProfileManager profileManager = new ProfileManager(webContext);

            UserProfile profile = profileManager.get(useSession(webContext, currentClients));
            if (profile != null) {
                // We have been authenticated, are we authorized? Use vert.x subsystem for this
                authorise(new Pac4jUser(profile), routingContext);
            } else {
                // We have not been authenticated yet, so start of the authentication process
                // First try direct clients complying to client name filter, then attempt indirect
                // authentication
                vertx.<Optional<UserProfile>>executeBlocking(
                    future -> {
                        // Note the following stream should NOT be executed in parallel
                        // as it may contain blocking operations which could block threads from the
                        // fork/join pool.
                        // This needs to be in an executeBlocking segment as it may involve blocking i/o
                        // so needs to be kept off the vert.x event loop
                        future.complete(currentClients.stream()
                            .filter(client -> client instanceof DirectClient)
                            .map(client -> {
                                try {
                                    final Credentials credentials = client.getCredentials(webContext);
                                    return client.getUserProfile(credentials, webContext);
                                } catch (RequiresHttpAction requiresHttpAction) {
                                    throw new TechnicalException("Unexpected HTTP action", requiresHttpAction);
                                }
                            })
                            .filter(userProfile -> userProfile != null)
                            .findFirst());
                    },
                    asyncResult -> {
                        if (asyncResult.succeeded()){

                            // Non error condition but we might not have a profile
                            final Optional<UserProfile> profileOption = asyncResult.result();
                            if (profileOption.isPresent()) {
                                // We/ve been successfully authenticated so authorise
                                final UserProfile authenticatedProfile = profileOption.get();
                                profileManager.save(useSession(webContext, currentClients), authenticatedProfile);
                                authorise(new Pac4jUser(authenticatedProfile), routingContext);
                            } else {
                                // direct authentication failed so attempt indirect authentication if possible,
                                // otherwise fail authentication
                                if (startAuthentication(webContext, currentClients)) {
                                    LOG.debug("Starting authentication");
                                    saveRequestedUrl(webContext, currentClients);
                                    redirectToIdentityProvider(webContext, currentClients);
                                } else {
                                    LOG.debug("unauthorized");
                                    unauthorized(webContext, currentClients);
                                }
                            }

                        } else {
                            throw toTechnicalException(asyncResult.cause());
                        }

                    }
                );
            }
        }

    }

    protected void unauthorized(VertxWebContext webContext, List<Client> currentClients) {
        httpActionAdapter.handle(HttpConstants.UNAUTHORIZED, webContext);
    }

    protected boolean startAuthentication(final VertxWebContext context, final List<Client> currentClients) {
        return currentClients != null && currentClients.size() > 0 && currentClients.get(0) instanceof IndirectClient;
    }

    protected void saveRequestedUrl(final WebContext context, final List<Client> currentClients) {
        final String requestedUrl = context.getFullRequestURL();
        LOG.debug("requestedUrl: " + requestedUrl);
        context.setSessionAttribute(Pac4jConstants.REQUESTED_URL, requestedUrl);
    }

    protected void redirectToIdentityProvider(final VertxWebContext webContext, final List<Client> currentClients) {
        try {
            final IndirectClient currentClient = (IndirectClient) currentClients.get(0);
            final RedirectAction action = currentClient.getRedirectAction(webContext, true);
            LOG.debug("redirectAction: " + action);
            httpActionAdapter.handleRedirect(action, webContext);
        } catch (final RequiresHttpAction requiresHttpAction) {
            LOG.debug("extra HTTP action required: " + requiresHttpAction.getCode());
            httpActionAdapter.handle(requiresHttpAction.getCode(), webContext);
        }
    }

    protected boolean useSession(final WebContext context, final List<Client> currentClients) {
        return currentClients == null || currentClients.size() == 0 || currentClients.get(0) instanceof IndirectClient;
    }

    private final TechnicalException toTechnicalException(final Throwable t) {
        return (t instanceof TechnicalException) ? (TechnicalException) t : new TechnicalException(t);
    }

}
