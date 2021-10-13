package org.pac4j.vertx.context.session;

import io.vertx.ext.web.Session;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.core.util.serializer.JavaSerializer;
import org.pac4j.vertx.VertxWebContext;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Vert.x implementation of pac4j SessionStore interface to access the existing vertx-web session.
 *
 */
public class VertxSessionStore implements SessionStore {

    private final io.vertx.ext.web.sstore.SessionStore sessionStore;
    private static final JavaSerializer JAVA_SERIALIZER = new JavaSerializer();

    private final Session providedSession;

    public VertxSessionStore(final io.vertx.ext.web.sstore.SessionStore sessionStore) {
        this(sessionStore, null);
    }

    public VertxSessionStore(final io.vertx.ext.web.sstore.SessionStore sessionStore, final Session providedSession) {
        this.sessionStore = sessionStore;
        this.providedSession = providedSession;
    }

    protected Session getVertxSession(final WebContext context) {
        if (providedSession != null) {
            return providedSession;
        } else {
            return ((VertxWebContext)context).getVertxSession();
        }
    }

    @Override
    public Optional<String> getSessionId(WebContext context, boolean b) {
        final Session vertxSession = getVertxSession(context);
        if (vertxSession != null) {
            return Optional.of(getVertxSession(context).id());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Object> get(final WebContext context, final String key) {
        final Session vertxSession = getVertxSession(context);
        if (vertxSession != null) {
            if (key.equals(Pac4jConstants.USER_PROFILES)) {
                var inputBytes = Base64.getDecoder().decode((String)vertxSession.get(key));
                return Optional.ofNullable(JAVA_SERIALIZER.deserializeFromBytes(inputBytes));
            }
            return Optional.ofNullable(vertxSession.get(key));
        }
        return Optional.empty();
    }

    @Override
    public void set(final WebContext context, final String key, final Object value) {
        final Session vertxSession = getVertxSession(context);

        if (vertxSession != null) {
            if (key.equals(Pac4jConstants.USER_PROFILES)) {
                vertxSession.put(key, Base64.getEncoder().encodeToString(JAVA_SERIALIZER.serializeToBytes(value)));
            } else {
                vertxSession.put(key, value);
            }
        }
    }

    @Override
    public boolean destroySession(final WebContext context) {
        final Session vertxSession = getVertxSession(context);
        if (vertxSession != null) {
            vertxSession.destroy();
            return true;
        }
        return false;
    }

    @Override
    public Optional<Object> getTrackableSession(final WebContext context) {
        final Session vertxSession = getVertxSession(context);
        if (vertxSession != null) {
            return Optional.of(getVertxSession(context).id());
        }
        return Optional.empty();
    }

    @Override
    public Optional<SessionStore> buildFromTrackableSession(final WebContext context, final Object trackableSession) {
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
    public boolean renewSession(WebContext context) {
        final Session vertxSession = getVertxSession(context);
        if (vertxSession != null) {
            vertxSession.regenerateId();
            return true;
        }
        return false;
    }
}
