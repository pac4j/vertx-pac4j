package org.pac4j.vertx.handler.impl;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.pac4j.core.config.Config;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.VertxFrameworkParameters;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jUser;
import org.pac4j.vertx.context.session.VertxSessionStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SecurityHandlerTest {

    private Vertx vertx;
    private Router router;
    private VertxSessionStore sessionStore;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1));
        router = Router.router(vertx);
        sessionStore = new VertxSessionStore(LocalSessionStore.create(vertx));
    }

    @AfterEach
    void tearDown() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void runsBlockingLogicOnWorkerAndContinuesOnOriginalContext(boolean withProfile) throws Exception {
        final CommonProfile profile = new CommonProfile();
        profile.setId("alice");
        final List<UserProfile> profiles = withProfile ? List.of(profile) : List.of();
        final CompletableFuture<Context> originalContext = new CompletableFuture<>();
        router.route().handler(rc -> {
            originalContext.complete(Vertx.currentContext());
            rc.next();
        });
        addSecurityHandler((config, granted, clients, authorizers, matchers, parameters) -> {
            assertTrue(Context.isOnWorkerThread());
            final CompletableFuture<Void> eventLoopProgress = new CompletableFuture<>();
            vertx.runOnContext(ignored -> eventLoopProgress.complete(null));
            try {
                // This cannot complete if security logic blocks the single event loop.
                eventLoopProgress.get(5, TimeUnit.SECONDS);
                return granted.adapt(webContext(parameters), sessionStore, profiles);
            } catch (Exception e) {
                throw new TechnicalException(e);
            }
        });
        router.route().handler(rc -> {
            assertTrue(Context.isOnEventLoopThread());
            assertSame(originalContext.join(), Vertx.currentContext());
            assertEquals(profiles, ((Pac4jUser) rc.user()).profiles());
            rc.response().end("granted");
        });

        final HttpResponse<String> response = request();
        assertEquals(200, response.statusCode());
        assertEquals("granted", response.body());
    }

    @Test
    void doesNotContinueAfterRedirection() throws Exception {
        final AtomicBoolean continued = new AtomicBoolean();
        addSecurityHandler((config, granted, clients, authorizers, matchers, parameters) ->
                config.getHttpActionAdapter().adapt(new FoundAction("/login"), webContext(parameters)));
        router.route().handler(rc -> continued.set(true));

        final HttpResponse<String> response = request();
        assertEquals(302, response.statusCode());
        assertEquals("/login", response.headers().firstValue("Location").orElseThrow());
        awaitSecurityCompletion();
        assertFalse(continued.get());
    }

    @Test
    void propagatesFailureOnEventLoopWithoutContinuing() throws Exception {
        final IllegalStateException failure = new IllegalStateException("Authentication failed");
        final AtomicBoolean continued = new AtomicBoolean();
        addSecurityHandler((config, granted, clients, authorizers, matchers, parameters) -> {
            throw failure;
        });
        router.route().handler(rc -> continued.set(true));
        router.route().failureHandler(rc -> {
            assertTrue(Context.isOnEventLoopThread());
            assertInstanceOf(TechnicalException.class, rc.failure());
            assertSame(failure, rc.failure().getCause());
            rc.response().setStatusCode(500).end("authentication failure");
        });

        final HttpResponse<String> response = request();
        assertEquals(500, response.statusCode());
        assertEquals("authentication failure", response.body());
        assertFalse(continued.get());
    }

    private void addSecurityHandler(SecurityLogic logic) {
        final Config config = new Config();
        config.setSecurityLogic(logic);
        router.route().handler(new SecurityHandler(vertx, sessionStore, config, new SecurityHandlerOptions()));
    }

    private VertxWebContext webContext(org.pac4j.core.context.FrameworkParameters parameters) {
        final RoutingContext rc = ((VertxFrameworkParameters) parameters).routingContext();
        return new VertxWebContext(rc);
    }

    private HttpResponse<String> request() throws Exception {
        final var server = vertx.createHttpServer().requestHandler(router).listen(0, "127.0.0.1")
                .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        final HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.actualPort() + "/"))
                .timeout(Duration.ofSeconds(10)).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void awaitSecurityCompletion() throws Exception {
        final CompletableFuture<Void> completed = new CompletableFuture<>();
        vertx.setTimer(100, ignored -> completed.complete(null));
        completed.get(5, TimeUnit.SECONDS);
    }
}
