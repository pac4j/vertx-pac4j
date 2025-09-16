package org.pac4j.vertx.handler.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.vertx.VertxFrameworkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callback handler for Vert.x pac4j binding. This handler finishes the stateful authentication process.
 *
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class CallbackHandler implements Handler<RoutingContext> {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackHandler.class);

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
    public void handle(final RoutingContext rc) {
        final CallbackLogic callbackLogic =
                (config.getCallbackLogic() != null) ? config.getCallbackLogic() : DefaultCallbackLogic.INSTANCE;

        vertx.<Void>executeBlocking(() -> {
                            callbackLogic.perform(
                                    config,
                                    defaultUrl,
                                    renewSession,
                                    defaultClient,
                                    new VertxFrameworkParameters(rc)
                            );
                            return null;
                        },
                        false
                )
                .onComplete(ar -> {
                    if (ar.failed()) {
                        rc.fail(new TechnicalException(ar.cause()));
                    } else {
                        LOG.debug("Callback handled for {}", rc.request().path());
                    }
                });
    }
}
