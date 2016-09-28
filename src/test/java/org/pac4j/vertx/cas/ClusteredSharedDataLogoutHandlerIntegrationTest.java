package org.pac4j.vertx.cas;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.auth.AuthProvider;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.SessionHandler;
import io.vertx.rxjava.ext.web.handler.UserSessionHandler;
import io.vertx.rxjava.ext.web.sstore.LocalSessionStore;
import io.vertx.test.core.VertxTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.config.CasProtocol;
import org.pac4j.cas.profile.CasProfile;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.handler.impl.CallbackHandler;
import org.pac4j.vertx.handler.impl.CallbackHandlerOptions;

import java.net.URLEncoder;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class  ClusteredSharedDataLogoutHandlerIntegrationTest extends VertxTestBase {

    private final static Logger LOG = LoggerFactory.getLogger(ClusteredSharedDataLogoutHandlerIntegrationTest.class);

    private static final String USER_ID = "userId";
    private static final String STORED_SESSION_ID = "storedSessionId";

    private static final String QUERY_STATE_URL = "/cas_state";
    private static final String TEST_USER_ID = "testUser1";
    private static final String SESSION_ID = "sessionId";
    private static final String CAS_CLIENT_NAME = "casClient";
    private static final String TEST_TICKET = "testTicket";
    private static final String CAS_LOGIN_URL = "http://localhost:8080/";
    private static final String SERVICE_VALIDATE_URL = "/serviceValidate";
    private static final String CALLBACK_URL = "/callback";

    private Vertx clusteredVertx;
    private io.vertx.rxjava.core.Vertx rxVertx;

    // This will be our session cookie header for use by requests
    protected final AtomicReference<String> sessionCookie = new AtomicReference<>();

    @Before
    public void clearSessionCookie() {
        sessionCookie.set(null);
    }

    @Before
    public void createClusteredVertx() throws Exception {
        final VertxOptions options = new VertxOptions().setClustered(true);
        final CompletableFuture<Vertx> createClusteredVertxFuture = new CompletableFuture<>();
        Vertx.clusteredVertx(options, asyncResult -> {
            if (asyncResult.succeeded()) {
                createClusteredVertxFuture.complete(asyncResult.result());
            }
        });
        clusteredVertx = createClusteredVertxFuture.get(2, TimeUnit.SECONDS);
        rxVertx = io.vertx.rxjava.core.Vertx.newInstance(clusteredVertx);
    }

    @After
    public void stopClusteredVertx() throws Exception {
        clusteredVertx.close();
    }

    @Test
    public void testTicketLifecycle() throws Exception {
        // Start server
        startServer();
        assertNullUserProfileAndStoredSessionId();
        LOG.info("Initial state validated");

        // Now simulate a login success by hitting callback handler with a ticket
        final String redirectUrl = spoofLogin();
        assertThat(redirectUrl, is("/")); // We didn't really care about redirecting, we just want to check the status now
        final JsonObject json = queryProfileAndStoredSessionId();
        final String sessionId = json.getString(SESSION_ID);
        final String storedSessionId = json.getString(STORED_SESSION_ID);
        final String userId = json.getString(USER_ID);
        assertThat(sessionId, is(storedSessionId));
        assertThat(userId, is(TEST_USER_ID));

        // Now finally simulate a Cas logout by hitting callback handler with appropriately constructed body for logout
        invokeCasLogout();
        // Now we should no longer have a user profile or stored session id
        assertNullUserProfileAndStoredSessionId();

    }

    /**
     * Spoof a login and return the session id for the current web session
     * @return The current web session id
     * @throws Exception when something gos wrong during spoofing of login
     */
    private String spoofLogin() throws Exception {
        final CompletableFuture<String> redirectUrlFuture = new CompletableFuture<>();
        final HttpClientRequest spoofLoginRequest = rxVertx.createHttpClient().get(8080, "localhost", "/callback?client_name=" +
                CAS_CLIENT_NAME+"&ticket=" + TEST_TICKET);
        Optional.ofNullable(sessionCookie.get()).ifPresent(cookie -> spoofLoginRequest.headers().add("cookie", cookie));
        final JsonObject spoofLoginRequestBody = new JsonObject().put(USER_ID, TEST_USER_ID);
        convertRequestToBlockingCallExpectingRedirect(redirectUrlFuture, spoofLoginRequest);
        spoofLoginRequest.end(spoofLoginRequestBody.encodePrettily());
        return redirectUrlFuture.get(1, TimeUnit.SECONDS);
    }

    private void invokeCasLogout() throws Exception {
        final CompletableFuture<Void> invokeLogoutFuture = new CompletableFuture<>();
        final String url =  "/callback?client_name=" +
                CAS_CLIENT_NAME + "&logoutRequest=" + URLEncoder.encode(casLogoutRequestBody());
        final HttpClientRequest casLogoutRequest = rxVertx.createHttpClient().post(8080, "localhost", url);
        casLogoutRequest.toObservable().map(resp -> {
            assertThat(resp.statusCode(), is(HttpConstants.OK));
            return null;
        })
        .subscribe(v -> invokeLogoutFuture.complete(null));
        casLogoutRequest.end();
        invokeLogoutFuture.get(1, TimeUnit.SECONDS);
    }

    private String casLogoutRequestBody() {
        return "<samlp:LogoutRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"[RANDOM ID]\" Version=\"2.0\" IssueInstant=\"[CURRENT DATE/TIME]\">\n" +
                "    <saml:NameID>@NOT_USED@</saml:NameID>\n" +
                "    <samlp:SessionIndex>"+ TEST_TICKET + "</samlp:SessionIndex>\n" +
                "</samlp:LogoutRequest>";
    }

    private void extractCookie(final HttpClientResponse resp) {
        // Only bother setting it if not already set
        if (sessionCookie.get() == null) {
            final String setCookie = resp.headers().get("set-cookie");
            assertNotNull(setCookie);
            sessionCookie.set(setCookie); // We're going to want to use this subsequently
        }
    }

    private void convertRequestToBlockingCallExpectingOk(final CompletableFuture<JsonObject> jsonFuture, final HttpClientRequest request) {
        request.toObservable()
                .flatMap(resp -> {
                    assertThat(resp.statusCode(), is(HttpConstants.OK));
                    extractCookie(resp);
                    return resp.toObservable();
                })
                .map(body -> new JsonObject(body.getDelegate().toString()))
                .doOnError(err -> {
                    throw new RuntimeException("Expected successful request round trip");
                })
                .subscribe(jsonFuture::complete);
    }

    private void convertRequestToBlockingCallExpectingRedirect(final CompletableFuture<String> redirectUrlFuture, final HttpClientRequest request) {
        request.toObservable().map(resp -> {
            assertThat(resp.statusCode(), is(HttpConstants.TEMP_REDIRECT));
            return resp.getHeader(HttpConstants.LOCATION_HEADER);
        })
        .subscribe(redirectUrlFuture::complete);
    }

    private void assertNullUserProfileAndStoredSessionId() throws Exception {
        final JsonObject json = queryProfileAndStoredSessionId();
        assertThat(json.getString(STORED_SESSION_ID), is(nullValue()));
        assertThat(json.getString(USER_ID), is(nullValue()));
    }

    private JsonObject queryProfileAndStoredSessionId() throws Exception {

        final CompletableFuture<JsonObject> jsonFuture = new CompletableFuture<>();
        final HttpClientRequest queryRequest = rxVertx.createHttpClient().get(8080, "localhost", QUERY_STATE_URL);

        Optional.ofNullable(sessionCookie.get()).ifPresent(cookie -> queryRequest.headers().add("cookie", cookie));
        convertRequestToBlockingCallExpectingOk(jsonFuture, queryRequest);
        queryRequest.end();

        // Enforce a timeout, this enables a lot more control over how we execute the test
        return jsonFuture.get(1, TimeUnit.SECONDS);
    }

//        testComplete();
    // Set up Config containing only a Cas Client
    // Register a callback handler for this Config/Client
    // Create an endpoint to spoof a login and return current session id
    // Create an endpoint to query the following: session id from shared data for a known ticket id
    // and a Json object representing currently logged in user

    // (1) Hit query endpoint, check session id for known ticket is empty, check no logged in user
    // (2) Spoof the login, store session id
    // (3) Hit query endpoint, check session id for known ticket is empty, check logged in user correct
    // (4) Hit callback endpoint with ticket param set to stored session id, should trigger storage in shared data
    // (5) Hit query endpoint, check session id for specified ticket matches session id we stored earlier
    // (6) Check currently logged in user has correct id
    // (7) Hit (POST) callback endpoint with correctly formed logoutRequest parameter - this should trigger logout as if hit by CAS server
    // (8) Hit query endpoint, check session id for known ticket is empty, check no logged in user

    private void startServer() throws Exception {
        final Router rxRouter = Router.router(rxVertx);
        final CallbackHandlerOptions callbackHandlerOptions = new CallbackHandlerOptions().setMultiProfile(false);

        final LocalSessionStore sessionStore = LocalSessionStore.create(rxVertx);
        final io.vertx.ext.web.sstore.SessionStore sstoreDelegate = (SessionStore) sessionStore.getDelegate();

        final AuthProvider authProvider = AuthProvider.newInstance(new Pac4jAuthProvider());
        rxRouter.route().handler(CookieHandler.create());
        rxRouter.route().handler(SessionHandler.create(sessionStore));
        rxRouter.route().handler(UserSessionHandler.create(authProvider));
        rxRouter.route(HttpMethod.GET, QUERY_STATE_URL).handler(queryHandler());
        rxRouter.route(HttpMethod.GET, SERVICE_VALIDATE_URL).handler(serviceValidateHandler());
        final CallbackHandler callbackHandler = new CallbackHandler((Vertx) rxVertx.getDelegate(), config(sstoreDelegate), callbackHandlerOptions);
        final io.vertx.ext.web.Router router = (io.vertx.ext.web.Router)rxRouter.getDelegate();
        router.route(HttpMethod.GET, CALLBACK_URL).handler(callbackHandler);
        router.route(HttpMethod.POST, CALLBACK_URL).handler(callbackHandler);

        final CompletableFuture<HttpServer> serverFuture = new CompletableFuture<>();
        rxVertx.createHttpServer().requestHandler(rxRouter::accept).listenObservable(8080).subscribe(serverFuture::complete);
        serverFuture.get(1, TimeUnit.SECONDS);
    }

    private Handler<RoutingContext> queryHandler() {
        return routingContext -> {

            LOG.info("queryHandler endpoint called");
            final String sessionId = routingContext.session().id();
            final ProfileManager<CasProfile> profileManager = profileManager(routingContext);
            final String userId = profileManager.get(true)
                    .map(UserProfile::getId)
                    .orElse(null);

            rxVertx.sharedData()
                    .<String, String>getClusterWideMapObservable(VertxSharedDataLogoutHandler.PAC4J_CAS_SHARED_DATA_KEY)
                    .flatMap(asyncMap -> asyncMap.getObservable("testTicket"))
                    .subscribe(storedSessionId -> {

                        final JsonObject json = new JsonObject()
                                .put(USER_ID, userId)
                                .put(SESSION_ID, sessionId)
                                .put(STORED_SESSION_ID, storedSessionId);
                        LOG.info("queryHandler endpoint about to respond with " + json.encodePrettily());
                        routingContext.response().end(json.encodePrettily());
                    });

        };
    }

    /*
    Trivial cas service validation just so we can turn a ticket into a user profile
     */
    private Handler<RoutingContext> serviceValidateHandler() {
        return rc ->
            rc.response().setStatusCode(200).end(casTicketValidationResponse());
    }

    private ProfileManager profileManager(final RoutingContext routingContext) {
        return new VertxProfileManager(new VertxWebContext((io.vertx.ext.web.RoutingContext) routingContext.getDelegate()));
    }

    // Trivial cas response
    private String casTicketValidationResponse() {
        return "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>\n" +
                "    <cas:authenticationSuccess>\n" +
                "        <cas:user>" + TEST_USER_ID + "</cas:user>\n" +
                "    </cas:authenticationSuccess>\n" +
                "</cas:serviceResponse>";
    }

    private Config config(final SessionStore sessionStore) {
        final Config config =  new Config(new Clients(CALLBACK_URL, casClient(sessionStore)));
        return config;
    }

    private CasClient casClient(final SessionStore sessionStore) {
        final CasClient casClient = new CasClient();
        casClient.setLogoutHandler(new VertxClusteredSharedDataLogoutHandler(clusteredVertx, sessionStore));
        casClient.setCasLoginUrl(CAS_LOGIN_URL);
        casClient.setName(CAS_CLIENT_NAME);
        casClient.setCasProtocol(CasProtocol.CAS20);
        return casClient;
    }


}
