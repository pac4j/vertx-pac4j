package org.pac4j.vertx.client

import org.pac4j.core.client.IndirectClient
import org.pac4j.core.context.WebContext
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.credentials.extractor.CredentialsExtractor
import org.pac4j.core.profile.creator.ProfileCreator
import org.pac4j.core.redirect.RedirectAction
import org.pac4j.core.redirect.RedirectActionBuilder
import org.pac4j.vertx.handler.impl.CENTRAL_LOGOUT_URL
import org.pac4j.vertx.handler.impl.QUERY_PARAM_CLIENT_NAME
import org.pac4j.vertx.handler.impl.QUERY_PARAM_EMAIL
import org.pac4j.vertx.handler.impl.QUERY_PARAM_USER_ID
import org.pac4j.vertx.profile.SimpleTestProfile
import java.util.*

/**
 * A spoof indirect client which generates a profile based on the values of specific query parameters passed into the
 * request and the redirection to the callback. Note that for now this only really implements callback and the ability
 * to generate a callback url from a context by passing in the query params provided.
 *
 * This is not in any way a secure client and is only intended for automated test purposes to provide an easy route to
 * logging in with a second profile for multi-profile testing.
 */
class QueryParamBasedIndirectClient(val server: String, val callbackPath: String):
        IndirectClient<TestCredentials, SimpleTestProfile>() {

    override fun clientInit(context: WebContext?) {
        // Don't think this needs to do anything special
    }

    init {
        credentialsExtractor = CredentialsExtractor { context ->
            TestCredentials(context.getRequestParameter(QUERY_PARAM_USER_ID),
                    context.getRequestParameter(QUERY_PARAM_EMAIL)) }
        authenticator = Authenticator { credentials, webContext ->
            Objects.requireNonNull(credentials.userId)
            Objects.requireNonNull(credentials.email)
        }
        profileCreator = ProfileCreator { credentials, webContext ->
            SimpleTestProfile(credentials.userId, credentials.email)}
        redirectActionBuilder = RedirectActionBuilder { webContext ->

            val clientNameKV = "$QUERY_PARAM_CLIENT_NAME=${webContext.getRequestParameter(QUERY_PARAM_CLIENT_NAME)}"
            val userIdKV = "$QUERY_PARAM_USER_ID=${webContext.getRequestParameter(QUERY_PARAM_USER_ID)}"
            val emailKV = "$QUERY_PARAM_EMAIL=${webContext.getRequestParameter(QUERY_PARAM_EMAIL)}"
            val location = "$server$callbackPath?$clientNameKV&$userIdKV&$emailKV"
            RedirectAction.redirect(location)
        }
        setLogoutActionBuilder { context, currentProfile, targetUrl ->
            RedirectAction.redirect(CENTRAL_LOGOUT_URL)
        }

    }

}