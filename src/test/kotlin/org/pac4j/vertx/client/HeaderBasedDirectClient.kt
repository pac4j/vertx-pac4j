package org.pac4j.vertx.client

import org.pac4j.core.client.DirectClient
import org.pac4j.core.context.WebContext
import org.pac4j.vertx.handler.impl.HEADER_EMAIL
import org.pac4j.vertx.handler.impl.HEADER_USER_ID
import org.pac4j.vertx.profile.SimpleTestProfile
import javax.security.auth.login.CredentialException

/**
 * Simple client to be used for the direct multi-profile integration test, authenticates based on an Authentication
 * header matching a specific pattern, extracts user id and email and writes them into a TestCredentuals object
 * @since 2.0.0
 */
class HeaderBasedDirectClient(matchingPattern: String): DirectClient<TestCredentials, SimpleTestProfile>() {

    override fun clientInit(context: WebContext?) {
        // Don't think this needs to do anything special
    }

    init {
        val matchingRegExp = Regex(matchingPattern)
        name = matchingPattern.toLowerCase()
        setCredentialsExtractor { context ->
            val authHeader = context.getRequestHeader("Authorization") ?: ""
            if (matchingRegExp.containsMatchIn(authHeader)) TestCredentials(context.getRequestHeader(HEADER_USER_ID),
                    context.getRequestHeader(HEADER_EMAIL)) else null

        }
        setAuthenticator { credentials, webContext ->
            if (credentials == null || credentials.userId == null || credentials.email == null) {
                throw CredentialException("Authorization header does not pass authentication")
            }
        }
        setProfileCreator { credentials, webContext ->  SimpleTestProfile(credentials.userId, credentials.email)}
    }
}