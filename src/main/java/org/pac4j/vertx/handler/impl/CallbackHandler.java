package org.pac4j.vertx.handler.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.http.HttpActionAdapter;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.core.engine.VertxCallbackLogic;
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
    private final Config config;
    private final boolean multiProfile;

    protected String defaultUrl = Pac4jConstants.DEFAULT_URL_VALUE;
    private final CallbackLogic<Void, VertxWebContext> callbackLogic = new VertxCallbackLogic();

    public CallbackHandler(final Vertx vertx,
                           final Config config,
                           final boolean multiProfile) {
        this.vertx = vertx;
        this.config = config;
        this.multiProfile = multiProfile;
    }

    @Override
    public void handle(RoutingContext event) {

        // Can we complete the authentication process here?
        final VertxWebContext webContext = new VertxWebContext(event);

        vertx.executeBlocking(future ->
                callbackLogic.perform(webContext, config, httpActionHandler, defaultUrl, multiProfile,  false),
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
