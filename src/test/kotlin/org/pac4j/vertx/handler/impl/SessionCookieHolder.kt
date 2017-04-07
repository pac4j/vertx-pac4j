package org.pac4j.vertx.handler.impl

import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Supplier

/**
 *
 */
class SessionCookieHolder {

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(SessionCookieHolder::class.java)
    }

    private val sessionCookie = AtomicReference<String>()

    fun reset() {
        LOG.info("Resetting session cookie")
        sessionCookie.set(null)
    }

    fun retrieve(): Supplier<String?> = Supplier {
        LOG.info("Retrieving session cookie ${sessionCookie.get()}")
        sessionCookie.get()
    }

    fun persist(): Consumer<String> = Consumer {

        LOG.info("Session cookie is ${sessionCookie.get()}")
        // Only bother setting it if not already set
        if(sessionCookie.get() == null) {
            LOG.info("Setting session cookie $it")
            sessionCookie.set(it)
        }
    }


}