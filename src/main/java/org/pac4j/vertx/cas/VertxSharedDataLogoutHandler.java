package org.pac4j.vertx.cas;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.web.sstore.SessionStore;
import org.apache.commons.lang3.StringUtils;
import org.pac4j.cas.logout.NoLogoutHandler;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.TechnicalException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public abstract class VertxSharedDataLogoutHandler extends NoLogoutHandler {

    private static final Logger LOG = LoggerFactory.getLogger(VertxSharedDataLogoutHandler.class);
    private static final String SESSION_USER_HOLDER_KEY = "__vertx.userHolder"; // Where vert.x stores the current vert.x user - we need to be able to clear this out

    public static final String PAC4J_CAS_SHARED_DATA_KEY = "pac4jCasSharedData";
    // Mimic https://github.com/pac4j/play-pac4j/blob/master/play-pac4j-java/src/main/java/org/pac4j/play/cas/logout/PlayCacheLogoutHandler.java
    // With Shared data replacing cache and vertx session store as the main session info
    protected final Vertx vertx;
    private final SessionStore sessionStore;
    protected final int blockingTimeoutSeconds;

    public VertxSharedDataLogoutHandler(final Vertx vertx, final io.vertx.ext.web.sstore.SessionStore sessionStore, int blockingTimeoutSeconds) {
        this.vertx = vertx;
        this.blockingTimeoutSeconds = blockingTimeoutSeconds;
        this.sessionStore = SessionStore.newInstance(sessionStore);
    }

    @Override
    public void recordSession(WebContext context, String ticket) {

        String sessionId = (String) context.getSessionIdentifier();
        doRecordSession(sessionId, ticket);

    }

    @Override
    public void destroySession(WebContext context) {
        final String logoutRequest = context.getRequestParameter("logoutRequest");
        LOG.debug("logoutRequest: {}", logoutRequest);
        final String ticket = StringUtils.substringBetween(logoutRequest, "SessionIndex>", "</");
        LOG.debug("extract ticket: {}", ticket);
        // get the session id first, then remove the pac4j profile from that session
// TODO:        ALSO MODIFY TO REMOVE VERTX USER
        final CompletableFuture<Void> userLogoutFuture = new CompletableFuture<>();
        final String sessionId = getSessionId(ticket);
        sessionStore.getObservable(sessionId)
                .map(session -> session.remove(SESSION_USER_HOLDER_KEY))
                .doOnError(Throwable::printStackTrace)
                .subscribe(s -> userLogoutFuture.complete(null));
        try {
            userLogoutFuture.get(blockingTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException|ExecutionException|TimeoutException e) {
            userLogoutFuture.completeExceptionally(new TechnicalException(e));
        }

        doDestroySession(ticket);
    }

    protected abstract void doRecordSession(final String sessionId, final String ticket);
    protected abstract void doDestroySession(final String ticket);
    protected abstract String getSessionId(final String ticket);

}
