package org.pac4j.vertx.handler.impl

import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.handler.UserSessionHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.buffer.Buffer
import io.vertx.rxjava.core.http.HttpClientResponse
import io.vertx.test.core.VertxTestBase
import org.hamcrest.CoreMatchers.`is`
import org.junit.Before
import org.junit.Test
import org.pac4j.core.client.Clients
import org.pac4j.core.client.direct.AnonymousClient
import org.pac4j.core.config.Config
import org.pac4j.vertx.TestConstants.FORBIDDEN_BODY
import org.pac4j.vertx.TestConstants.UNAUTHORIZED_BODY
import org.pac4j.vertx.auth.Pac4jAuthProvider
import org.pac4j.vertx.client.HeaderBasedDirectClient
import org.pac4j.vertx.context.session.VertxSessionStore
import org.pac4j.vertx.http.DefaultHttpActionAdapter
import rx.Observable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Test to ensure that direct clients are not retained in session for subsequent
 * authentication attempts - only indirect clients should be passed to a subsequent
 * VerxWebContext
 */
class Pac4jUserCleanupTest: VertxTestBase() {

    // This will be our session cookie header for use by requests
    private val sessionCookie = SessionCookieHolder()

    // rx vertx
    private var rxVertx: Vertx? = null

    // Start web server with session/user-session support as if we were also running stateful
    // clients (this is only a concern where we have user-session support)
    fun startWebServer() {

        val server = vertx.createHttpServer()
        val router = Router.router(vertx)
        val authProvider = Pac4jAuthProvider()
        val sessionStore = LocalSessionStore.create(vertx)
        val pac4jSessionStore = VertxSessionStore()
        val config = Config(clients())
        config.httpActionAdapter = DefaultHttpActionAdapter()

        // Now let's create an anonymous index handler protected by anonymous client
        // And a private index handler protected by the header-based direct client
        val anonymousOptions = SecurityHandlerOptions().setClients("AnonymousClient")
        val headerBasedOptions = SecurityHandlerOptions().setClients("abc")
        val anonymousSecurityHandler = SecurityHandler(vertx, pac4jSessionStore, config, authProvider, anonymousOptions)
        val headerBasedSecurityHandler = SecurityHandler(vertx, pac4jSessionStore, config, authProvider, headerBasedOptions)
        val profileRetrievalHandler = Handler<RoutingContext>() { rc -> getProfileHandler(io.vertx.rxjava.ext.web.RoutingContext(rc), pac4jSessionStore)}

        with(router) {
            route().handler(CookieHandler.create())
            route().handler(SessionHandler.create(sessionStore))
            route().handler(UserSessionHandler.create(authProvider))
            listOf(anonymousSecurityHandler, profileRetrievalHandler)
                    .forEach { route(HttpMethod.GET, "/anonymous/index.html").handler(it) }
            listOf(headerBasedSecurityHandler, profileRetrievalHandler)
                    .forEach { route(HttpMethod.GET, "/header-based/index.html").handler(it) }
            route().failureHandler { rc ->

                val statusCode = rc.statusCode()
                rc.response().statusCode = if (statusCode > 0) statusCode else 500 // use status code 500 in the event that vert.x hasn't set one,

                when (rc.response().statusCode) {

                    401 -> rc.response().end(UNAUTHORIZED_BODY)

                    403 -> rc.response().end(FORBIDDEN_BODY)

                    else -> {
                        LOG.error("Unexpected error in request handling", rc.failure())
                        rc.response().end("Unexpected error")
                    }
                }
            }
        }

        val latch = CountDownLatch(1)
        server.requestHandler({ router.accept(it) }).listen(8080, { asyncResult ->
            if (asyncResult.succeeded()) {
                latch.countDown()
            } else {
                fail("Http server failed to start so test could not proceed")
            }
        })
        assertTrue(latch.await(1L, TimeUnit.SECONDS))


//        val pac4jAuthHandler = authHandler(router,
//                authProvider,
//                baseAuthUrl,
//                options,
//                callbackHandlerOptions,
//                requiredPermissions)
//
//        routerDecorator.accept(router, config)

//        startWebServer(router, pac4jAuthHandler)
    }

    @Before
    fun resetSessionCookie() {
        sessionCookie.reset()
        rxVertx = Vertx.newInstance(vertx)
    }

    /**
     * Test for anyonymous access alone, to demonstrate what should happen when this is called alone
     */
    @Test
    fun testAnonymousAccess() {
        startWebServer()
        val client = rxVertx!!.createHttpClient()
        val anonymousIndexRequest = client.get(PORT, HOST, "/anonymous/index.html")
        toResponseObservable(anonymousIndexRequest, addHeader("cookie", sessionCookie.retrieve()))
                .map { extractCookie(it, sessionCookie.persist()) }
                .map { assertThatResponseCodeIs(it, 200)}
                .flatMap { convertBodyToJson(it) }
                .map { assertThat(it.getString(FIELD_USER_ID), `is`("anonymous")) }
                .doOnError { fail(it) }
                .subscribe {
                    testComplete()
                }
        await(2, TimeUnit.SECONDS)
    }

    /**
     * Test for frailing direct client, to demonstrate what should happen when called alone. This should also be
     * the behaviour when called with the same session id after hitting an anonymously secured endpoint
     */
    @Test
    fun testFailingHeaderBasedAccess() {
        startWebServer()
        val client = rxVertx!!.createHttpClient()
        val headerBasedIndexRequest = client.get(PORT, HOST, "/header-based/index.html")
        toResponseObservable(headerBasedIndexRequest,
                addHeader("cookie", sessionCookie.retrieve())
                        .andThen {headerBasedIndexRequest.putHeader("Authorization", "DEF")})
            .map { extractCookie(it, sessionCookie.persist()) }
            .map { assertThatResponseCodeIs(it, 401)}
            .doOnError { fail(it) }
            .subscribe { testComplete() }

        await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testFailingHeaderBasedAccessWhenAnonymousAccessHasBeenTriggered() {
        startWebServer()
        val client = rxVertx!!.createHttpClient()
        val anonymousIndexRequest = client.get(PORT, HOST, "/anonymous/index.html")
        toResponseObservable(anonymousIndexRequest, addHeader("cookie", sessionCookie.retrieve()))
                .map { extractCookie(it, sessionCookie.persist()) }
                .map { assertThatResponseCodeIs(it, 200)}
                .flatMap { convertBodyToJson(it) }
                .map { assertThat(it.getString(FIELD_USER_ID), `is`("anonymous")) }
                // Now trigger a failing header-based request which should fail with a 401 as per previous test
                .flatMap {
                    val headerBasedIndexRequest = client.get(PORT, HOST, "/header-based/index.html")
                    toResponseObservable(headerBasedIndexRequest,
                        addHeader("cookie", sessionCookie.retrieve())
                                .andThen {headerBasedIndexRequest.putHeader("Authorization", "DEF")})
                }
                .map { extractCookie(it, sessionCookie.persist()) }
                .map { assertThatResponseCodeIs(it, 401)}
                .doOnError { fail(it) }
                .subscribe { testComplete() }

        await(2, TimeUnit.SECONDS)

    }

    fun convertBodyToJson(response: HttpClientResponse): Observable<JsonObject> = response.toObservable()
                .reduce { accumulator: Buffer?, current: Buffer? ->  accumulator!!.appendBuffer(current) }
                .map(Buffer::toJsonObject)

    fun clients(): Clients {
        return Clients(AnonymousClient(), HeaderBasedDirectClient("ABC"))
    }


}