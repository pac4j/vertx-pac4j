package org.pac4j.vertx.core.engine;

import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxSecurityLogic extends DefaultSecurityLogic<Void, VertxWebContext> {

    @Override
    protected ProfileManager getProfileManager(VertxWebContext context) {
        return new VertxProfileManager(context);
    }
}
