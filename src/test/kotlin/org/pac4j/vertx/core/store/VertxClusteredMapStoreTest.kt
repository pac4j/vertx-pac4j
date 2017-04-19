package org.pac4j.vertx.core.store

import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import org.pac4j.core.store.Store
import java.util.concurrent.CompletableFuture

/**
 * Test for VertxClusteredMap, note that this will also need some integration tests to check threading behaviours
 */
class VertxClusteredMapStoreTest: VertxStoreTestBase() {

    override val store: Store<String, String> = createStore()

    fun createStore(): Store<String, String> {
                val setupFuture: CompletableFuture<VertxClusteredMapStore<String, String>> = CompletableFuture()
        Vertx.clusteredVertx(VertxOptions().setClustered(true), { result ->
            if (result.succeeded()) {
                setupFuture.complete(VertxClusteredMapStore(result.result()))
            } else {
                setupFuture.completeExceptionally(result.cause())
            }
        })
        return setupFuture.join()
    }

}