package org.pac4j.context.session;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.vertx.context.session.Session;

/**
 *
 */
public interface ExtendedSessionStore<C extends WebContext> extends SessionStore<C> {

//    void logoutUser(final String sessionId, final boolean destroySession);
    Session getSession(final String sessionId);

}
