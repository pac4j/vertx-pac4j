/*
  Copyright 2015 - 2015 pac4j organization

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.pac4j.vertx.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.core.DefaultJsonConverter;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class Pac4jUser extends AbstractUser {

    private UserProfile userProfile;
    private JsonObject principal;

    public Pac4jUser() {
        // I think this noop default constructor is required for deserialization from a clustered session
    }

    public Pac4jUser(UserProfile userProfile) {
        setUserProfile(userProfile);
    }

    @Override
    protected void doIsPermitted(String permission, Handler<AsyncResult<Boolean>> resultHandler) {
        if (userProfile.getPermissions().contains(permission)) {
            resultHandler.handle(Future.succeededFuture(true));
        } else {
            resultHandler.handle(Future.succeededFuture(false));
        }
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
        final String json = principal.encodePrettily();
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        buff.appendInt(jsonBytes.length)
            .appendBytes(jsonBytes);

    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) {
        pos = super.readFromBuffer(pos, buffer);
        final int jsonByteCount = buffer.getInt(pos);
        pos += 4;
        final byte[] jsonBytes = buffer.getBytes(pos, pos + jsonByteCount);
        pos += jsonByteCount;
        final String json = new String(jsonBytes, StandardCharsets.UTF_8);
        final UserProfile userProfileLocal = (UserProfile) DefaultJsonConverter.getInstance().decodeObject(json);
        setUserProfile(userProfileLocal);
        return pos;
    }

    public UserProfile pac4jUserProfile() {
        return userProfile;
    }

    private final void setUserProfile(UserProfile profile) {

        Objects.requireNonNull(profile);
        this.userProfile = profile;
        principal = new JsonObject();
        userProfile.getAttributes().keySet().stream().forEach(key -> {
            principal.put(key, userProfile.getAttribute(key).toString());
        });
    }
}
