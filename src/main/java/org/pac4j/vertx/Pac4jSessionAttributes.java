package org.pac4j.vertx;

import org.pac4j.core.profile.UserProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author jez
 */
public class Pac4jSessionAttributes {

  private UserProfile userProfile = null;
  private final Map<String, Object> customAttributes;

  public Pac4jSessionAttributes() {
    this(new HashMap<>());
  }

  public Pac4jSessionAttributes(final Pac4jSessionAttributes that) {
    this.userProfile = that.userProfile;
    this.customAttributes = new HashMap<>(that.customAttributes);
  }

  public Pac4jSessionAttributes(final Map<String, Object> attributes) {
    Objects.requireNonNull(attributes);
    this.customAttributes = attributes;
  }

  public Pac4jSessionAttributes(final UserProfile userProfile) {
    customAttributes = new HashMap<>();
    this.userProfile = userProfile;
  }

  public Optional<UserProfile> getUserProfile() {
    return Optional.ofNullable(userProfile);
  }

  public void setUserProfile(UserProfile userProfile) {
    this.userProfile = userProfile;
  }

  public Map<String, Object> getCustomAttributes() {
    return customAttributes;
  }

}
