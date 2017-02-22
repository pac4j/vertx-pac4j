package org.pac4j.vertx.handler.impl

import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory.getLogger
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.buffer.Buffer
import io.vertx.rxjava.core.http.HttpClient
import io.vertx.rxjava.core.http.HttpClientResponse
import io.vertx.rxjava.ext.web.Router
import io.vertx.rxjava.ext.web.RoutingContext
import io.vertx.test.core.VertxTestBase
import org.junit.Before
import org.junit.Test
import org.pac4j.core.context.Pac4jConstants
import org.pac4j.core.context.session.SessionStore
import org.pac4j.vertx.VertxWebContext
import org.pac4j.vertx.context.session.VertxSessionStore
import org.pac4j.vertx.profile.TestOAuth1Profile
import rx.Observable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Supplier
import org.hamcrest.core.Is.`is` as isEqualTo

/**
 * Tests for application logout handler. Ensure that it carries out expected
 * logic on application logout.
 * @since 2.0.0
 */
class ApplicationLogoutHandlerIntegrationTest: VertxTestBase() {

    companion object {

        val LOG: Logger = getLogger(ApplicationLogoutHandlerIntegrationTest::class.java)

        /**
         * A handler whose only job is to record a pac4j profile as logged in
         * so that we can subsequently log it out.
         */
        fun spoofLoginHandler(rc: RoutingContext, sessionStore: SessionStore<VertxWebContext>) {

            LOG.info("Spoof login endpoint called")
            LOG.info("Session id = " + rc.session().id())

            // Set up a pac4j user and save into the session, we can interrogate later
            val profileManager = getProfileManager(rc, sessionStore)
            val profile = TestOAuth1Profile()
            with (profile) {
                setId(TEST_USER1)
                addAttribute(EMAIL_KEY, TEST_EMAIL)
            }
            profileManager.save(true, profile, false)

            LOG.info("Spoof login endpoint completing")
            rc.response().setStatusCode(204).end()
        }

    }

    // rx Vertx reference
    private var rxVertx: Vertx? = null

    // Http client to be used by all tests
    private var client: HttpClient? = null

    // This will be our session cookie header for use by requests
    private val sessionCookie = AtomicReference<String>()

    private val sessionStore = VertxSessionStore()

    @Before
    fun spinUpServerAndClient() {
        rxVertx = Vertx.newInstance(vertx)
        client = rxVertx!!.createHttpClient()
        startServerWithSessionSupport(rxVertx!!, Consumer<Router> {
            r ->
            with(r) {
                route(HttpMethod.POST, URL_SPOOF_LOGIN).handler  { spoofLoginHandler(it, sessionStore) }
                route(HttpMethod.GET, URL_QUERY_PROFILE).handler { getProfileHandler(it, sessionStore) }
                get(URL_LOGOUT).handler(logoutHandler(vertx, sessionStore))
            }
        })
    }

    @Before
    fun resetSessionCookie() {
        sessionCookie.set(null)
    }

    /**
     * Test that hitting the logout endpoint actually logs the user out from the point of view of
     * removing all the user's details.
     */
    @Test
    fun simpleLogoutTest() {

        val notNullClient = client!!
        testLogout(notNullClient, URL_LOGOUT, {
            assertThat(it.statusCode(), isEqualTo(200))
        })
    }

    @Test
    fun testLogoutWithRedirectToQueryParamUrl() {
        val notNullClient = client!!
        val urlWithRedirect = URL_LOGOUT + "?" + Pac4jConstants.URL + "=" + "/logoutDone"

        testLogout(notNullClient, urlWithRedirect, {
            assertThat(it.statusCode(), isEqualTo(302))
            assertThat(it.getHeader("location"), isEqualTo("/logoutDone"))
        })
    }

    fun testLogout(client: HttpClient, logoutUrl: String, responseValidator: (HttpClientResponse) -> Unit ) {
        successfulLoginObservable(client)
                .flatMap { Observable.just(client.get(PORT, HOST, logoutUrl)) }
                .flatMap { toResponseObservable(it, addHeader("cookie", retrieveSessionCookie())) }
                .map {
                    responseValidator(it)
                    it
                }
                .flatMap { Observable.just(client.get(PORT, HOST, URL_QUERY_PROFILE)) }
                .flatMap { toResponseObservable(it, addHeader("cookie", retrieveSessionCookie())) }
                .map { assertThatResponseCodeIs(it, 200) }
                .flatMap { it.toObservable() }
                .reduce { accumulator: Buffer?, current: Buffer? ->  accumulator!!.appendBuffer(current) }
                .map { it.toJsonObject() }
                .doOnError { fail(it) }
                .subscribe {
                    with (it) {
                        assertThat(getString(org.pac4j.vertx.handler.impl.USER_ID_KEY), org.hamcrest.core.Is.`is`(org.hamcrest.CoreMatchers.nullValue()))
                        assertThat(getString(org.pac4j.vertx.handler.impl.EMAIL_KEY), org.hamcrest.core.Is.`is`(org.hamcrest.CoreMatchers.nullValue()))
                    }
                    testComplete()
                }

        await(2, TimeUnit.SECONDS)

    }

    fun successfulLoginObservable(client: HttpClient): Observable<JsonObject> {
        val spoofLoginRequest = client.post(PORT, HOST, URL_SPOOF_LOGIN)

        return toResponseObservable(spoofLoginRequest, addHeader("cookie", retrieveSessionCookie()))
                .map { extractCookie(it, persistSessionCookie()) }
                .map { assertThatResponseCodeIs(it, 204)}
                .flatMap { Observable.just(client.get(PORT, HOST, URL_QUERY_PROFILE)) }
                .flatMap { toResponseObservable(it, addHeader("cookie", retrieveSessionCookie())) }
                .map { assertThatResponseCodeIs(it, 200) }
                .flatMap { it.toObservable() }
                .reduce { accumulator: Buffer?, current: Buffer? ->  accumulator!!.appendBuffer(current) }
                .map { it.toJsonObject() }
                .map {
                    with (it) {
                        assertThat(getString(USER_ID_KEY), isEqualTo(TEST_USER1))
                        assertThat(getString(EMAIL_KEY), isEqualTo(TEST_EMAIL))
                    }
                    it
                }

    }

    private fun persistSessionCookie(): Consumer<String> = Consumer {

        // Only bother setting it if not already set
        if(sessionCookie.get() == null) {
            sessionCookie.set(it)
        }
    }

    private fun retrieveSessionCookie(): Supplier<String?> = Supplier { sessionCookie.get() }

}