package org.pac4j.vertx.handler.impl

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import org.hamcrest.CoreMatchers.`is`
import org.junit.Test
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.HttpConstants
import org.pac4j.core.matching.ExcludedPathMatcher
import org.pac4j.vertx.StatelessPac4jAuthHandlerIntegrationTestBase
import org.pac4j.vertx.auth.Pac4jAuthProvider
import org.pac4j.vertx.client.HeaderBasedDirectClient
import java.util.*
import java.util.function.Consumer

/**
 * Simple integration test for testing direct client multi-profile behaviour in vertx-pac4j
 * @since 2.0.0
 */
class StatelessMultiProfileIntegrationTest: StatelessPac4jAuthHandlerIntegrationTestBase(), MultiProfileTest {

    @Test
    fun testTwoOfThreeMatch() {

        fun validateUserIdAndEmailForJsonProfile(jsonProfile: JsonObject) {
            assertThat(jsonProfile.getString(FIELD_USER_ID), `is`(TEST_USER1))
            assertThat(jsonProfile.getString(FIELD_EMAIL), `is`(TEST_EMAIL))
        }

        startWebServer()
        // Need to set userId and email headers
        val headers = mapOf(HEADER_AUTHORIZATION to "ABCGHI", HEADER_USER_ID to TEST_USER1, HEADER_EMAIL to TEST_EMAIL)
        testProtectedResourceAccessWithHeaders(headers, HttpConstants.OK, {
            body -> validateProfilesInBody(body, listOf(
                Pair("abc", Consumer(::validateUserIdAndEmailForJsonProfile)),
                Pair("ghi", Consumer(::validateUserIdAndEmailForJsonProfile))
            ))
        })
    }

    @Test
    fun testNoMatches() {
        startWebServer()
        // Need to set userId and email headers
        val headers = mapOf(HEADER_AUTHORIZATION to "AAAAAAA", HEADER_USER_ID to TEST_USER1, HEADER_EMAIL to TEST_EMAIL)
        testProtectedResourceAccessWithHeaders(headers, HttpConstants.UNAUTHORIZED, { })
    }

    @Throws(Exception::class)
    override fun startWebServer() {

        val router = Router.router(vertx)
        // Configure a pac4j stateless handler configured for basic http auth
        val authProvider = Pac4jAuthProvider()
        val options = SecurityHandlerOptions().setClients("abc,def,ghi").setMultiProfile(true)
        val handler = SecurityHandler(vertx, sessionStore, config(), authProvider, options)
        startWebServer(router, handler)

    }

    private fun config(): Config {
        val clients = Clients(HeaderBasedDirectClient("ABC"),
                HeaderBasedDirectClient("DEF"),
                HeaderBasedDirectClient("GHI"))
        val config = Config(clients, authorizers(ArrayList<String>()))
        config.setMatcher(ExcludedPathMatcher("^/private/public/.*$"))
        return config
    }

}