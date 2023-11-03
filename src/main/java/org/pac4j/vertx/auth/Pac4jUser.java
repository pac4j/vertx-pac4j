package org.pac4j.vertx.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.impl.UserImpl;
import org.pac4j.core.profile.UserProfile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class Pac4jUser extends UserImpl implements User, ClusterSerializable {

    private final Map<String, UserProfile> profiles = new LinkedHashMap<>();
    private JsonObject principal;

    public Pac4jUser() {
        // I think this noop default constructor is required for deserialization from a clustered session

        // Initialize fields to non-null values to avoid io.vertx.ext.auth.impl.UserConverter from thowing NPE
        super(new JsonObject(), new JsonObject());
    }

    @Override
    public User isAuthorized(Authorization authorization, Handler<AsyncResult<Boolean>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(
                profiles.values().stream()
                        .anyMatch(p -> p.getRoles().contains(authorization))
        ));
        return this;
    }

    @Override
    public JsonObject principal() {
        return principal;
    }

    @Override
    public void setAuthProvider(AuthProvider authProvider) {
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
