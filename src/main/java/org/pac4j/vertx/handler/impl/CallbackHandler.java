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
import org.pac4j.core.http.HttpActionAdapter;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.http.DefaultHttpActionAdapter;

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
    private final SessionStore<VertxWebContext> sessionStore;
    private final Config config;

    // Config elements which are all optional
    private final Boolean multiProfile;
    private final Boolean renewSession;
    private final String defaultUrl;

    private final CallbackLogic<Void, VertxWebContext> callbackLogic = new DefaultCallbackLogic();
    {
        ((DefaultCallbackLogic<Void, VertxWebContext>) callbackLogic)
                .setProfileManagerFactory(VertxProfileManager::new);
    }

    public CallbackHandler(final Vertx vertx,
                           final SessionStore<VertxWebContext> sessionStore,
                           final Config config,
                           final CallbackHandlerOptions options) {
        this.vertx = vertx;
        this.sessionStore = sessionStore;
        this.config = config;
        this.multiProfile = options.getMultiProfile();
        this.renewSession = options.getRenewSession();
        this.defaultUrl = options.getDefaultUrl();
    }

    @Override
    public void handle(RoutingContext event) {

        // Can we complete the authentication process here?
        final VertxWebContext webContext = new VertxWebContext(event, sessionStore);

        vertx.executeBlocking(future -> {
                    callbackLogic.perform(webContext, config, httpActionHandler, defaultUrl, multiProfile, renewSession);
                    future.complete(null);
                },
                false,
                asyncResult -> {
                    // If we succeeded we're all good here, the job is done either through approving, or redirect, or
                    // forbidding
                    // However, if an error occurred we need to handle this here
                    if (asyncResult.failed()) {
                        event.fail(new TechnicalException(asyncResult.cause()));
                    }
                });

    }

}
