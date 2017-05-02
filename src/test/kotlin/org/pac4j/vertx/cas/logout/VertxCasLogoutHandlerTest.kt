package org.pac4j.vertx.cas.logout

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.net.impl.SocketAddressImpl
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.Session
import io.vertx.ext.web.handler.impl.UserHolder
import io.vertx.ext.web.sstore.LocalSessionStore
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.pac4j.vertx.*
import org.pac4j.vertx.auth.Pac4jUser
import org.pac4j.vertx.cas.logout.VertxCasLogoutHandler.PAC4J_CAS_TICKET
import org.pac4j.vertx.context.session.VertxSessionStore
import org.pac4j.vertx.core.store.VertxLocalMapStore
import org.pac4j.vertx.profile.SimpleTestProfile
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 *
 */
class VertxCasLogoutHandlerTest {

    val vertx: Vertx = Vertx.vertx()
    val store = VertxLocalMapStore<String, Any>(vertx)
    val vertxSessionStore = LocalSessionStore.create(vertx)
    val sessionStore = VertxSessionStore(vertxSessionStore)

    var vertxSession: Session? = null
    var routingContext: RoutingContext? = null
    var webContext: VertxWebContext? = null

    @Before
    fun setup() {
        val setupFuture = CompletableFuture<Void>()
        val request: HttpServerRequest = mockRequest()
        vertxSession = vertxSessionStore.createSession(20000)
        vertxSessionStore.put(vertxSession, { result ->
            if (result.succeeded()) {
                setupFuture.complete(null)
            } else {
                setupFuture.completeExceptionally(result.cause())
            }
        })
        routingContext = mock<RoutingContext> {
            on { session() } doReturn vertxSession
            on { request() } doReturn request
        }
        webContext = VertxWebContext(routingContext, sessionStore)
        setupFuture.get(1, TimeUnit.SECONDS)
    }

    @Test
    @Throws(Exception::class)
    fun testRecordSession() {
        val handler = VertxCasLogoutHandler(store, false, ::VertxProfileManager)
        handler.recordSession(webContext, TEST_CAS_TICKET)
        assertThat(vertxSession!!.get(PAC4J_CAS_TICKET), `is`(TEST_CAS_TICKET))
        assertThat(store.get(TEST_CAS_TICKET).toString(), `is`(vertxSession!!.id()))
    }

    @Test
    @Throws(Exception::class)
    fun testDestroySessionFront() {
        val handler = VertxCasLogoutHandler(store, false, ::VertxProfileManager)
        vertxSession!!.put(PAC4J_CAS_TICKET, TEST_CAS_TICKET)
        store.set(TEST_CAS_TICKET, vertxSession!!.id())
        handler.destroySessionFront(webContext, TEST_CAS_TICKET)
        assertThat(vertxSession!!.get(PAC4J_CAS_TICKET), `is`(nullValue()))
        assertThat(store.get(TEST_CAS_TICKET), `is`(nullValue()))
    }

    @Test
    @Throws(Exception::class)
    fun destroySessionBack() {

        val otherSession = otherSession()

        // Now set up a pac 4j user
        val vertxUser = Pac4jUser()
        vertxUser.setUserProfile(OAUTH2_CLIENT_NAME, SimpleTestProfile(TEST_USER1, TEST_EMAIL), false)
        val userHolder = UserHolder()
        userHolder.user = vertxUser

        // Put pac4j user in other otherSession
        otherSession.put(SESSION_USER_HOLDER_KEY, userHolder)
        otherSession.put(PAC4J_CAS_TICKET, TEST_CAS_TICKET)

        store.set(TEST_CAS_TICKET, otherSession.id())
        val handler = VertxCasLogoutHandler(store, false, ::VertxProfileManager)

        handler.destroySessionBack(webContext, TEST_CAS_TICKET)
        assertThat(otherSession.get<Any>(SESSION_USER_HOLDER_KEY), `is`(nullValue()))
        assertThat(otherSession.get(PAC4J_CAS_TICKET), `is`(nullValue()))
    }

    @Test
    @Throws(Exception::class)
    fun renewSession() {

        val handler = VertxCasLogoutHandler(store, false, ::VertxProfileManager)
        val otherSession = otherSession()
        // Put pac4j ticket in other otherSession
        otherSession.put(PAC4J_CAS_TICKET, TEST_CAS_TICKET)
        store.set(TEST_CAS_TICKET, otherSession.id())

        // point current webcontext session to the ticket
        vertxSession!!.put(PAC4J_CAS_TICKET, TEST_CAS_TICKET)

        handler.renewSession(otherSession.id(), webContext)
        assertThat(otherSession.get(PAC4J_CAS_TICKET), `is`(nullValue()))
        assertThat(store.get(TEST_CAS_TICKET).toString(), `is`(vertxSession!!.id()))
        assertThat(vertxSession!!.get(PAC4J_CAS_TICKET), `is`(TEST_CAS_TICKET))
    }

    // Create a session not on current web context to allow for renewal
    private fun otherSession(): Session {
        // Set up another otherSession to which cas ticket will point
        val otherSessionFuture: CompletableFuture<Void> = CompletableFuture()
        val otherSession = vertxSessionStore.createSession(20000)
        vertxSessionStore.put(otherSession, {
            if (it.succeeded()) {
                otherSessionFuture.complete(null)
            } else {
                otherSessionFuture.completeExceptionally(it.cause())
            }
        })
        otherSessionFuture.get(1, TimeUnit.SECONDS)
        return otherSession
    }


    private fun mockRequest(): HttpServerRequest {
        val emptyMap = emptyMultimap()
        return mock {
            on { method() } doReturn HttpMethod.GET
            on { absoluteURI() } doReturn "http://localhost:8080/logout"
            on { remoteAddress() } doReturn SocketAddressImpl(12334, "localhost")
            on { headers() } doReturn emptyMap
            on { params() } doReturn emptyMap
        }
    }

    private fun emptyMultimap(): MultiMap {
        val map = mock<MultiMap> {
            on { names() } doReturn HashSet()
        }
        return map
    }

}