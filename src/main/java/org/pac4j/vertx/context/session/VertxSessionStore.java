package org.pac4j.vertx.context.session;

import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.vertx.VertxWebContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import io.vertx.ext.web.Session;

/**
 * Vert.x implementation of pac4j SessionStore interface to access the existing vertx-web session.
 *
 */
public class VertxSessionStore implements SessionStore<VertxWebContext> {

    private final io.vertx.ext.web.sstore.SessionStore sessionStore;

    private final Session providedSession;

    public VertxSessionStore(final io.vertx.ext.web.sstore.SessionStore sessionStore) {
        this(sessionStore, null);
    }

    public VertxSessionStore(final io.vertx.ext.web.sstore.SessionStore sessionStore, final Session providedSession) {
        this.sessionStore = sessionStore;
        this.providedSession = providedSession;
    }

    protected Session getVertxSession(final VertxWebContext context) {
        if (providedSession != null) {
            return providedSession;
        } else {
            return context.getVertxSession();
        }
    }

    @Override
    public String getOrCreateSessionId(final VertxWebContext context) {
        final Session vertxSession = getVertxSession(context);
        if (vertxSession != null) {
            return getVertxSession(context).id();
        }
        return null;
    }

    @Override
    public Optional<Object> get(final VertxWebContext context, final String key) {
        final Session vertxSession = getVertxSession(context);
        if (vertxSession != null) {
            return Optional.ofNullable(vertxSession.get(key));
        }
        return Optional.empty();
    }

    @Override
    public void set(final VertxWebContext context, final String key, final Object value) {
        final Session vertxSession = getVertxSession(context);
        if (vertxSession != null) {
            vertxSession.put(key, value);
        }
    }

    @Override
    public boolean destroySession(final VertxWebContext context) {
        final Session vertxSession = getVertxSession(context);
        if (vertxSession != null) {
            vertxSession.destroy();
            return true;
        }
        return false;
    }

    @Override
    public Optional<Object> getTrackableSession(final VertxWebContext context) {
        final Session vertxSession = getVertxSession(context);
        if (vertxSession != null) {
            return Optional.of(getVertxSession(context).id());
        }
        return Optional.empty();
    }

    @Override
    public Optional<SessionStore<VertxWebContext>> buildFromTrackableSession(final VertxWebContext context, final Object trackableSession) {
        if (trackableSession != null) {
            final CompletableFuture<io.vertx.ext.web.Session> vertxSessionFuture = new CompletableFuture<>();
            sessionStore.get((String) trackableSession, asyncResult -> {
                if (asyncResult.succeeded()) {
                    vertxSessionFuture.complete(asyncResult.result());
                } else {
                    vertxSessionFuture.completeExceptionally(asyncResult.cause());
                }
            });
            final CompletableFuture<VertxSessionStore> pac4jSessionFuture = vertxSessionFuture.thenApply(session -> {
                if (session != null) {
                    return new VertxSessionStore(sessionStore, session);
                } else {
                    return null;
                }
            });
            try {
                return Optional.ofNullable(pac4jSessionFuture.get());
            } catch (final InterruptedException|ExecutionException e) {
                throw new TechnicalException(e);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean renewSession(VertxWebContext context) {
        final Session vertxSession = getVertxSession(context);
        if (vertxSession != null) {
            vertxSession.regenerateId();
            return true;
        }
        return false;
    }
}
