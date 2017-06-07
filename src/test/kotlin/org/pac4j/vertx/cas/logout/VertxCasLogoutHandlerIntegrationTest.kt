package org.pac4j.vertx.cas.logout

import io.vertx.core.Handler
import io.vertx.core.VertxOptions
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.sstore.ClusteredSessionStore
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.buffer.Buffer
import io.vertx.rxjava.core.http.HttpServer
import io.vertx.rxjava.ext.auth.AuthProvider
import io.vertx.rxjava.ext.web.Router
import io.vertx.rxjava.ext.web.RoutingContext
import io.vertx.rxjava.ext.web.handler.BodyHandler
import io.vertx.rxjava.ext.web.handler.CookieHandler
import io.vertx.rxjava.ext.web.handler.SessionHandler
import io.vertx.rxjava.ext.web.handler.UserSessionHandler
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.pac4j.cas.client.CasClient
import org.pac4j.cas.config.CasConfiguration
import org.pac4j.cas.config.CasProtocol
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.HttpConstants
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.profile.ProfileManager
import org.pac4j.core.store.Store
import org.pac4j.vertx.*
import org.pac4j.vertx.auth.Pac4jAuthProvider
import org.pac4j.vertx.context.session.VertxSessionStore
import org.pac4j.vertx.core.store.VertxClusteredMapStore
import org.pac4j.vertx.core.store.VertxMapStoreBase
import org.pac4j.vertx.handler.impl.*
import rx.Observable
import rx.Single
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import java.util.zip.Deflater
import io.vertx.rxjava.ext.web.sstore.ClusteredSessionStore as RxClusteredSessionStore
import org.pac4j.core.context.session.SessionStore as Pac4jSessionStore


/**
 * Integration test for VertxCasLogoutHandler using store and session store based on the
 * clustered vert.x elements and calling from within executeBlocking, as we would in a
 * deployed version.
 */
class VertxCasLogoutHandlerIntegrationTest {

    companion object {
        private val LOG = LoggerFactory.getLogger(VertxCasLogoutHandlerIntegrationTest::class.java)
    }

    // This will be our session cookie header for use by requests
    private val sessionCookie = SessionCookieHolder()

    val options = VertxOptions().setClustered(true)
    val rxVertxObservable = Vertx.rxClusteredVertx(options).cache()
    val vertxSessionStoreObservable = rxVertxObservable.map { v -> ClusteredSessionStore.create(v.delegate) }.cache()
    val sessionStoreObservable = vertxSessionStoreObservable.map { s -> VertxSessionStore(s) }.cache()
    val storeObservable = rxVertxObservable.map {v -> VertxClusteredMapStore<String, Any>(v.delegate) }.cache()
    val httpClientObservable = rxVertxObservable.map { v -> v.createHttpClient() }.cache().toObservable()
    var serverObservable: Observable<HttpServer>? = null

    @Before
    fun clearSessionCookie() {
        sessionCookie.reset()
    }

    @Before
    fun startHttpServer() {
        serverObservable = startServer().cache().toObservable()
    }

    @After
    fun stopHttpServer() {
        serverObservable!!.subscribe { it.close() }
    }

    @Test(timeout = 4000)
    @Throws(Exception::class)
    fun testRecordSession() {
        val testFuture = CompletableFuture<Void>()
        serverObservable!!.flatMap { httpClientObservable }
                .map { assertNullUserProfileAndStoredSessionId() }
                .flatMap { spoofLogin()  }
                .map { assertThat(it, `is`("/"))}
                .flatMap { queryProfileAndStoredSessionId() }
                .map {
                    val sessionId = it.getString(FIELD_SESSION_ID);
                    val storedSessionId = it.getString(FIELD_STORED_SESSION_ID);
                    val userId = it.getString(FIELD_USER_ID)
                    assertThat(storedSessionId, `is`(notNullValue()))
                    assertThat(sessionId, `is`(storedSessionId))
                    assertThat(userId, `is`(TEST_USER1))
                    testFuture.complete(null)
                }
                .doOnError {
                    LOG.info(it)
                    testFuture.completeExceptionally(it)
                }
                .subscribe { Unit }
        testFuture.get(2, TimeUnit.SECONDS)
    }

