package org.pac4j.vertx.handler.impl

import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory.getLogger
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.buffer.Buffer
import io.vertx.rxjava.core.http.HttpClient
import io.vertx.rxjava.core.http.HttpClientRequest
import io.vertx.rxjava.core.http.HttpClientResponse
import io.vertx.rxjava.ext.web.Router
import io.vertx.rxjava.ext.web.RoutingContext
import io.vertx.rxjava.ext.web.handler.BodyHandler
import io.vertx.test.core.VertxTestBase
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Before
import org.junit.Test
import org.pac4j.core.context.Pac4jConstants
import org.pac4j.core.context.session.SessionStore
import org.pac4j.vertx.VertxWebContext
import org.pac4j.vertx.context.session.VertxSessionStore
import org.pac4j.vertx.profile.SimpleTestProfile
import rx.Observable
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.hamcrest.core.Is.`is` as isEqualTo

/**
 * Tests for application logout handler. Ensure that it carries out expected
 * logic on application logout.
 * @since 2.0.0
 */
class LogoutHandlerIntegrationTest : VertxTestBase() {

    companion object {

        val LOG: Logger = getLogger(LogoutHandlerIntegrationTest::class.java)

        /**
         * A handler whose only job is to record a pac4j profile as logged in
         * so that we can subsequently log it out.
         */
        fun spoofLoginHandler(rc: RoutingContext, sessionStore: SessionStore<VertxWebContext>) {

            LOG.info("Spoof login endpoint called")
            LOG.info("Session id = " + rc.session().id())

            // Set up a pac4j user and save into the session, we can interrogate later
            val profileManager = getProfileManager(rc, sessionStore)
            val profile = SimpleTestProfile(TEST_USER1, TEST_EMAIL)
            profile.clientName = TEST_QUERY_PARAM_CLIENT_NAME
            profileManager.save(true, profile, false)

            LOG.info("Spoof login endpoint completing")
            rc.response().setStatusCode(204).end()
        }

        fun addValueToSessionHandler(rc: RoutingContext) {
            LOG.info("Add value to session endpoint called")
            val body = rc.bodyAsJson
            val key = body.getString(FIELD_KEY)
            val value = body.getString(FIELD_VALUE)
            Objects.requireNonNull(key)
            Objects.requireNonNull(value)
            LOG.info("Setting session entry " + key + "to " + value)
            rc.session().put(key, value)
            rc.response().setStatusCode(200).end()
        }

        fun getValueFromSessionHandler(rc: RoutingContext) {
            LOG.info("Get value from session endpoint called")
            val key = rc.request().getParam(FIELD_KEY)
            Objects.requireNonNull(key)
            val value = rc.session().get<String>(key)
            LOG.info("Value from session is " + value)
            val responseBody = JsonObject().put(FIELD_VALUE, value)
            rc.response().setChunked(true).setStatusCode(200).write(responseBody.toString()).end()
        }

    }

    // rx Vertx reference
    private var rxVertx: Vertx? = null

    // Http client to be used by all tests
    private var client: HttpClient? = null

    // This will be our session cookie header for use by requests
    private val sessionCookie = SessionCookieHolder()

    private val sessionStore = VertxSessionStore()

    fun spinUpServerAndClient() {
        spinUpServerAndClient(LogoutHandlerOptions())
    }

    fun spinUpServerAndClient(logoutHandlerOptions: LogoutHandlerOptions) {
        rxVertx = Vertx.newInstance(vertx)
        client = rxVertx!!.createHttpClient()
        startServerWithSessionSupport(rxVertx!!, Consumer<Router> {
            r ->
            with(r) {
                route(HttpMethod.POST, URL_SPOOF_LOGIN).handler  { spoofLoginHandler(it, sessionStore) }
                route(HttpMethod.GET, URL_QUERY_PROFILE).handler { getProfileHandler(it, sessionStore) }
                route(HttpMethod.POST, URL_SET_SESSION_VALUE).handler(BodyHandler.create())
                route(HttpMethod.POST, URL_SET_SESSION_VALUE).handler { addValueToSessionHandler(it) }
                route(HttpMethod.GET, URL_GET_SESSION_VALUE).handler { getValueFromSessionHandler(it) }
                get(URL_LOGOUT).handler(logoutHandler(vertx, sessionStore, logoutHandlerOptions))
            }
        })
    }

    @Before
    fun resetSessionCookie() {
        sessionCookie.reset()
    }

