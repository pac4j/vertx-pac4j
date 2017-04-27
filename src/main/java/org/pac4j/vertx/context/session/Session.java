package org.pac4j.vertx.context.session;

/**
 *
 */
public interface Session {

    void destroy(); // Destroy the whole session

    void set(String key, Object value); // Set the session value

    <T> T get(String key); // Retrieve a session value
}
