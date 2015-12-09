/*
  Copyright 2015 - 2015 pac4j organization

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.pac4j.vertx.cas;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(VertxClusteredSharedDataLogoutHandler.class);

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
                .subscribe(result -> sessionIdFuture.complete(result));
        try {
            return sessionIdFuture.get(blockingTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException|ExecutionException|TimeoutException e) {
            throw new TechnicalException(e);
        }
    }
}
