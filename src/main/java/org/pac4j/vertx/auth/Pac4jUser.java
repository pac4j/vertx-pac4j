package org.pac4j.vertx.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.core.DefaultJsonConverter;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class Pac4jUser extends AbstractUser {

    private final LinkedHashMap<String, CommonProfile> profiles = new LinkedHashMap<>();
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
    public JsonObject principal() {
        return principal;
    }

    @Override
    public void setAuthProvider(AuthProvider authProvider) {
    }

    @Override
    public void writeToBuffer(Buffer buff) {
        super.writeToBuffer(buff);
        // Now write the remainder of our stuff to the buffer;
        final JsonObject profilesAsJson = new JsonObject();
        profiles.forEach((name, profile) -> {
            final JsonObject profileAsJson = (JsonObject) DefaultJsonConverter.getInstance().encodeObject(profile);
            profilesAsJson.put(name, profileAsJson);
        });

        final String json = profilesAsJson.toString();
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        buff.appendInt(jsonBytes.length)
            .appendBytes(jsonBytes);

    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) {
        int posLocal = super.readFromBuffer(pos, buffer);
        final int jsonByteCount = buffer.getInt(posLocal);
        posLocal += 4;
        final byte[] jsonBytes = buffer.getBytes(posLocal, posLocal + jsonByteCount);
        posLocal += jsonByteCount;

        final String json = new String(jsonBytes, StandardCharsets.UTF_8);
        final JsonObject profiles = new JsonObject(json);

        final Map<String, CommonProfile> decodedUserProfiles = profiles.stream()
                .filter(e -> e.getValue() instanceof JsonObject)
                .map(e -> new MappedPair<>(e.getKey(),
                        (CommonProfile) DefaultJsonConverter.getInstance().decodeObject(e.getValue())))
                .collect(toMap(e -> e.key, e -> e.value));

        setUserProfiles(decodedUserProfiles);
        return posLocal;
    }

    public Map<String, CommonProfile> pac4jUserProfiles() {
        return profiles;
    }

    public void setUserProfile(final String clientName, final CommonProfile profile, final boolean multiProfile) {
        if (!multiProfile) {
            profiles.clear();
        }
        profiles.put(clientName, profile);
        updatePrincipal();
    }

    private void setUserProfiles(final Map<String, CommonProfile> userProfiles) {

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

    private static class MappedPair<T, U> {
        public final T key;
        public final U value;

        public MappedPair(final T key, final U value) {
            this.key = key;
            this.value = value;
        }
    }
}
