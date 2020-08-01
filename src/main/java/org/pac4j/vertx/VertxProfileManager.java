package org.pac4j.vertx;

import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.vertx.auth.Pac4JUserProfiles;
import org.pac4j.vertx.auth.Pac4jUser;
import org.pac4j.vertx.context.session.VertxSessionStore;

import java.util.LinkedHashMap;
import java.util.Optional;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxProfileManager extends ProfileManager<CommonProfile> {

    private final VertxWebContext vertxWebContext;

    public VertxProfileManager(final VertxWebContext context) {
        super(context);
        this.vertxWebContext = context;
    }

    public VertxProfileManager(final VertxWebContext context, final VertxSessionStore sessionStore) {
        super(context, sessionStore);
        this.vertxWebContext = context;
    }

    @Override
    protected void saveAll(final LinkedHashMap<String, CommonProfile> profiles, final boolean saveInSession) {
        super.saveAll(profiles, saveInSession);

        final Pac4jUser vertxUser = Optional.ofNullable(vertxWebContext.getVertxUser()).orElse(new Pac4jUser());
        vertxUser.setUserProfiles(profiles);
        vertxWebContext.setVertxUser(vertxUser);
    }

    @Override
    public void remove(boolean removeFromSession) {
        super.remove(removeFromSession);

        vertxWebContext.removeVertxUser();
    }
}
