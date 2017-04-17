package org.pac4j.vertx.core.store;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import org.pac4j.core.store.Store;

/**
 * Implementation of pac4j store based on vert.x LocalMap implementation. If the store is to be cluster-wide then
 * the clustered map implementation should be used instead.
 */
public class VertxLocalMapStore<K, V> implements Store<K, V> {

    public static final String PAC4J_SHARED_DATA_KEY = "pac4jSharedData";

    private final LocalMap<K, V> store;

    public VertxLocalMapStore(final Vertx vertx) {
        store = vertx.sharedData().getLocalMap(PAC4J_SHARED_DATA_KEY);
    }

    @Override
    public V get(K key) {
        return store.get(key);
    }

    @Override
    public void set(K key, V value) {
        store.put(key, value);
    }

    @Override
    public void remove(K key) {
        store.remove(key);
    }
}
