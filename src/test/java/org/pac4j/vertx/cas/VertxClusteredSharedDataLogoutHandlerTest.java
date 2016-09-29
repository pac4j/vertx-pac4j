package org.pac4j.vertx.cas;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.junit.Test;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import rx.Observable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxClusteredSharedDataLogoutHandlerTest extends VertxSharedDataLogoutHandlerTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(VertxClusteredSharedDataLogoutHandlerTest.class);

    @Test
    public void testRecordSessionClustered() throws Exception {

        final VertxOptions options = new VertxOptions().setClustered(true);

        final CompletableFuture<String> expectedSessionIdFuture = new CompletableFuture<>();
        final CompletableFuture<String> actualSessionIdFuture = new CompletableFuture<>();

        // Next chunk all happens on the event bus, we'll wait for the futures on the test thread
        Vertx.clusteredVertx(options, asyncResult -> {
            if (asyncResult.succeeded()) {
                final Vertx clusteredVertx = asyncResult.result();
                final SessionStore sessionStore = LocalSessionStore.create(clusteredVertx);
                clusteredVertx.<String>executeBlocking(future -> {
                            String expectedSessionId = null;
                            try {
                                expectedSessionId = recordSession(new VertxClusteredSharedDataLogoutHandler(clusteredVertx, sessionStore), sessionStore);
                                future.complete(expectedSessionId);
                            } catch (Exception e) {
                               future.fail(e);
                            }
                        },
                        false,
                        recordSessionResult -> {
                            if (recordSessionResult.succeeded()) {
                                expectedSessionIdFuture.complete(recordSessionResult.result());
                                getSessionIdFromClusteredData(clusteredVertx, actualSessionIdFuture);

                            }
                        });

            }

        });

        // Now wait on test thread for both futures - we'll set a max of 5 seconds, no need to use
        // the basic test wait mechanism
        String expectedSessionId = expectedSessionIdFuture.get(5, TimeUnit.SECONDS);
        String actualSessionId = actualSessionIdFuture.get(5, TimeUnit.SECONDS);
        assertThat(actualSessionId, is(expectedSessionId));

    }

    @Test
    public void testRecordSessionRx() throws Exception {

        final io.vertx.rxjava.core.Vertx clusteredVertx = getClusteredVertx();
        final Observable<String> expectedSessionIdObservable = clusteredVertx
                .executeBlockingObservable(future -> {
                    final Vertx delegate = (Vertx) clusteredVertx.getDelegate();
                    final SessionStore sessionStore = LocalSessionStore.create(delegate);
                    final String expectedSessionId;
                    try {
                        expectedSessionId = recordSession(
                                new VertxClusteredSharedDataLogoutHandler(delegate, sessionStore),
                                sessionStore);
                        future.complete(expectedSessionId);
                    } catch (Exception e) {
                        future.fail(e.getMessage());
                    }
                }, false);

        final CompletableFuture<String> expectedSessionIdFuture = new CompletableFuture<>();
        expectedSessionIdObservable.subscribe(expectedSessionIdFuture::complete);
        final String expectedSessionId = expectedSessionIdFuture.get(2, TimeUnit.SECONDS);

        // Now we know record session has happened we should check the session id from the shared data cluster, we need
        // to ensure record has happened before we do this
        final String actualSessionId = getFromAsyncMap(clusteredVertx, TEST_TICKET);
        assertThat(actualSessionId, is(expectedSessionId));

    }

    @Test
    public void testDestroySessionRx() throws Exception {

        final io.vertx.rxjava.core.Vertx clusteredVertx = getClusteredVertx();
        final Vertx delegate = (Vertx) clusteredVertx.getDelegate();
        final SessionStore sessionStore = LocalSessionStore.create(delegate);
        final Session session = getSession(sessionStore);
        final VertxWebContext webContext = dummyWebContext(session);
        simulateLogin(webContext);
        final VertxClusteredSharedDataLogoutHandler sharedDataLogoutHandler = new VertxClusteredSharedDataLogoutHandler(delegate, sessionStore);
        final CompletableFuture<Void> putSessionIdFuture = new CompletableFuture<>();

        clusteredVertx.sharedData()
                .<String, String>getClusterWideMapObservable(VertxSharedDataLogoutHandler.PAC4J_CAS_SHARED_DATA_KEY)
                .flatMap(asyncMap -> asyncMap.putObservable(TEST_TICKET, session.id()))
                .subscribe(putSessionIdFuture::complete);

        putSessionIdFuture.get(1, TimeUnit.SECONDS); // Wait here until setup is complete or we timeout
        assertThat(webContext.getVertxUser(), is(notNullValue()));

        // Now our starting state should be in place so now to destroy the session and check results
        final CompletableFuture<Void> sessionDestructionFuture = new CompletableFuture<>();
        clusteredVertx.<Void>executeBlockingObservable(future -> {
            sharedDataLogoutHandler.destroySession(webContext);
            future.complete(null);
        }, false)
        .doOnError(sessionDestructionFuture::completeExceptionally)
        .subscribe(sessionDestructionFuture::complete);
        sessionDestructionFuture.get(1, TimeUnit.SECONDS); // Wait till we think session destruction is complete or
                                                           // we timeout

        // Now check final state
        final String actualSessionId = getFromAsyncMap(clusteredVertx, TEST_TICKET);
        assertThat(actualSessionId, is(nullValue()));
        final UserProfile profileFromSession = new VertxProfileManager(webContext).get(true).orElse(null);
        assertThat(profileFromSession, is(nullValue()));

    }

    private String getFromAsyncMap(io.vertx.rxjava.core.Vertx clusteredVertx, String key) throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
        final Observable<String> actualSessionIdObservable = clusteredVertx.sharedData()
                .<String, String>getClusterWideMapObservable(VertxSharedDataLogoutHandler.PAC4J_CAS_SHARED_DATA_KEY)
                .flatMap(asyncMap -> asyncMap.getObservable(key));
        final CompletableFuture<String> actualSessionIdFuture = new CompletableFuture<>();
        actualSessionIdObservable.subscribe(actualSessionIdFuture::complete);
        return actualSessionIdFuture.get(2, TimeUnit.SECONDS);
    }

    private io.vertx.rxjava.core.Vertx getClusteredVertx() throws Exception{

        final VertxOptions options = new VertxOptions().setClustered(true);
        final CompletableFuture<io.vertx.rxjava.core.Vertx> vertxFuture = new CompletableFuture<>();
        io.vertx.rxjava.core.Vertx.clusteredVertxObservable(options).subscribe(vertxFuture::complete);
        return vertxFuture.get(2, TimeUnit.SECONDS);
    }


    private static void getSessionIdFromClusteredData(final Vertx clusteredVertx, final CompletableFuture<String> sessionIdFuture) {
        clusteredVertx.sharedData().<String, String>getClusterWideMap(VertxSharedDataLogoutHandler.PAC4J_CAS_SHARED_DATA_KEY,
                getMapResult -> {
                    if (getMapResult.succeeded()) {
                        AsyncMap<String, String> asyncMap = getMapResult.result();
                        asyncMap.get(TEST_TICKET, actualSessionIdResult -> {
                            if (actualSessionIdResult.succeeded()) {
                                sessionIdFuture.complete(actualSessionIdResult.result());
                            }
                        });
                    }
                });
    }


}
