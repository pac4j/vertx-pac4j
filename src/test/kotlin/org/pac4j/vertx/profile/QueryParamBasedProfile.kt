package org.pac4j.vertx.profile

import org.pac4j.core.profile.CommonProfile

/**
 * Simple profile type for testing
 */
class QueryParamBasedProfile(userId: String?, email: String?): CommonProfile() {

    init {
        super.setId(userId)
        super.addAttribute("email", email)
    }

}