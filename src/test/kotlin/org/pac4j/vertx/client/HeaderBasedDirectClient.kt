package org.pac4j.vertx.client

import org.pac4j.core.client.DirectClient
import org.pac4j.vertx.HEADER_EMAIL
import org.pac4j.vertx.HEADER_USER_ID
import org.pac4j.vertx.profile.SimpleTestProfile
import javax.security.auth.login.CredentialException

/**
 * Simple client to be used for the direct multi-profile integration test, authenticates based on an Authentication
 * header matching a specific pattern, extracts user id and email and writes them into a TestCredentuals object
 * @since 2.0.0
 */
class HeaderBasedDirectClient(matchingPattern: String): DirectClient<TestCredentials, SimpleTestProfile>() {

    override fun clientInit() {
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
        setAuthenticator { credentials, _ ->
            if (credentials == null) {
                throw CredentialException("Authorization header does not pass authentication")
            }
        }
        setProfileCreator { credentials, _ ->  SimpleTestProfile(credentials.userId, credentials.email)}
    }
}