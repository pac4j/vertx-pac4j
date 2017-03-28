package org.pac4j.vertx.context.session;

import org.pac4j.core.context.session.SessionStore;
import org.pac4j.vertx.VertxWebContext;

/**
 * Vert.x implementation of pac4j SessionStore interface to access the existing vertx-web session.
 *
 */
public class VertxSessionStore implements SessionStore<VertxWebContext> {
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
        return false;
    }


}
