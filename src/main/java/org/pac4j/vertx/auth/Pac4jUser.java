package org.pac4j.vertx.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import org.pac4j.core.profile.UserProfile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class Pac4jUser extends AbstractUser {

    private final Map<String, UserProfile> profiles = new LinkedHashMap<>();
    private JsonObject principal;

    public Pac4jUser() {
        // I think this noop default constructor is required for deserialization from a clustered session
    }

    @Override
    protected void doIsPermitted(String permission, Handler<AsyncResult<Boolean>> resultHandler) {

        /*
         * Assume permitted if any profile is permitted
         */
        resultHandler.handle(Future.succeededFuture(
            profiles.values().stream()
                .anyMatch(p -> p.getPermissions().contains(permission))
        ));

    }

    @Override
    public JsonObject attributes() {
        return null;
    }

    @Override
    public User isAuthorized(Authorization authorization, Handler<AsyncResult<Boolean>> handler) {
        return null;
    }

    @Override
    public JsonObject principal() {
        return principal;
    }

    @Override
    public void setAuthProvider(AuthProvider authProvider) {
    }

    public Map<String, UserProfile> pac4jUserProfiles() {
        return profiles;
    }

    public void setUserProfiles(final Map<String, UserProfile> userProfiles) {
        Objects.requireNonNull(userProfiles);
        profiles.clear();
        profiles.putAll(userProfiles);
        updatePrincipal();
    }

    /**
     * Update the principal, to be called on any modification of the profiles map internally.
     */
    private void updatePrincipal() {

        principal = new JsonObject();
        profiles.forEach((name, profile) -> {
            final JsonObject jsonProfile = new JsonObject();
            profile.getAttributes()
                    .forEach((attributeName, attributeValue) ->
                            jsonProfile.put(attributeName, attributeValue.toString()));
            principal.put(name, jsonProfile);
        });
    }
}
