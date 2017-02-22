package org.pac4j.vertx.handler.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jAuthProvider;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Version of the Pac4j authentication handler which auto-deploys a CallbackHandler on the relative URL derived from
 * the path of the callback URL specified in the Clients object held within the Config object supplied in the
 * constructor.
 *
 * There is no requirement to use this handler rather than a SecurityHandler (and indeed where the same
 * callback URL is being potentially used for multiple authentication handlers it may muddy the waters) but it is supplied
 * as a convenience to anyone wanting to perform very simple indirect authentications.
 *
 * If there is a desire to use the same callback for multiple indirect authentications, it is recommended to explicitly
 * deploy a CallbackHandler for clarity.
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class CallbackDeployingPac4jAuthHandler extends SecurityHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackDeployingPac4jAuthHandler.class);

    // Consider coalescing the manager options into the handler options and then generating the manageroptions from them
    public CallbackDeployingPac4jAuthHandler(final Vertx vertx,
                                             final SessionStore<VertxWebContext> sessionStore,
                                             final Config config,
                                             final Router router,
                                             final Pac4jAuthProvider authProvider,
                                             final SecurityHandlerOptions options,
                                             final CallbackHandlerOptions callbackOptions) {
        super(vertx, sessionStore, config, authProvider, options);
        // Other null checks performed by parent class
        CommonHelper.assertNotNull("router", router);
        CommonHelper.assertNotBlank("callbackUrl", config.getClients().getCallbackUrl());

        final URI uri;
        try {
            uri = new URI(config.getClients().getCallbackUrl());
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage());
            throw toTechnicalException(e);
        }

        // Start manager verticle
        router.route(HttpMethod.GET, uri.getPath()).handler(authResultHandler(vertx, sessionStore, config, callbackOptions));
    }

    private Handler<RoutingContext> authResultHandler(final Vertx vertx,
                                                      final SessionStore<VertxWebContext> sessionStore,
                                                      final Config config,
                                                      final CallbackHandlerOptions callbackOptions) {
        return new CallbackHandler(vertx, sessionStore,  config, callbackOptions);
    }

}