    @Test(timeout = 3000)
    @Throws(Exception::class)
    fun testDestroySessionFront() {
        // Use a front-based logout
        val testFuture = CompletableFuture<Void>()
        serverObservable!!.flatMap { httpClientObservable }
                .map { assertNullUserProfileAndStoredSessionId() }
                .flatMap { spoofLogin()  }
                .map { assertThat(it, `is`("/"))}
                // We've already asserted state here in a separate test so no need to query it at this point
                // Do a front logout
                .flatMap { frontLogout() }
                .map { assertThat(it, `is`("/"))}
                .flatMap { queryProfileAndStoredSessionId() }
                .map {
                    assertProfileAndStoredSessionId(notNullValue(), nullValue(), TEST_USER1).invoke(it)
                    testFuture.complete(null)
                }
                .doOnError {
                    LOG.info(it)
                    testFuture.completeExceptionally(it)
                }
                .subscribe { Unit }
        testFuture.get(2, TimeUnit.SECONDS)
    }

    @Test(timeout = 3000)
    @Throws(Exception::class)
    fun testDestroySessionBack() {
        // Use a front-based logout
        val testFuture = CompletableFuture<Void>()
        serverObservable!!.flatMap { httpClientObservable }
                .map { assertNullUserProfileAndStoredSessionId() }
                .flatMap { spoofLogin()  }
                .map { assertThat(it, `is`("/"))}
                .flatMap { queryProfileAndStoredSessionId() }
                // We've already asserted state here in a separate test so no need to query it at this point
                // Do a back logout on a new session id
                .flatMap { backLogout() }
                .flatMap { queryProfileAndStoredSessionId() }
                .map {
                    println(it)
                    assertProfileAndStoredSessionId(notNullValue(), nullValue(), nullValue()).invoke(it)
                    testFuture.complete(null)
                }
                .doOnError {
                    LOG.info(it)
                    testFuture.completeExceptionally(it)
                }
                .subscribe { Unit }
        testFuture.get(2, TimeUnit.SECONDS)
    }

    /**
     * Spoof a login and return the session id for the current web session
     * @return The current web session id
     * @throws Exception when something gos wrong during spoofing of login
     */
    @Throws(Exception::class)
    private fun spoofLogin(): Observable<String> {
        val spoofLoginRequestBody = JsonObject().put(FIELD_USER_ID, TEST_USER1)
        return httpClientObservable.map { it.get(8080, "localhost",
                "/callback?client_name=$CAS_CLIENT_NAME&ticket=$TEST_CAS_TICKET").setChunked(true) }
                .flatMap { toResponseObservable(it,
                        addHeader("cookie", sessionCookie.retrieve()).andThen { it.write(spoofLoginRequestBody.encodePrettily()) })
                }
                .map {
                    assertThat(it.statusCode(), `is`(HttpConstants.TEMP_REDIRECT))
                    it
                }
                .map { extractCookie(it, sessionCookie.persist()) }
                .map { it.getHeader(HttpConstants.LOCATION_HEADER) }
    }

    @Throws(Exception::class)
    private fun frontLogout(): Observable<String> {
        return httpClientObservable.map {  it.get(8080, "localhost",
                "/callback?client_name=$CAS_CLIENT_NAME&logoutRequest=${urlEncodedBase64EncodedCasLogoutRequestBody()}") }
                .flatMap { toResponseObservable(it,
                        addHeader("cookie", sessionCookie.retrieve()))
                }
                .map {
                    LOG.info("frontLogout called")
                    assertThat(it.statusCode(), `is`(HttpConstants.TEMP_REDIRECT))
                    it
                }
                .map { extractCookie(it, sessionCookie.persist()) }
                .map { it.getHeader(HttpConstants.LOCATION_HEADER) }
    }

