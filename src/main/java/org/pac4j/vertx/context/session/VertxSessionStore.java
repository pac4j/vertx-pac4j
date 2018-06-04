package org.pac4j.vertx.context.session;

import org.pac4j.context.session.ExtendedSessionStore;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.vertx.VertxWebContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Vert.x implementation of pac4j SessionStore interface to access the existing vertx-web session.
 *
 */
public class VertxSessionStore implements ExtendedSessionStore<VertxWebContext> {

    private final io.vertx.ext.web.sstore.SessionStore sessionStore;

    public VertxSessionStore(io.vertx.ext.web.sstore.SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public String getOrCreateSessionId(VertxWebContext context) {
        return context.getVertxSession().id();
    }

    @Override
    public Object get(VertxWebContext context, String key) {
        return context.getVertxSession().get(key);
    }

    @Override
    public void set(VertxWebContext context, String key, Object value) {
        context.getVertxSession().put(key, value);
    }

    @Override
    public boolean destroySession(VertxWebContext context) {
        context.getVertxSession().destroy();
        return true;
    }

    @Override
    public Object getTrackableSession(VertxWebContext context) {
        return null;
    }

    @Override
    public SessionStore<VertxWebContext> buildFromTrackableSession(VertxWebContext context, Object trackableSession) {
        return null;
    }

    @Override
    public boolean renewSession(VertxWebContext context) {
        context.getVertxSession().regenerateId();
        return true;
    }

    @Override
    public Session getSession(String sessionId) {
        final CompletableFuture<io.vertx.ext.web.Session> vertxSessionFuture = new CompletableFuture<>();
        sessionStore.get(sessionId, asyncResult -> {
            if (asyncResult.succeeded()) {
                vertxSessionFuture.complete(asyncResult.result());
            } else {
                vertxSessionFuture.completeExceptionally(asyncResult.cause());
            }
        });
        final CompletableFuture<Session> pac4jSessionFuture = vertxSessionFuture.thenApply(session -> {
            if (session != null) {
                return new VertxSession(session);
            } else {
                return null;
            }
        });
        try {
            return pac4jSessionFuture.get();
        } catch (InterruptedException|ExecutionException e) {
            throw new TechnicalException(e);
        }
    }
}
