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
 * @author Jeremy Prime
 * @since 2.0.0
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
        saveRequestedUrl(webContext);
        redirectToIdentityProvider(webContext);
    }

    protected void redirectToIdentityProvider(final VertxWebContext webContext) {
        try {
            final RedirectAction redirectAction = client.getRedirectAction(webContext, true);
            httpActionHandler.handleRedirect(redirectAction, webContext);
        } catch (RequiresHttpAction requiresHttpAction) {
            requiresHttpAction.printStackTrace();
        }
    }

    protected void saveRequestedUrl(final VertxWebContext webContext) {
        webContext.setSessionAttribute(Pac4jConstants.REQUESTED_URL, webContext.getFullRequestURL());
    }

    @Override
    public boolean useSession(WebContext context) {
        return true;
    }

}
