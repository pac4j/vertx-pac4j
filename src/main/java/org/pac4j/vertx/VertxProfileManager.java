package org.pac4j.vertx;

import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.vertx.auth.Pac4jUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxProfileManager<T extends CommonProfile> extends ProfileManager<T> {

    private VertxWebContext vertxWebContext;

    public VertxProfileManager(final VertxWebContext context) {
        super(context);
        this.vertxWebContext = context;
    }

    @Override
    public Optional<T> get(boolean readFromSession) {
        return Optional.ofNullable(vertxWebContext.getVertxUser())
                .map(user -> (T) user.pac4jUserProfile());
    }

    @Override
    public void remove(boolean removeFromSession) {
        vertxWebContext.removeVertxUser();
    }

    @Override
    public List<T> getAll(boolean readFromSession) {
        final ArrayList<T> profiles = new ArrayList<>(1);
        final Optional<T> profile = get(readFromSession);
        profile.ifPresent(profiles::add);
        return profiles;
    }

    // TODO: Multiprofile support
    @Override
    public void save(boolean saveInSession, T profile, boolean multiProfile) {
        final Pac4jUser vertxUser = new Pac4jUser(profile);
        vertxWebContext.setVertxUser(vertxUser);
    }

}