    @Throws(Exception::class)
    private fun backLogout() : Observable<Unit> {
        return httpClientObservable.map { it.post(8080, "localhost",
                "/callback?client_name=$CAS_CLIENT_NAME") }
                        .flatMap { toResponseObservable(it,
                                addHeader(HttpConstants.CONTENT_TYPE_HEADER, Supplier { "application/x-www-form-urlencoded" })
                                    .andThen { it.setChunked(true)}
                                    .andThen { it.write ("${CasConfiguration.LOGOUT_REQUEST_PARAMETER}=${URLEncoder.encode(casLogoutRequestBody())}") }
                        ) }
                .map {
                    LOG.info("backLogout called")
                    assertThat(it.statusCode(), `is`(HttpConstants.OK))
                    it
                }
                .map { Unit }
    }

    @Throws(Exception::class)
    private fun assertNullUserProfileAndStoredSessionId(): Observable<Unit> {
        return queryProfileAndStoredSessionId()
                .map {
                    assertThat(it.getString(FIELD_STORED_SESSION_ID), `is`(nullValue()))
                    assertThat(it.getString(FIELD_USER_ID), `is`(nullValue()))
                    Unit
                }
    }

    @Throws(Exception::class)
    private fun assertProfileAndStoredSessionId(sessionIdMatcher: Matcher<Any>,
                                                storedSessionIdMatcher: Matcher<Any>,
                                                expectedUserId: String): (JsonObject) -> Unit {
        return {
            val sessionId = it.getString(FIELD_SESSION_ID);
            val storedSessionId = it.getString(FIELD_STORED_SESSION_ID);
            val userId = it.getString(FIELD_USER_ID)
            assertThat(storedSessionId, `is`(storedSessionIdMatcher))
            assertThat(sessionId, `is`(sessionIdMatcher))
            assertThat(userId, `is`(expectedUserId))
        }
    }

    @Throws(Exception::class)
    private fun assertProfileAndStoredSessionId(sessionIdMatcher: Matcher<Any>,
                                                storedSessionIdMatcher: Matcher<Any>,
                                                userIdMatcher: Matcher<Any>): (JsonObject) -> Unit {
        return {
            val sessionId = it.getString(FIELD_SESSION_ID);
            val storedSessionId = it.getString(FIELD_STORED_SESSION_ID);
            val userId = it.getString(FIELD_USER_ID)
            assertThat(storedSessionId, `is`(storedSessionIdMatcher))
            assertThat(sessionId, `is`(sessionIdMatcher))
            assertThat(userId, `is`(userIdMatcher))
        }
    }

    @Throws(Exception::class)
    private fun queryProfileAndStoredSessionId(): Observable<JsonObject> {

        return httpClientObservable.map { it.get(8080, "localhost", QUERY_STATE_URL) }
                .flatMap { toResponseObservable(it,
                        addHeader("cookie", sessionCookie.retrieve()))
                }
                .map {
                    assertThat(it.statusCode(), `is`(200))
                    it
                }
                .map { extractCookie(it, sessionCookie.persist()) }
                .flatMap { it.toObservable() }
                .reduce(Buffer.buffer(), {b1, b2 -> b1.appendBuffer(b2)})
                .map {it.toJsonObject()}

    }

    @Throws(Exception::class)
    private fun startServer(): Single<HttpServer> {

        val authProvider = Pac4jAuthProvider()
        val callbackHandlerOptions = CallbackHandlerOptions().setMultiProfile(false)

        val routerObservable = rxVertxObservable.map { v -> Router.router(v) }
        val callbackHandlerObservable = Single.zip(rxVertxObservable,
                sessionStoreObservable,
                storeObservable,
                { v, ss, s -> CallbackHandler(v.delegate, ss, config(s), callbackHandlerOptions) })

        return Single.zip(rxVertxObservable,
                routerObservable,
                vertxSessionStoreObservable,
                sessionStoreObservable,
                callbackHandlerObservable,
                { v, r, s, ss, ch ->

            with(r) {
                route().handler(BodyHandler.create())
                route().handler(CookieHandler.create())
                route().handler(SessionHandler.create(RxClusteredSessionStore.newInstance(s)))
                route().handler(UserSessionHandler.create(AuthProvider.newInstance(authProvider)))
                route(HttpMethod.GET, QUERY_STATE_URL).handler(queryHandler(v, ss))
                route(HttpMethod.GET, SERVICE_VALIDATE_URL).handler(serviceValidateHandler())
            }
            with(r.delegate) {
                route(HttpMethod.GET, URL_CALLBACK).handler(ch)
                route(HttpMethod.POST, URL_CALLBACK).handler(ch)
            }
            v.createHttpServer().requestHandler(r::accept)
        })
        .flatMap { s -> s.rxListen(8080) }

    }

