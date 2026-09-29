package org.pac4j.vertx.handler.impl;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import lombok.val;
import org.pac4j.core.adapter.FrameworkAdapter;
import org.pac4j.core.config.Config;
import org.pac4j.core.engine.SecurityGrantedAccessAdapter;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.vertx.VertxFrameworkParameters;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jUser;
import org.pac4j.vertx.context.session.VertxSessionStore;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class SecurityHandler implements AuthenticationHandler {

    private final Vertx vertx;
    private final VertxSessionStore sessionStore;
    private final Config config;
    private final SecurityHandlerOptions options;

    public SecurityHandler(final Vertx vertx,
                           final VertxSessionStore sessionStore,
                           final Config config,
                           final SecurityHandlerOptions options) {
        this.vertx = Objects.requireNonNull(vertx, "vertx");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.config = Objects.requireNonNull(config, "config");
        this.options = Objects.requireNonNull(options, "options");

        config.setSessionStoreFactoryIfUndefined(parameters -> sessionStore);
    }

    @Override
    public void handle(final RoutingContext ctx) {

        FrameworkAdapter.INSTANCE.applyDefaultSettingsIfUndefined(config);

        val securityLogic = config.getSecurityLogic();
        final AtomicBoolean accessGranted = new AtomicBoolean();

        final SecurityGrantedAccessAdapter granted = (context, store, profiles) -> {
            final Pac4jUser user = new Pac4jUser(profiles);

            ((VertxWebContext) context).setVertxUser(user);

            accessGranted.set(true);
            return null;
        };

        vertx.executeBlocking(() -> {
            securityLogic.perform(
                    config,
                    granted,
                    options.getClients(),
                    options.getAuthorizers(),
                    options.getMatchers(),
                    new VertxFrameworkParameters(ctx)
            );
            return accessGranted.get();
        }, false).onComplete(ar -> {
            if (ar.failed()) {
                ctx.fail(new TechnicalException(ar.cause()));
            } else if (ar.result()) {
                ctx.next();
            }
        });
    }
}
