package org.pac4j.vertx.context.session;

/**
 *
 */
public interface Session {

    void destroy(); // Destroy the whole session

    void set(String userkey, Object value); // Set the session value
}