    /**
     * Test that hitting the logout endpoint actually logs the user out from the point of view of
     * removing all the user's details. Effects should be as for centralLogout false, destroySession false,
     * localLogout true
     */
    @Test
    fun simpleLogoutTesWithDefaults() {

        spinUpServerAndClient()
        val notNullClient = client!!
        setSessionValue(notNullClient, TEST_SESSION_KEY, TEST_SESSION_VALUE)
                .flatMap {
                    loginThenLogout(notNullClient, URL_LOGOUT ,{
                    assertThat(it.statusCode(), isEqualTo(200))
                },
                {
                    with (it) {
                        assertThat(getString(USER_ID_KEY),`is`(org.hamcrest.CoreMatchers.nullValue()))
                        assertThat(getString(EMAIL_KEY), `is`(org.hamcrest.CoreMatchers.nullValue()))
                    }
                })}
                .flatMap { retrieveSessionValue(notNullClient, TEST_SESSION_KEY) }
                .doOnError { fail(it) }
                .subscribe({
                    assertThat(it, `is`(TEST_SESSION_VALUE))
                    testComplete()
                })
        await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testLogoutWithLocalLogoutTrue() {
        spinUpServerAndClient(LogoutHandlerOptions().setLocalLogout(true))
        val notNullClient = client!!
        testLogoutExpectingNoProfile(notNullClient, URL_LOGOUT, {
            assertThat(it.statusCode(), isEqualTo(200))
        })
    }

    @Test
    fun testLogoutWithLocalLogoutFalse() {
        spinUpServerAndClient(LogoutHandlerOptions().setLocalLogout(false))
        val notNullClient = client!!
        testLogoutExpectingProfileToMatch(notNullClient, URL_LOGOUT,
                {
                    assertThat(it.statusCode(), isEqualTo(200))
                },
                {
                    with (it) {
                        assertThat(getString(USER_ID_KEY), isEqualTo(TEST_USER1))
                        assertThat(getString(EMAIL_KEY), isEqualTo(TEST_EMAIL))
                    }
                })
    }

    @Test
    fun testLogoutWithCentralLogoutFalse() {
        spinUpServerAndClient(LogoutHandlerOptions().setCentralLogout(false))
        val notNullClient = client!!
        setSessionValue(notNullClient, TEST_SESSION_KEY, TEST_SESSION_VALUE)
                .flatMap {
                    loginThenLogout(notNullClient, URL_LOGOUT ,{
                    assertThat(it.statusCode(), isEqualTo(200))
                },
                        {
                            with (it) {
                                assertThat(getString(USER_ID_KEY),`is`(org.hamcrest.CoreMatchers.nullValue()))
                                assertThat(getString(EMAIL_KEY), `is`(org.hamcrest.CoreMatchers.nullValue()))
                            }
                        })}
                .flatMap { retrieveSessionValue(notNullClient, TEST_SESSION_KEY) }
                .doOnError { fail(it) }
                .subscribe({
                    assertThat(it, `is`(TEST_SESSION_VALUE))
                    testComplete()
                })
        await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testLogoutWithCentralLogoutTrue() {
        spinUpServerAndClient(LogoutHandlerOptions().setCentralLogout(true))
        val notNullClient = client!!
        successfulLogin(notNullClient)
                .flatMap { logout(notNullClient, URL_LOGOUT) }
                // We expect to be redirected to the url specified for the client
                .map {
                    LOG.info(it.statusMessage())
                    LOG.info(it.statusCode())
                    LOG.info(it.toString())
                    assertThat(it.statusCode(), `is`(302))
                    assertThat(it.getHeader("location"), `is`(CENTRAL_LOGOUT_URL))
                    it
                }
                .flatMap { retrieveProfile(notNullClient) }
                .subscribe {
                    // Check that profile was cleared as well as the redirect
                    with (it) {
                        assertThat(getString(USER_ID_KEY),`is`(org.hamcrest.CoreMatchers.nullValue()))
                        assertThat(getString(EMAIL_KEY), `is`(org.hamcrest.CoreMatchers.nullValue()))
                        testComplete()
                    }
                }
        await(3, TimeUnit.SECONDS)
    }


    @Test
    fun testLogoutWithDestroySessionFalseDoesNotDestroySession() {
        spinUpServerAndClient(LogoutHandlerOptions().setDestroySession(false))
        val notNullClient = client!!
        setSessionValue(notNullClient, TEST_SESSION_KEY, TEST_SESSION_VALUE)
                .flatMap {
                    loginThenLogout(notNullClient, URL_LOGOUT ,{
                    assertThat(it.statusCode(), isEqualTo(200))
                },
                        {
                            with (it) {
                                assertThat(getString(USER_ID_KEY),`is`(org.hamcrest.CoreMatchers.nullValue()))
                                assertThat(getString(EMAIL_KEY), `is`(org.hamcrest.CoreMatchers.nullValue()))
                            }
                        })}
                .flatMap { retrieveSessionValue(notNullClient, TEST_SESSION_KEY) }
                .doOnError { fail(it) }
                .subscribe({
                    assertThat(it, `is`(TEST_SESSION_VALUE))
                    testComplete()
                })
        await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testLogoutWithDestroySessionTrueDestroysSession() {
        spinUpServerAndClient(LogoutHandlerOptions().setDestroySession(true))
        val notNullClient = client!!
        setSessionValue(notNullClient, TEST_SESSION_KEY, TEST_SESSION_VALUE)
                .flatMap {
                    loginThenLogout(notNullClient, URL_LOGOUT ,{
                    assertThat(it.statusCode(), isEqualTo(200))
                },
                        {
                            with (it) {
                                assertThat(getString(USER_ID_KEY),`is`(org.hamcrest.CoreMatchers.nullValue()))
                                assertThat(getString(EMAIL_KEY), `is`(org.hamcrest.CoreMatchers.nullValue()))
                            }
                        })}
                .flatMap { retrieveSessionValue(notNullClient, TEST_SESSION_KEY) }
                .doOnError { fail(it) }
                .subscribe({
                    assertThat(it, `is`(nullValue()))
                    testComplete()
                })
        await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testLogoutWithRedirectToQueryParamUrl() {
        spinUpServerAndClient()
        val notNullClient = client!!
        val urlWithRedirect = URL_LOGOUT + "?" + Pac4jConstants.URL + "=" + "/logoutDone"

        testLogoutExpectingNoProfile(notNullClient, urlWithRedirect, {
            assertThat(it.statusCode(), isEqualTo(302))
            assertThat(it.getHeader("location"), isEqualTo("/logoutDone"))
        })
    }

    fun testLogoutExpectingNoProfile(client: HttpClient, logoutUrl: String, responseValidator: (HttpClientResponse) -> Unit ) {
        testLogoutExpectingProfileToMatch(client, logoutUrl, responseValidator, {
            with (it) {
                assertThat(getString(org.pac4j.vertx.handler.impl.USER_ID_KEY), org.hamcrest.core.Is.`is`(org.hamcrest.CoreMatchers.nullValue()))
                assertThat(getString(org.pac4j.vertx.handler.impl.EMAIL_KEY), org.hamcrest.core.Is.`is`(org.hamcrest.CoreMatchers.nullValue()))
            }

        })

    }

    fun testLogoutExpectingProfileToMatch(client: HttpClient, logoutUrl: String, responseValidator: (HttpClientResponse) -> Unit,
                                   profileJsonValidator: (JsonObject) -> Unit) {
        loginThenLogout(client, logoutUrl, responseValidator, profileJsonValidator)
                .subscribe {
                    testComplete()
                }

        await(2, TimeUnit.SECONDS)
    }

    fun logout(client: HttpClient, logoutUrl: String): Observable<HttpClientResponse> {
        return Observable.just(client.get(PORT, HOST, logoutUrl))
                .flatMap { toResponseObservable(it, addHeader("cookie", sessionCookie.retrieve())) }
    }

    fun loginThenLogout(client: HttpClient, logoutUrl: String, responseValidator: (HttpClientResponse) -> Unit,
                        profileJsonValidator: (JsonObject) -> Unit): Observable<JsonObject> {
        return successfulLogin(client)
                .flatMap { logout(client, logoutUrl) }
                .map {
                    responseValidator(it)
                    it
                }
                .flatMap { retrieveProfile(client) }
                .map {
                    profileJsonValidator(it)
                    it
                }
                .doOnError { fail(it) }
    }

    fun successfulLogin(client: HttpClient): Observable<JsonObject> {
        val spoofLoginRequest = client.post(PORT, HOST, URL_SPOOF_LOGIN)

        return toResponseObservable(spoofLoginRequest, addHeader("cookie", sessionCookie.retrieve()))
                .map { extractCookie(it, sessionCookie.persist()) }
                .map { assertThatResponseCodeIs(it, 204)}
                .flatMap { retrieveProfile(client) }
                .map {
                    with (it) {
                        assertThat(getString(USER_ID_KEY), isEqualTo(TEST_USER1))
                        assertThat(getString(EMAIL_KEY), isEqualTo(TEST_EMAIL))
                    }
                    it
                }

    }

    fun retrieveProfile(client: HttpClient): Observable<JsonObject> {
        return Observable.just(client.get(PORT, HOST, URL_QUERY_PROFILE))
                .flatMap { toResponseObservable(it, addHeader("cookie", sessionCookie.retrieve())) }
                .map { assertThatResponseCodeIs(it, 200) }
                .flatMap { it.toObservable() }
                .reduce { accumulator: Buffer?, current: Buffer? ->  accumulator!!.appendBuffer(current) }
                .map(Buffer::toJsonObject)
    }

    fun setSessionValue(client: HttpClient, keyName: String, keyValue: String): Observable<HttpClientResponse> {
        val setSessionRequest = client.post(PORT, HOST, URL_SET_SESSION_VALUE).setChunked(true)
        val requestBody = JsonObject()
                .put(FIELD_KEY, keyName)
                .put(FIELD_VALUE, keyValue)
        return toResponseObservable(setSessionRequest, Consumer<HttpClientRequest>{it.write(requestBody.toString())})
                .map { extractCookie(it, sessionCookie.persist()) }
    }

    private fun retrieveSessionValue(client: HttpClient, sessionKey: String): Observable<String> =
        Observable.just(client.get(PORT, HOST, "$URL_GET_SESSION_VALUE?$FIELD_KEY=$sessionKey"))
            .flatMap { toResponseObservable(it, addHeader("cookie", sessionCookie.retrieve())) }
            .map { assertThatResponseCodeIs(it, 200)}
            .flatMap { it.toObservable() }
            .reduce { accumulator: Buffer?, current: Buffer? ->  accumulator!!.appendBuffer(current) }
            .map(Buffer::toJsonObject)
            .map { it.getString(FIELD_VALUE) }

}