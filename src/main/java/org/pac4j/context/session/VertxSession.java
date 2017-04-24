package org.pac4j.context.session;


import org.pac4j.vertx.context.session.Session;

/**
 *
 */
public class VertxSession implements Session {

    private final io.vertx.ext.web.Session vertxSession;

    public VertxSession(final io.vertx.ext.web.Session vertxSession) {
        this.vertxSession = vertxSession;
    }

    @Override
    public void destroy() {
        vertxSession.destroy();
    }

    @Override
    public void set(String key, Object value) {
        vertxSession.put(key, value);
    }
}
