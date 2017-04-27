package org.pac4j.vertx.context.session;


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

    @Override
    public <T> T get(String key) {
        return vertxSession.get(key);
    }
}
