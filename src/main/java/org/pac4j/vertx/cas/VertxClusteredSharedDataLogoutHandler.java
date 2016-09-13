package org.pac4j.vertx.cas;

import io.vertx.rxjava.core.Vertx;
import org.pac4j.core.exception.TechnicalException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxClusteredSharedDataLogoutHandler extends VertxSharedDataLogoutHandler {

    private final Vertx rxVertx;

    public VertxClusteredSharedDataLogoutHandler(final io.vertx.core.Vertx vertx, final io.vertx.ext.web.sstore.SessionStore sessionStore) {
        this(vertx, sessionStore, 1);
    }

    public VertxClusteredSharedDataLogoutHandler(final io.vertx.core.Vertx vertx, final io.vertx.ext.web.sstore.SessionStore sessionStore, final int timeoutSeconds) {
        super(vertx, sessionStore, timeoutSeconds);
        rxVertx = Vertx.newInstance(vertx);
    }

    @Override
    protected void doRecordSession(String sessionId, String ticket) {

        CompletableFuture<Void> future = new CompletableFuture<>();

        rxVertx.sharedData().getClusterWideMapObservable(PAC4J_CAS_SHARED_DATA_KEY)
                .map(map -> map.putObservable(ticket, sessionId))
                .subscribe(result -> future.complete(null));

        try {
            future.get(blockingTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException|ExecutionException|TimeoutException e) {
            future.completeExceptionally(new TechnicalException(e));
        }

    }

    @Override
    protected void doDestroySession(String ticket) {

        CompletableFuture<Void> future = new CompletableFuture<>();

        rxVertx.sharedData().<String, String>getClusterWideMapObservable(PAC4J_CAS_SHARED_DATA_KEY)
                .map(map -> map.removeObservable(ticket))
                .subscribe(result -> future.complete(null));

        try {
            future.get(blockingTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException|ExecutionException|TimeoutException e) {
            future.completeExceptionally(new TechnicalException(e));
        }
    }

    @Override
    protected String getSessionId(String ticket) {
        final CompletableFuture<String> sessionIdFuture = new CompletableFuture<>();
        rxVertx.sharedData().<String, String>getClusterWideMapObservable(PAC4J_CAS_SHARED_DATA_KEY)
                .flatMap(map -> map.getObservable(ticket))
                .subscribe(sessionIdFuture::complete);
        try {
            return sessionIdFuture.get(blockingTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException|ExecutionException|TimeoutException e) {
            throw new TechnicalException(e);
        }
    }
}
