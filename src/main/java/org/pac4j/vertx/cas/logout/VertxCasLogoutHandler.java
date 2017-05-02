package org.pac4j.vertx.cas.logout;

import org.pac4j.cas.logout.CasLogoutHandler;
import org.pac4j.context.session.ExtendedSessionStore;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.ExtendedProfileManager;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.store.Store;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.context.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 *
 */
public class VertxCasLogoutHandler implements CasLogoutHandler<VertxWebContext> {

    public static final String PAC4J_CAS_TICKET = "pac4jCasTicket";

    protected static final Logger logger = LoggerFactory.getLogger(VertxCasLogoutHandler.class);

    private final Store<String, Object> store;
    private final boolean destroySession;

    private final Function<VertxWebContext, ProfileManager> profileManagerFactory;


    public VertxCasLogoutHandler(final Store<String, Object> store, final boolean destroySession) {
        this(store, destroySession, webContext -> new ProfileManager(webContext));
    }

    public VertxCasLogoutHandler(final Store<String, Object> store, final boolean destroySession, Function<VertxWebContext, ProfileManager> profileManagerFactory) {
        this.store = store;
        this.destroySession = destroySession;
        this.profileManagerFactory = profileManagerFactory;
    }

    @Override
    public void recordSession(final VertxWebContext context, final String ticket) {
        // Record session connection as per the existing cas behaviour
        final SessionStore sessionStore = context.getSessionStore();
        if (sessionStore == null) {
            logger.error("No session store available for this web context");
        } else {

            final String sessionId = sessionStore.getOrCreateSessionId(context);
            if (sessionId != null) {
                logger.debug("ticket: {} -> sessionId: {}", ticket, sessionId);
                store.set(ticket, sessionId);
                context.setSessionAttribute(PAC4J_CAS_TICKET, ticket); // Gives us a two-way link
            } else {
                logger.debug("Can not identify id for current session");
            }
        }
    }

    @Override
    public void destroySessionFront(VertxWebContext context, String ticket) {
        store.remove(ticket);

        final SessionStore sessionStore = context.getSessionStore();
        if (sessionStore == null) {
            logger.error("No session store available for this web context");
        } else {
            final String currentSessionId = sessionStore.getOrCreateSessionId(context);
            logger.debug("currentSessionId: {}", currentSessionId);
            final String sessionToTicket = (String) sessionStore.get(context, PAC4J_CAS_TICKET);
            logger.debug("-> ticket: {}", ticket);
            sessionStore.set(context, PAC4J_CAS_TICKET, null);

            if (CommonHelper.areEquals(ticket, sessionToTicket)) {

                // remove profiles
                final ProfileManager manager = profileManagerFactory.apply(context);
                manager.logout();
                logger.debug("destroy the user profiles");
                // and optionally the web session
                if (destroySession) {
                    logger.debug("destroy the whole session");
                    final boolean invalidated = sessionStore.destroySession(context);
                    if (!invalidated) {
                        logger.error("The session has not been invalidated for front channel logout");
                    }
                }
            } else {
                logger.error("The user profiles (and session) can not be destroyed for CAS front channel logout because the provided ticket is not the same as the one linked to the current session");
            }
        }
    }

    @Override
    public void destroySessionBack(final VertxWebContext context, final String ticket) {
        // Use the ticket to determine a session id
        final String sessionId = (String) store.get(ticket);
        // Take the session id and use it to retrieve a session
        // Log the user out of the session - need a profile manager extension to handle this
        // Destroy the session if required
        logger.debug("ticket: {} -> trackableSession: {}", ticket, sessionId);
        if (sessionId == null) {
            logger.error("No session found for back channel logout. Possibly it has expired from the store and the store settings must be updated (expired data)");
        } else {
            store.remove(ticket);

            // renew context with the original session store
            final SessionStore sessionStore = context.getSessionStore();
            if (sessionStore == null || !(sessionStore instanceof ExtendedSessionStore)) {
                logger.error("No extended session store available for this web context");
            } else {
                final ExtendedSessionStore extendedSessionStore = (ExtendedSessionStore) sessionStore;
                final Session session = extendedSessionStore.getSession(sessionId);
                if (session != null) {
                    logger.debug("session: {}", session);
                    final ProfileManager manager = profileManagerFactory.apply(context);
                    if (!(manager instanceof ExtendedProfileManager)) {
                        logger.error("Profile manager not capable of back-channel logout");
                    } else {
                        final ExtendedProfileManager extendedProfileManager = (ExtendedProfileManager)manager;
                        extendedProfileManager.removeFromSession(session);
                    }
                    logger.debug("remove sessionId from session: {}", sessionId);
                    session.remove(PAC4J_CAS_TICKET);
                    if (destroySession) {
                        logger.debug("destroy the whole session");
                        session.destroy();
                    }
                } else {
                    logger.error("Session not found for session id {}", sessionId);
                }
            }
        }

    }

    @Override
    public void renewSession(String oldSessionId, VertxWebContext context) {
        final String ticket = (String) context.getSessionAttribute(PAC4J_CAS_TICKET);
        logger.debug("oldSessionId: {} -> ticket: {}", oldSessionId, ticket);
        final SessionStore sessionStore = context.getSessionStore();
        if (!(sessionStore instanceof ExtendedSessionStore)) {
            logger.error("Session store does not support session renewal");
        } else {
            if (ticket != null) {
                store.remove(ticket);
                final ExtendedSessionStore extendedSessionStore = (ExtendedSessionStore) sessionStore;
                final Session session = extendedSessionStore.getSession(oldSessionId);
                if (session != null) { // Switch to use optional
                    session.set(PAC4J_CAS_TICKET, null);
                }
                recordSession(context, ticket);
            }
        }
    }
}
