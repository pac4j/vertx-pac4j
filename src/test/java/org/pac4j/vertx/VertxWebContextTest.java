package org.pac4j.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.pac4j.core.context.Cookie;

import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class VertxWebContextTest {

    @Test
    void returnsRemoteAddressWithoutPort() throws Exception {
        final Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1));
        try {
            final Router router = Router.router(vertx);
            router.route().handler(rc -> rc.response().end(new VertxWebContext(rc).getRemoteAddr()));
            final var server = vertx.createHttpServer().requestHandler(router).listen(0, "127.0.0.1")
                    .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
            final HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + server.actualPort() + "/"))
                    .timeout(Duration.ofSeconds(10)).build();
            final HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals("127.0.0.1", response.body());
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"Strict", "Lax", "None"})
    void preservesResponseCookieSameSite(String sameSite) throws Exception {
        final Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1));
        try {
            final Router router = Router.router(vertx);
            router.route().handler(rc -> {
                final Cookie cookie = new Cookie("test-cookie", "value");
                cookie.setPath("/");
                cookie.setSecure(true);
                cookie.setSameSitePolicy(sameSite);
                new VertxWebContext(rc).addResponseCookie(cookie);
                rc.response().end();
            });
            final var server = vertx.createHttpServer().requestHandler(router).listen(0, "127.0.0.1")
                    .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
            final HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + server.actualPort() + "/"))
                    .timeout(Duration.ofSeconds(10)).build();
            final HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            final String setCookie = response.headers().firstValue("Set-Cookie").orElseThrow();
            assertTrue(HttpCookie.parse(setCookie).get(0).getSecure(), setCookie);
            if (sameSite == null) {
                assertFalse(setCookie.toLowerCase(Locale.ROOT).contains("samesite="), setCookie);
            } else {
                assertTrue(java.util.Arrays.asList(setCookie.split(";\\s*")).contains("SameSite=" + sameSite),
                        setCookie);
            }
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 3600})
    void preservesResponseCookieMaxAge(int maxAge) throws Exception {
        final Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1));
        try {
            final Router router = Router.router(vertx);
            router.route().handler(rc -> {
                final Cookie cookie = new Cookie("test-cookie", "value");
                cookie.setPath("/");
                cookie.setMaxAge(maxAge);
                new VertxWebContext(rc).addResponseCookie(cookie);
                rc.response().end();
            });
            final var server = vertx.createHttpServer().requestHandler(router).listen(0, "127.0.0.1")
                    .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
            final HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + server.actualPort() + "/"))
                    .timeout(Duration.ofSeconds(10)).build();
            final HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            final String setCookie = response.headers().firstValue("Set-Cookie").orElseThrow();
            final HttpCookie cookie = HttpCookie.parse(setCookie).get(0);
            assertEquals("test-cookie", cookie.getName());
            assertEquals("value", cookie.getValue());
            assertEquals(maxAge, cookie.getMaxAge(), setCookie);
            if (maxAge == -1) {
                assertFalse(setCookie.toLowerCase(Locale.ROOT).contains("max-age="), setCookie);
                assertFalse(setCookie.toLowerCase(Locale.ROOT).contains("expires="), setCookie);
            }
        } finally {
            vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"authorization", "Authorization", "AUTHORIZATION"})
    @ResourceLock("java.util.Locale.default")
    void readsHeadersIgnoringCaseRegardlessOfDefaultLocale(String headerName) throws Exception {
        final Locale originalLocale = Locale.getDefault();
        final Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(1));
        try {
            // In Turkish, lowercasing an ASCII I without Locale.ROOT produces a dotless i.
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            final Router router = Router.router(vertx);
            router.route().handler(rc -> {
                final VertxWebContext context = new VertxWebContext(rc);
                final JsonObject headers = new JsonObject();
                for (String name : new String[]{"authorization", "Authorization", "AUTHORIZATION"}) {
                    headers.put(name, context.getRequestHeader(name).orElse(null));
                }
                headers.put("missing", context.getRequestHeader("X-Missing").isEmpty());
                rc.response().end(headers.encode());
            });
            final var server = vertx.createHttpServer().requestHandler(router).listen(0, "127.0.0.1")
                    .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
            final HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://127.0.0.1:" + server.actualPort() + "/"))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header(headerName, "Bearer CaseSensitiveToken")
                    .timeout(Duration.ofSeconds(10)).build();
            final HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            final JsonObject headers = new JsonObject(response.body());
            for (String name : new String[]{"authorization", "Authorization", "AUTHORIZATION"}) {
                assertEquals("Bearer CaseSensitiveToken", headers.getString(name), name);
            }
            assertTrue(headers.getBoolean("missing"));
        } finally {
            Locale.setDefault(originalLocale);
            vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }
}
