package org.pac4j.vertx.client

import org.pac4j.core.credentials.Credentials

/**
 * Simple credentials class for use in tests
 */
class TestCredentials(val userId: String, val email: String): Credentials() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as TestCredentials

        if (userId != other.userId) return false
        if (email != other.email) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 7
        result = 31 * result + userId.hashCode()
        result = 31 * result + email.hashCode()
        return result
    }
}