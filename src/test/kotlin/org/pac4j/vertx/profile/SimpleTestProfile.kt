package org.pac4j.vertx.profile

import org.pac4j.core.profile.CommonProfile
import org.pac4j.vertx.handler.impl.FIELD_EMAIL
import org.pac4j.vertx.handler.impl.FIELD_USER_ID

/**
 * Simple profile type for testing
 */
class SimpleTestProfile(userId: String?, email: String?): CommonProfile() {

    init {
        super.setId(userId)
        super.addAttribute(FIELD_EMAIL, email)
        super.addAttribute(FIELD_USER_ID, userId)
    }

}