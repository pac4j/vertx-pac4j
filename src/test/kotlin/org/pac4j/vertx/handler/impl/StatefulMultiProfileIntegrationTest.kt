package org.pac4j.vertx.handler.impl

import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Test
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.Pac4jConstants
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.exception.HttpAction
import org.pac4j.core.exception.TechnicalException
import org.pac4j.vertx.StatefulPac4jAuthHandlerIntegrationTestBase
import org.pac4j.vertx.VertxWebContext
import org.pac4j.vertx.client.QueryParamBasedIndirectClient
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.hamcrest.core.Is.`is` as isEqualTo


/**
 * Simple integration test for multi profile stateful login behaviour. Emulate successful login on a stateful
 * client then force a login on a subsequent client, finally examining the resultant pac4j user's profiles to
 * ensure that both are present.
 */
class StatefulMultiProfileIntegrationTest: StatefulPac4jAuthHandlerIntegrationTestBase(), MultiProfileTest {

    @Test
    fun testMultiProfileSecondLoginWorksAsExpected() {

        startOAuthProviderMimic("testOAuth1User")
        startWebServer(TEST_OAUTH2_SUCCESS_URL, optionsWithBothNamesProvided(), callbackHandlerOptions(), null,
                { router, config ->
                    router.route(HttpMethod.GET, "/forceSecondLogin").handler(forceLoginHandler(config, sessionStore))
                })

        // First login successfully following approach for single profile oAuth2 test client
        // Start a web server with no required authorities (i.e. only authentication required) for the secured resource
        val client = vertx.createHttpClient()
        loginSuccessfullyExpectingAuthorizedUser(client) { Void ->

            // now force a login
            val forceLoginRequest = client.get(8080, "localhost",
                    "/forceSecondLogin?$QUERY_PARAM_CLIENT_NAME=$TEST_QUERY_PARAM_CLIENT_NAME" +
                            "&$QUERY_PARAM_USER_ID=$TEST_USER1&$QUERY_PARAM_EMAIL=$TEST_EMAIL" +
                            "&$QUERY_PARAM_REDIRECT_URI=${URLEncoder.encode(successResourceUrl(), "UTF-8")}")
            sessionCookie.ifPresent({ cookie -> forceLoginRequest.putHeader("cookie", cookie) })
            forceLoginRequest.handler(expectAndHandleRedirect(client,
                    Consumer {},
                    expectAndHandleRedirect(client, {},
                    { resp ->
                        resp.bodyHandler {
                            body ->
                                try {
                                    validateProfilesInBody(body,
                                            listOf(
                                                    Pair(OAUTH2_CLIENT_NAME, Consumer { child ->
                                                        assertThat<Any>(child.getString(FIELD_ACCESS_TOKEN),
                                                                isEqualTo<Any>(notNullValue()))
                                                    }),
                                                    Pair(TEST_QUERY_PARAM_CLIENT_NAME, Consumer { child ->
                                                        assertThat<Any>(child.getString(FIELD_EMAIL),
                                                                isEqualTo<Any>(TEST_EMAIL)) })
                                            ))

                                    testComplete()
                                } catch (ex: Exception) {
                                    fail(ex)
                                }
                        }
                    })))
                    .end()

        }
        await(10, TimeUnit.SECONDS)

    }

    private fun queryParamBasedIndirectClient(server: String, callbackPath: String): QueryParamBasedIndirectClient {
        val client = QueryParamBasedIndirectClient(server, callbackPath)
        client.name = TEST_QUERY_PARAM_CLIENT_NAME
        return client
    }

    private fun successResourceUrl() = "http://$HOST:$PORT$PROTECTED_RESOURCE_SUCCESS"

    private fun forceLoginHandler(config: Config, sessionStore: SessionStore<VertxWebContext>): Handler<RoutingContext> = Handler { rc ->
        val context = VertxWebContext(rc, sessionStore)
        val client = config.clients.findClient(context.getRequestParameter(Clients.DEFAULT_CLIENT_NAME_PARAMETER))
        try {
            val redirectUri = context.getRequestParameter("redirect_uri")
            context.setSessionAttribute(Pac4jConstants.REQUESTED_URL, redirectUri)
            val action = client.redirect(context)
            config.httpActionAdapter.adapt(action.code, context)
        } catch (e: HttpAction) {
            throw TechnicalException(e)
        }
    }


    override fun callbackHandlerOptions(): CallbackHandlerOptions {
        return CallbackHandlerOptions().setDefaultUrl(Pac4jConstants.DEFAULT_URL)
                .setMultiProfile(true)
    }

    override fun clients(baseAuthUrl: String): Clients {
        return Clients(oAuth2Client(baseAuthUrl), queryParamBasedIndirectClient("http://localhost:8080", "/authResult"))
    }
}