     private fun urlEncodedBase64EncodedCasLogoutRequestBody(): String {
        val requestBodyBytes = casLogoutRequestBody().toByteArray(Charsets.UTF_8)
        val output = ByteArray(requestBodyBytes.size)
        val deflater = Deflater()
        deflater.setInput(requestBodyBytes)
        deflater.finish()
        val compressedLength = deflater.deflate(output)

        val base64 = Base64.getEncoder().encodeToString(output.copyOf(compressedLength))
        deflater.end()
        LOG.info("Base 64 = $base64")
        return URLEncoder.encode(base64, "UTF-8")
    }

    private fun casLogoutRequestBody(): String {
        return "<samlp:LogoutRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"[RANDOM ID]\" Version=\"2.0\" IssueInstant=\"[CURRENT DATE/TIME]\">\n" +
                "    <saml:NameID>@NOT_USED@</saml:NameID>\n" +
                "    <samlp:SessionIndex>$TEST_CAS_TICKET</samlp:SessionIndex>\n" +
                "</samlp:LogoutRequest>"
    }

    private fun queryHandler(rxVertx: Vertx, sessionStore: Pac4jSessionStore<VertxWebContext>): Handler<RoutingContext> {

        return Handler { routingContext: RoutingContext ->

            LOG.info("queryHandler endpoint called")
            val sessionId = routingContext.session().id()
            val profileManager = profileManager(routingContext, sessionStore)
            val userId = profileManager.get(true)
                    .map { p -> p.id }
                    .orElse(null)

            rxVertx.sharedData()
                    .rxGetClusterWideMap<String, String>(VertxMapStoreBase.PAC4J_SHARED_DATA_KEY)
                    .flatMap<String> { asyncMap -> asyncMap.rxGet(TEST_CAS_TICKET) }
                    .subscribe { storedSessionId ->

                        val json = JsonObject()
                                .put(FIELD_USER_ID, userId)
                                .put(FIELD_SESSION_ID, sessionId)
                                .put(FIELD_STORED_SESSION_ID, storedSessionId);
                        LOG.info("queryHandler endpoint about to respond with " + json.encodePrettily());
                        routingContext.response().end(json.encodePrettily());
                    }

        }
    }

    private fun serviceValidateHandler(): Handler<RoutingContext> {
        return Handler { rc -> rc.response().setStatusCode(200).end(casTicketValidationResponse()) }
    }

    // Trivial cas response
    private fun casTicketValidationResponse(): String {
        return "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>\n" +
                "    <cas:authenticationSuccess>\n" +
                "        <cas:user>" + TEST_USER1 + "</cas:user>\n" +
                "    </cas:authenticationSuccess>\n" +
                "</cas:serviceResponse>"
    }

    private fun profileManager(routingContext: RoutingContext, sessionStore: Pac4jSessionStore<VertxWebContext>): ProfileManager<CommonProfile> {
        return VertxProfileManager(VertxWebContext(routingContext.delegate, sessionStore))
    }

    private fun config(store: Store<String, Any>): Config {
        val config = Config(Clients(URL_CALLBACK, casClient(store)))
        return config
    }

    private fun casClient(store: Store<String, Any>): CasClient {
        val casConfig = CasConfiguration()
        casConfig.loginUrl = CAS_LOGIN_URL
        casConfig.protocol = CasProtocol.CAS20
        casConfig.logoutHandler = VertxCasLogoutHandler(store, false)
        val casClient = CasClient(casConfig)
        casClient.name = CAS_CLIENT_NAME
        return casClient
    }
}