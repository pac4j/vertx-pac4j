package org.pac4j.vertx.core.engine;

import org.pac4j.core.engine.DefaultCallbackLogic;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxCallbackLogic extends DefaultCallbackLogic<Void, VertxWebContext> {

    @Override
    protected void saveUserProfile(final VertxWebContext context, final CommonProfile profile,
                                   final boolean multiProfile, final boolean renewSession) {
        final ProfileManager manager = new VertxProfileManager(context);
        if (profile != null) {
            manager.save(true, profile, multiProfile);
            if (renewSession) {
                renewSession(context);
            }
        }
    }


}
