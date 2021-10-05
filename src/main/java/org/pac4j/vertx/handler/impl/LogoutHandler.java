package org.pac4j.vertx.handler.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.DefaultLogoutLogic;
import org.pac4j.core.engine.LogoutLogic;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.util.FindBest;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.http.VertxHttpActionAdapter;

/**
 * Implementation of a handler for handling pac4j user logout
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class LogoutHandler implements Handler<RoutingContext> {

    protected final String defaultUrl;
    protected final String logoutUrlPattern;
    protected final Config config;

    private final Vertx vertx;
    private final SessionStore sessionStore;
    private final boolean localLogout;
    private final boolean destroySession;
    private final boolean centralLogout;

    /**
     * Construct based on the option values provided
     *
     * @param vertx the vertx API
     * @param sessionStore the session store
     * @param options - the options to configure this handler
     * @param config the pac4j configuration
     */
    public LogoutHandler(final Vertx vertx,
                         final SessionStore sessionStore ,
                         final LogoutHandlerOptions options, final Config config) {
        this.defaultUrl = options.getDefaultUrl();
        this.logoutUrlPattern = options.getLogoutUrlPattern();
        this.config = config;
        this.vertx = vertx;
        this.sessionStore = sessionStore;
        this.localLogout = options.isLocalLogout();
        this.destroySession = options.isDestroySession();
        this.centralLogout = options.isCentralLogout();
    }

    @Override
    public void handle(final RoutingContext routingContext) {

        final LogoutLogic bestLogic = FindBest.logoutLogic(null, config, DefaultLogoutLogic.INSTANCE);
        final HttpActionAdapter bestAdapter = FindBest.httpActionAdapter(null, config, VertxHttpActionAdapter.INSTANCE);

        final VertxWebContext webContext = new VertxWebContext(routingContext, sessionStore);

        vertx.executeBlocking(future -> {
            bestLogic.perform(webContext, sessionStore, config, bestAdapter, defaultUrl, logoutUrlPattern, localLogout, destroySession, centralLogout);
            future.complete(null);
        },
        false,
        asyncResult -> {
            // If we succeeded we're all good here, the job is done either through approving, or redirect, or
            // forbidding
            // However, if an error occurred we need to handle this here
            if (asyncResult.failed()) {
                routingContext.fail(new TechnicalException(asyncResult.cause()));
            }
        });
    }
}
