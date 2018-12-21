package org.pac4j.vertx.context.session

import io.vertx.ext.web.Session
import io.vertx.ext.web.sstore.impl.SharedDataSessionImpl
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.pac4j.vertx.TEST_SESSION_KEY
import org.pac4j.vertx.TEST_SESSION_VALUE

/**
 *
 */
class VertxSessionTest {

    internal var vertxSession: Session? = null
    internal var pac4jSession: VertxSession? = null

    @Before
    fun setUp() {
        vertxSession = SharedDataSessionImpl()
        pac4jSession = VertxSession(vertxSession)
    }

    @Test
    @Throws(Exception::class)
    fun testDestroy() {
        pac4jSession!!.destroy()
        assertThat<Boolean>(vertxSession!!.isDestroyed, `is`<Boolean>(true))
    }

    @Test
    @Throws(Exception::class)
    fun set() {
        assertThat<Any>(vertxSession!!.get(TEST_SESSION_KEY), `is`(nullValue()))
        pac4jSession!!.set(TEST_SESSION_KEY, TEST_SESSION_VALUE)
        assertThat(vertxSession!!.get(TEST_SESSION_KEY), `is`(TEST_SESSION_VALUE))
    }

}