package org.pac4j.vertx.auth;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.core.DefaultJsonConverter;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import static java.util.stream.Collectors.toMap;

public class Pac4JUserProfiles extends LinkedHashMap<String, CommonProfile> implements ClusterSerializable {

    public Pac4JUserProfiles() {
        super();
    }

    public Pac4JUserProfiles(Object profiles) {
        super();
        putAll((LinkedHashMap<String, CommonProfile>)profiles);
    }

    @Override
    public void writeToBuffer(Buffer buff) {
        final JsonObject profilesAsJson = new JsonObject();
        this.forEach((name, profile) -> {
            final JsonObject profileAsJson = (JsonObject) DefaultJsonConverter.getInstance().encodeObject(profile);
            profilesAsJson.put(name, profileAsJson);
        });

        final String json = profilesAsJson.toString();
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        buff.appendInt(jsonBytes.length).appendBytes(jsonBytes);
    }

    @Override
    public int readFromBuffer(int i, Buffer buffer) {
        int posLocal = i;
        final int jsonByteCount = buffer.getInt(posLocal);
        posLocal += 4;
        final byte[] jsonBytes = buffer.getBytes(posLocal, posLocal + jsonByteCount);
        posLocal += jsonByteCount;

        final String json = new String(jsonBytes, StandardCharsets.UTF_8);
        final JsonObject profiles = new JsonObject(json);

        this.clear();
        this.putAll(profiles.stream()
                .filter(e -> e.getValue() instanceof JsonObject)
                .map(e -> new MappedPair<>(e.getKey(),
                        (CommonProfile) DefaultJsonConverter.getInstance().decodeObject(e.getValue())))
                .collect(toMap(e -> e.key, e -> e.value)));
        return posLocal;
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
