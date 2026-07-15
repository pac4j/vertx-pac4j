package org.pac4j.vertx.auth;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.ClusterSerializable;
import io.vertx.ext.auth.User;
import java.util.ArrayList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.pac4j.core.profile.UserProfile;

import java.util.Collection;
import java.util.List;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
@Getter
@EqualsAndHashCode
public class Pac4jUser implements User, ClusterSerializable {

  private final List<UserProfile> profiles;
  private final JsonObject principal;
  private final JsonObject attributes;

  public Pac4jUser(final Collection<UserProfile> profiles) {
    this.profiles = (profiles == null) ? new ArrayList<>() : new ArrayList<>(profiles);

    this.principal = new JsonObject().put("profilesCount", this.profiles.size());
    this.attributes = new JsonObject();
  }

  public Pac4jUser() {
    this(null);
  }

  // ----- io.vertx.ext.auth.User -----

  @Override
  public JsonObject principal() {
    return principal;
  }

  @Override
  public JsonObject attributes() {
    return attributes;
  }

  public List<UserProfile> profiles() {
      return profiles;
  }

  @Override
  public User merge(User other) {
    if (other == null) {
      return this;
    }

    if (other instanceof Pac4jUser) {
      this.profiles.addAll(((Pac4jUser) other).profiles);
      this.principal.put("profilesCount", this.profiles.size());
    }

    JsonObject otherPrinc = other.principal();
    if (otherPrinc != null) {
      for (String k : otherPrinc.fieldNames()) {
        this.principal.put(k, otherPrinc.getValue(k));
      }
    }

    JsonObject otherAttr = other.attributes();
    if (otherAttr != null) {
      for (String k : otherAttr.fieldNames()) {
        this.attributes.put(k, otherAttr.getValue(k));
      }
    }
    return this;
  }

  @Override
  public void writeToBuffer(final Buffer buffer) {
    final Buffer buf = Json.CODEC.toBuffer(this, false);
    buffer.appendInt(buf.length());
    buffer.appendBuffer(buf);
  }

  @Override
  public int readFromBuffer(final int pos, final Buffer buffer) {
    final int length = buffer.getInt(pos);
    final int start = pos + 4;
    final Buffer buf = buffer.getBuffer(start, start + length);
    merge(Json.CODEC.fromBuffer(buf, Pac4jUser.class));
    return pos + length + 4;
  }

}
