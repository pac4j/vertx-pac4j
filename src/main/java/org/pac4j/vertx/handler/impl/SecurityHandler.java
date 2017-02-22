package org.pac4j.vertx.handler.impl;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.http.HttpActionAdapter;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.http.DefaultHttpActionAdapter;

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

    protected final HttpActionAdapter<Void, VertxWebContext> httpActionAdapter = new DefaultHttpActionAdapter();

    private final SecurityLogic<Void, VertxWebContext> securityLogic;

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

        final DefaultSecurityLogic<Void, VertxWebContext> securityLogic = new DefaultSecurityLogic<>();
        securityLogic.setProfileManagerFactory(VertxProfileManager::new);
        this.securityLogic = securityLogic;
    }

    // Port of Pac4J auth to a handler in vert.x 3.
    @Override
    public void handle(final RoutingContext routingContext) {

        // No longer sufficient to check whether we have a user, as the user must have an appropriate pac4j profile
        // so a user != null check is insufficient, so we'll go straight to the pac4j logic check
        // Note that at present the security logic call is blocking (and authorization contained within can also
        // be blocking) so we have to wrap the following call in an executeBlocking call to avoid blocking the
        // event loop
        final VertxWebContext webContext = new VertxWebContext(routingContext, sessionStore);

        vertx.executeBlocking(future -> securityLogic.perform(webContext, config,
            (ctx, parameters) -> {
                // This is what should occur if we are authenticated and authorized to view the requested
                // resource
                LOG.info("Authorised to view resource " + routingContext.request().path());
                routingContext.next();
                return null;
            },
            httpActionAdapter,
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
            }
        });

    }


    protected void unexpectedFailure(final RoutingContext context, Throwable failure) {
        context.fail(toTechnicalException(failure));
    }

    protected final TechnicalException toTechnicalException(final Throwable t) {
        return (t instanceof TechnicalException) ? (TechnicalException) t : new TechnicalException(t);
    }

}
