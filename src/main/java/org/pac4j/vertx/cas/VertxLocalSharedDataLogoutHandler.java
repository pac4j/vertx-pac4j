package org.pac4j.vertx.cas;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.sstore.SessionStore;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxLocalSharedDataLogoutHandler extends VertxSharedDataLogoutHandler {

    public VertxLocalSharedDataLogoutHandler(final Vertx vertx, final SessionStore sessionStore) {
        this(vertx, sessionStore, 1);
    }

    public VertxLocalSharedDataLogoutHandler(final Vertx vertx, final SessionStore sessionStore, final int blockingTimeoutSeconds) {
        super(vertx, sessionStore, blockingTimeoutSeconds);
    }

    @Override
    protected void doRecordSession(String sessionId, String ticket) {
        final LocalMap<String, String> localMap = vertx.sharedData().getLocalMap(PAC4J_CAS_SHARED_DATA_KEY);
        localMap.put(ticket, sessionId);
    }

    @Override
    protected void doDestroySession(String ticket) {
        final LocalMap<String, String> cache =vertx.sharedData().getLocalMap(PAC4J_CAS_SHARED_DATA_KEY);
        cache.remove(ticket);
    }

    @Override
    protected String getSessionId(String ticket) {
        return vertx.sharedData().<String, String>getLocalMap(PAC4J_CAS_SHARED_DATA_KEY).get(ticket);
    }
}
