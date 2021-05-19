package org.pac4j.vertx.core.store;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.shareddata.AsyncMap;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.store.Store;
import rx.Single;
import rx.functions.Func1;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Pac4j shared store implementation based on vert.x clustered shared data.
 */
public class VertxClusteredMapStore<K, V> extends VertxMapStoreBase implements Store<K, V> {

    private final Vertx rxVertx;
    private final int blockingTimeoutSeconds;

    public VertxClusteredMapStore(final io.vertx.core.Vertx vertx) {
        this(vertx, 1);
    }

    public VertxClusteredMapStore(final io.vertx.core.Vertx vertx, final int timeoutSeconds) {
        rxVertx = Vertx.newInstance(vertx);
        blockingTimeoutSeconds = timeoutSeconds;
    }

    @Override
    public Optional<V> get(K key) {
        voidAsyncOpToBlocking(map -> map.rxGet(key));

        final CompletableFuture<V> valueFuture = new CompletableFuture<>();
        rxVertx.sharedData().<K, V>rxGetClusterWideMap(PAC4J_SHARED_DATA_KEY)
                .flatMap(map -> map.rxGet(key))
                .subscribe(valueFuture::complete);
        try {
            return Optional.ofNullable(valueFuture.get(blockingTimeoutSeconds, TimeUnit.SECONDS));
        } catch (InterruptedException|ExecutionException|TimeoutException e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public void set(K key, V value) {
        voidAsyncOpToBlocking(map -> map.rxPut(key, value));
    }

    @Override
    public void remove(K key) {
        voidAsyncOpToBlocking(map -> map.rxRemove(key));
    }

    public void voidAsyncOpToBlocking(Func1<AsyncMap, Single> asyncOp) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        rxVertx.sharedData().rxGetAsyncMap(PAC4J_SHARED_DATA_KEY)
                .map(asyncOp)
                .subscribe(result -> future.complete(null));

        try {
            future.get(blockingTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException|ExecutionException |TimeoutException e) {
            throw new TechnicalException(e);
        }
    }
}
