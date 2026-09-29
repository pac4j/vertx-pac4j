package org.pac4j.vertx.cas.logout;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.spi.cluster.hazelcast.ConfigUtil;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.cas.config.CasProtocol;
import org.pac4j.core.config.Config;
import org.pac4j.core.logout.handler.DefaultSessionLogoutHandler;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.context.session.VertxSessionStore;
import org.pac4j.vertx.core.store.VertxClusteredMapStore;
import org.pac4j.vertx.handler.impl.CallbackHandler;
import org.pac4j.vertx.handler.impl.CallbackHandlerOptions;

import java.io.ByteArrayOutputStream;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ports the removed VertxCasLogoutHandlerIntegrationTest to the current CAS callback
 * and DefaultSessionLogoutHandler APIs. Both sessions and SSO mappings are shared
 * by two real Hazelcast members; ticket validation is served locally over HTTP.
 * Runs with the standard test suite: mvn test
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class CasLogoutIntegrationTest {

    private static final String CLIENT = "CasClient";
    private static final String USER = "alice";
    private final List<Vertx> cluster = new ArrayList<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private Node first;
    private Node second;

    @BeforeAll
    void startCluster() throws Exception {
        final String clusterName = "vertx-pac4j-it-" + UUID.randomUUID();
        final int firstPort;
        final int secondPort;
        try (ServerSocket a = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             ServerSocket b = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            firstPort = a.getLocalPort();
            secondPort = b.getLocalPort();
        }
        try {
            final Vertx a = startMember(clusterName, firstPort, firstPort);
            final Vertx b = startMember(clusterName, secondPort, firstPort);
            final var casServer = await(a.createHttpServer().requestHandler(request -> {
                if ("/cas/serviceValidate".equals(request.path()) && request.getParam("ticket") != null) {
                    request.response().putHeader("Content-Type", "application/xml").end("""
                            <cas:serviceResponse xmlns:cas="http://www.yale.edu/tp/cas">
                              <cas:authenticationSuccess><cas:user>alice</cas:user></cas:authenticationSuccess>
                            </cas:serviceResponse>
                            """);
                } else {
                    request.response().setStatusCode(404).end();
                }
            }).listen(0, "127.0.0.1"));
            final String casUrl = "http://127.0.0.1:" + casServer.actualPort() + "/cas";
            first = startApplication(a, casUrl);
            second = startApplication(b, casUrl);
        } catch (Exception | Error e) {
            stopCluster();
            throw e;
        }
    }

    private Vertx startMember(String clusterName, int port, int seedPort) throws Exception {
        final var hazelcast = ConfigUtil.loadConfig();
        hazelcast.setClusterName(clusterName);
        hazelcast.setProperty("hazelcast.phone.home.enabled", "false");
        hazelcast.setProperty("hazelcast.operation.thread.count", "2");
        hazelcast.setProperty("hazelcast.operation.generic.thread.count", "2");
        hazelcast.getNetworkConfig().setPort(port).setPortAutoIncrement(false);
        hazelcast.getNetworkConfig().getInterfaces().setEnabled(true).addInterface("127.0.0.1");
        hazelcast.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        hazelcast.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);
        hazelcast.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true)
                .setMembers(List.of("127.0.0.1:" + seedPort));
        final var manager = new HazelcastClusterManager(hazelcast);
        final Vertx vertx = await(Vertx.builder().with(new VertxOptions()
                        .setEventLoopPoolSize(2).setWorkerPoolSize(4)
                        .setEventBusOptions(new EventBusOptions().setHost("127.0.0.1").setPort(0)))
                .withClusterManager(manager).buildClustered());
        cluster.add(vertx);
        assertEquals(cluster.size(), manager.getHazelcastInstance().getCluster().getMembers().size());
        return vertx;
    }

    private Node startApplication(Vertx vertx, String casUrl) throws Exception {
        final var sessions = ClusteredSessionStore.create(vertx, "vertx-web.sessions", 1000);
        final var sessionStore = new VertxSessionStore(sessions);
        final var mappings = new VertxClusteredMapStore<String, Object>(vertx, 10);
        final var logout = new DefaultSessionLogoutHandler(mappings);
        final var cas = new CasConfiguration(casUrl + "/login", CasProtocol.CAS20);
        final Config config = new Config("/callback", new CasClient(cas));
        config.setSessionLogoutHandler(logout);
        config.setSessionStoreFactory(parameters -> sessionStore);

        final Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(SessionHandler.create(sessions).setSessionTimeout(120_000));
        router.get("/cart").handler(rc -> {
            rc.session().put("cart", "item-1");
            rc.response().end();
        });
        router.route("/callback").handler(new CallbackHandler(vertx, sessionStore, config,
                new CallbackHandlerOptions().setDefaultUrl("/state")));
        router.get("/state").handler(rc -> {
            final var profiles = new ProfileManager(new VertxWebContext(rc), sessionStore).getProfiles();
            rc.response().putHeader("Content-Type", "application/json").end(new JsonObject()
                    .put("sessionId", rc.session().id())
                    .put("user", profiles.isEmpty() ? null : profiles.get(0).getId())
                    .put("cart", rc.session().get("cart")).encode());
        });
        router.route().failureHandler(rc -> rc.response().setStatusCode(500)
                .end(String.valueOf(rc.failure())));
        final var server = await(vertx.createHttpServer().requestHandler(router).listen(0, "127.0.0.1"));
        return new Node(vertx, sessions, mappings, logout, "http://127.0.0.1:" + server.actualPort());
    }

    @AfterAll
    void stopCluster() throws Exception {
        final var closed = cluster.stream().map(v -> v.close().toCompletionStage().toCompletableFuture())
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(closed).get(60, TimeUnit.SECONDS);
        cluster.clear();
    }

    @Test
    void recordsSessionAndMappingsAcrossMembers() throws Exception {
        final Login login = login();
        final JsonObject state = state(second, login.cookie());
        assertEquals(USER, state.getString("user"));
        assertEquals(login.sessionId(), state.getString("sessionId"));
        assertEquals("item-1", state.getString("cart"));
        assertEquals(login.sessionId(), await(second.vertx().executeBlocking(
                () -> second.mappings().get(login.ticket()).orElse(null), false)));
        assertEquals(login.ticket(), await(second.vertx().executeBlocking(
                () -> second.mappings().get(login.sessionId()).orElse(null), false)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void frontChannelLogoutIsPersisted(boolean destroySession) throws Exception {
        final Login login = login();
        second.logout().setDestroySession(destroySession);
        final var response = send(second, "/callback?client_name=" + CLIENT + "&logoutRequest="
                + encode(compressedLogoutRequest(login.ticket())), login.cookie(), null);
        assertEquals(200, response.statusCode(), response.body());
        assertLoggedOut(login, destroySession);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void backChannelLogoutIsPersisted(boolean destroySession) throws Exception {
        final Login login = login();
        second.logout().setDestroySession(destroySession);
        // The identity provider calls the other member without the browser's session cookie.
        final var response = send(second, "/callback?client_name=" + CLIENT, null,
                "logoutRequest=" + encode(logoutRequest(login.ticket())));
        assertEquals(204, response.statusCode(), response.body());
        assertLoggedOut(login, destroySession);
    }

    private Login login() throws Exception {
        final var cart = send(first, "/cart", null, null);
        assertEquals(200, cart.statusCode(), cart.body());
        final String ticket = "ST-" + UUID.randomUUID();
        final var callback = send(first, "/callback?client_name=" + CLIENT + "&ticket=" + ticket,
                cookie(cart), null);
        assertTrue(callback.statusCode() >= 300 && callback.statusCode() < 400, callback.body());
        final String cookie = cookie(callback);
        final JsonObject state = state(first, cookie);
        assertEquals(USER, state.getString("user"), state.encode());
        return new Login(ticket, state.getString("sessionId"), cookie);
    }

    private void assertLoggedOut(Login login, boolean destroySession) throws Exception {
        assertAll(
                () -> assertTrue(await(first.vertx().executeBlocking(
                        () -> first.mappings().get(login.ticket()).isEmpty(), false)), "SSO mapping remains"),
                () -> assertTrue(await(first.vertx().executeBlocking(
                        () -> first.mappings().get(login.sessionId()).isEmpty(), false)), "Reverse mapping remains"),
                () -> {
                    final Session persisted = await(first.sessions().get(login.sessionId()));
                    if (destroySession) {
                        assertNull(persisted, "Back-end session must be deleted");
                    } else {
                        assertNotNull(persisted, "Application session must be retained");
                        final var stored = new VertxSessionStore(first.sessions(), persisted)
                                .get(null, Pac4jConstants.USER_PROFILES);
                        assertTrue(stored.isEmpty() || ((Map<?, ?>) stored.get()).isEmpty(),
                                "Back-end session still contains authenticated profiles");
                        assertEquals("item-1", persisted.get("cart"));
                    }
                },
                () -> assertNull(state(first, login.cookie()).getString("user"),
                        "The browser must no longer be authenticated on the original member")
        );
    }

    private JsonObject state(Node node, String cookie) throws Exception {
        final var response = send(node, "/state", cookie, null);
        assertEquals(200, response.statusCode(), response.body());
        return new JsonObject(response.body());
    }

    private HttpResponse<String> send(Node node, String path, String cookie, String form) throws Exception {
        final var request = HttpRequest.newBuilder(URI.create(node.baseUrl() + path))
                .timeout(Duration.ofSeconds(20));
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        if (form != null) {
            request.header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form));
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String cookie(HttpResponse<?> response) {
        return response.headers().allValues("Set-Cookie").stream()
                .flatMap(header -> HttpCookie.parse(header).stream())
                .filter(c -> "vertx-web.session".equals(c.getName()))
                .map(c -> c.getName() + "=" + c.getValue()).findFirst().orElseThrow();
    }

    private static String logoutRequest(String ticket) {
        return """
                <samlp:LogoutRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                    xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion" Version="2.0">
                  <saml:NameID>unused</saml:NameID>
                  <samlp:SessionIndex>SESSION_TICKET</samlp:SessionIndex>
                </samlp:LogoutRequest>
                """.replace("SESSION_TICKET", ticket);
    }

    private static String compressedLogoutRequest(String ticket) throws Exception {
        final var bytes = new ByteArrayOutputStream();
        try (var deflater = new DeflaterOutputStream(bytes)) {
            deflater.write(logoutRequest(ticket).getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(60, TimeUnit.SECONDS);
    }

    private record Node(Vertx vertx, ClusteredSessionStore sessions,
                        VertxClusteredMapStore<String, Object> mappings,
                        DefaultSessionLogoutHandler logout, String baseUrl) { }

    private record Login(String ticket, String sessionId, String cookie) { }
}
