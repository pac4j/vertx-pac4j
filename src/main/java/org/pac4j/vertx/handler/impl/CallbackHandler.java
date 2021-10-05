package org.pac4j.vertx.handler.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.util.FindBest;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.http.VertxHttpActionAdapter;

/**
 * Callback handler for Vert.x pac4j binding. This handler finishes the stateful authentication process.
 *
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class CallbackHandler implements Handler<RoutingContext> {

    protected static final Logger LOG = LoggerFactory.getLogger(CallbackHandler.class);

    private final Vertx vertx;
    private final SessionStore sessionStore;
    private final Config config;

    // Config elements which are all optional
    private final String defaultUrl;
    private final Boolean renewSession;
    private final String defaultClient;

    public CallbackHandler(final Vertx vertx,
                           final SessionStore sessionStore,
                           final Config config,
                           final CallbackHandlerOptions options) {
        this.vertx = vertx;
        this.sessionStore = sessionStore;
        this.config = config;
        this.defaultUrl = options.getDefaultUrl();
        this.renewSession = options.getRenewSession();
        this.defaultClient = options.getDefaultClient();
    }

    @Override
    public void handle(RoutingContext event) {

        final CallbackLogic bestLogic = FindBest.callbackLogic(null, config, DefaultCallbackLogic.INSTANCE);
        final HttpActionAdapter bestAdapter = FindBest.httpActionAdapter(null, config, VertxHttpActionAdapter.INSTANCE);

        // Can we complete the authentication process here?
        final VertxWebContext webContext = new VertxWebContext(event, sessionStore);

        vertx.executeBlocking(future -> {
            bestLogic.perform(webContext, sessionStore, config, bestAdapter, defaultUrl, renewSession, defaultClient);
            future.complete(null);
        },
        false,
        asyncResult -> {
            // If we succeeded we're all good here, the job is done either through approving, or redirect, or
            // forbidding. However, if an error occurred we need to handle this here
            if (asyncResult.failed()) {
                event.fail(new TechnicalException(asyncResult.cause()));
            }
        });
    }
}
