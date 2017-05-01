package org.pac4j.vertx;

import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ExtendedProfileManager;
import org.pac4j.vertx.auth.Pac4jUser;
import org.pac4j.vertx.context.session.Session;

import java.util.LinkedHashMap;
import java.util.Optional;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxProfileManager extends ExtendedProfileManager<CommonProfile> {

    private static final String SESSION_USER_HOLDER_KEY = "__vertx.userHolder";
    private final VertxWebContext vertxWebContext;

    public VertxProfileManager(final VertxWebContext context) {
        super(context);
        this.vertxWebContext = context;
    }

    @Override
    protected LinkedHashMap<String, CommonProfile> retrieveAll(final boolean readFromSession) {
        final LinkedHashMap<String, CommonProfile> profiles = new LinkedHashMap<>();
        final Pac4jUser user = vertxWebContext.getVertxUser();
        Optional.ofNullable(user).map(Pac4jUser::pac4jUserProfiles).ifPresent(profiles::putAll);
        return profiles;
    }

    @Override
    public void remove(boolean removeFromSession) {
        vertxWebContext.removeVertxUser();
    }

    @Override
    public void save(boolean saveInSession, CommonProfile profile, boolean multiProfile) {

        final String clientName = retrieveClientName(profile);
        final Pac4jUser vertxUser = Optional.ofNullable(vertxWebContext.getVertxUser()).orElse(new Pac4jUser());
        vertxUser.setUserProfile(clientName, profile, multiProfile);
        vertxWebContext.setVertxUser(vertxUser);
    }

    @Override
    public void removeFromSession(final Session session) {
        // Find the user profile in the session, if pac4j user profile
        session.remove(SESSION_USER_HOLDER_KEY);
    }
}
