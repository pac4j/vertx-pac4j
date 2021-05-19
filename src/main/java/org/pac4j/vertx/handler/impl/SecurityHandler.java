package org.pac4j.vertx.handler.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.util.FindBest;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.context.session.VertxSessionStore;
import org.pac4j.vertx.http.VertxHttpActionAdapter;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class SecurityHandler extends AuthHandlerImpl {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityHandler.class);

    protected final Config config;

    protected final String clientNames;
    protected final String authorizerName;
    protected final String matcherName;
    protected final boolean multiProfile;
    protected final Vertx vertx;
    private final SessionStore<VertxWebContext> sessionStore;

    static {
        Config.defaultProfileManagerFactory("VertxProfileManager", ctx -> new VertxProfileManager((VertxWebContext) ctx));
        Config.defaultProfileManagerFactory2("VertxProfileManager2", (ctx, store) -> new VertxProfileManager((VertxWebContext) ctx, (VertxSessionStore) store));
    }

    public SecurityHandler(final Vertx vertx,
                           final SessionStore<VertxWebContext> sessionStore,
                           final Config config, final Pac4jAuthProvider authProvider,
                           final SecurityHandlerOptions options) {
        super(authProvider);
        CommonHelper.assertNotNull("vertx", vertx);
        CommonHelper.assertNotNull("sessionStore", sessionStore);
        CommonHelper.assertNotNull("config", config);
        CommonHelper.assertNotNull("config.getClients()", config.getClients());
        CommonHelper.assertNotNull("authProvider", authProvider);
        CommonHelper.assertNotNull("options", options);

        clientNames = options.getClients();
        authorizerName = options.getAuthorizers();
        matcherName = options.getMatchers();
        multiProfile = options.isMultiProfile();
        this.vertx = vertx;
        this.sessionStore = sessionStore;
        this.config = config;
    }

    // Port of Pac4J auth to a handler in vert.x 3.
    @Override
    public void handle(final RoutingContext routingContext) {

        final SecurityLogic<Void, VertxWebContext> bestLogic = FindBest.securityLogic(null, config, DefaultSecurityLogic.INSTANCE);
        final HttpActionAdapter<Void, VertxWebContext> bestAdapter = FindBest.httpActionAdapter(null, config, VertxHttpActionAdapter.INSTANCE);

        final VertxWebContext webContext = new VertxWebContext(routingContext, sessionStore);

        vertx.executeBlocking(future -> bestLogic.perform(webContext, config,
            (ctx, profiles, parameters) -> {
                // This is what should occur if we are authenticated and authorized to view the requested
                // resource
                future.complete();
                return null;
            },
            bestAdapter,
            clientNames,
            authorizerName,
            matcherName,
            multiProfile),
        asyncResult -> {
            // If we succeeded we're all good here, the job is done either through approving, or redirect, or
            // forbidding
            // However, if an error occurred we need to handle this here
            if (asyncResult.failed()) {
                unexpectedFailure(routingContext, asyncResult.cause());
            } else {
                LOG.info("Authorised to view resource " + routingContext.request().path());
                routingContext.next();
            }
        });

    }


    protected void unexpectedFailure(final RoutingContext context, Throwable failure) {
        context.fail(toTechnicalException(failure));
    }

    protected final TechnicalException toTechnicalException(final Throwable t) {
        return (t instanceof TechnicalException) ? (TechnicalException) t : new TechnicalException(t);
    }

    @Override
    public void parseCredentials(RoutingContext routingContext, Handler<AsyncResult<Credentials>> handler) {

    }
}
