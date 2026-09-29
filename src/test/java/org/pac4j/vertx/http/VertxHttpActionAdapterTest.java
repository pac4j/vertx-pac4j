package org.pac4j.vertx.http;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.Test;
import org.pac4j.core.exception.http.UnauthorizedAction;
import org.pac4j.vertx.VertxWebContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class VertxHttpActionAdapterTest {

    @Test
    void leavesErrorResponseToAsynchronousFailureHandler() throws Exception {
        final Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1));
        try {
            final Router router = Router.router(vertx);
            final CompletableFuture<Boolean> responseAlreadyEnded = new CompletableFuture<>();
            router.route().handler(rc -> {
                final UnauthorizedAction action = new UnauthorizedAction();
                action.setContent("pac4j error content");
                VertxHttpActionAdapter.INSTANCE.adapt(action, new VertxWebContext(rc));
            });
            router.route().failureHandler(rc -> vertx.runOnContext(ignored -> {
                // This runs after adapt() has returned, without relying on a timer.
                final boolean ended = rc.response().ended();
                responseAlreadyEnded.complete(ended);
                if (!ended) {
                    rc.response().setStatusCode(rc.statusCode())
                            .putHeader("WWW-Authenticate", "Bearer")
                            .end("custom authentication failure");
                }
            }));
            final var server = vertx.createHttpServer().requestHandler(router).listen(0, "127.0.0.1")
                    .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
            final HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + server.actualPort() + "/"))
                    .timeout(Duration.ofSeconds(10)).build();
            final HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(401, response.statusCode());
            assertEquals("custom authentication failure", response.body());
            assertEquals("Bearer", response.headers().firstValue("WWW-Authenticate").orElseThrow());
            assertFalse(responseAlreadyEnded.get(10, TimeUnit.SECONDS));
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }
}
