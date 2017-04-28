package org.pac4j.core.profile;

import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.vertx.context.session.Session;

import java.util.LinkedHashMap;

/**
 *
 */
public class ExtendedProfileManager<U extends CommonProfile> extends ProfileManager<U> {

    public ExtendedProfileManager(WebContext context) {
        super(context);
    }

    public void removeFromSession(final Session session) {
        // Remove profiles from the specified session, which may not be the one used for the current WebContext
        // (for example back channel logout)
        session.set(Pac4jConstants.USER_PROFILES, new LinkedHashMap<String, U>());
    }
}
