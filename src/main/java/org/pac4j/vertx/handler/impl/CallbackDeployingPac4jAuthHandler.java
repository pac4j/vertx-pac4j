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
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.config.Config;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.vertx.auth.Pac4jAuthProvider;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Version of the Pac4j authentication handler which auto-deploys a CallbackHandler on the relative URL derived from
 * the path of the callback URL specified in the Clients object held within the Config object supplied in the
 * constructor.
 *
 * There is no requirement to use this handler rather than a RequiresAuthenticationHandler (and indeed where the same
 * callback URL is being potentially used for multiple authentication handlers it may muddy the waters) but it is supplied
 * as a convenience to anyone wanting to perform very simple indirect authentications.
 *
 * If there is a desire to use the same callback for multiple indirect authentications, it is recommended to explicitly
 * deploy a CallbackHandler for clarity.
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class CallbackDeployingPac4jAuthHandler extends RequiresAuthenticationHandler {

    private final Logger LOG = LoggerFactory.getLogger(CallbackDeployingPac4jAuthHandler.class);

    // Consider coalescing the manager options into the handler options and then generating the manageroptions from them
    public CallbackDeployingPac4jAuthHandler(final Vertx vertx,
                                             final Config config,
                                             final Router router,
                                             final Pac4jAuthProvider authProvider,
                                             final Pac4jAuthHandlerOptions options) {
        super(vertx, config, authProvider, options);
        // Other null checks performed by parent class
        CommonHelper.assertNotNull("router", router);
        CommonHelper.assertNotBlank("callbackUrl", config.getClients().getCallbackUrl());

        final URI uri;
        try {
            uri = new URI(config.getClients().getCallbackUrl());
        } catch (URISyntaxException e) {
            LOG.error(e.getStackTrace().toString());
            throw toTechnicalException(e);
        }

        // Start manager verticle
        router.route(HttpMethod.GET, uri.getPath()).handler(authResultHandler(vertx, config));
    }

    private Handler<RoutingContext> authResultHandler(final Vertx vertx, final Config config) {
        return new CallbackHandler(vertx, config);
    }

}
