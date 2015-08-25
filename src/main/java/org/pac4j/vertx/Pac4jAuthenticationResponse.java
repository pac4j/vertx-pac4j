package org.pac4j.vertx;

import org.pac4j.core.profile.UserProfile;

import java.util.Optional;

/**
 * @author jez
 */
public class Pac4jAuthenticationResponse extends Pac4jResponse {

  UserProfile profile = null;

  public Pac4jAuthenticationResponse(VertxWebContext webContext) {
    super(webContext);
  }

  public Pac4jAuthenticationResponse(final VertxWebContext webContext, final UserProfile userProfile) {
    super(webContext);
    profile = userProfile;
  }

  public void setProfile(UserProfile profile) {
    this.profile = profile;
  }

  public Optional<UserProfile> getProfile() {
    return Optional.ofNullable(profile);
  }
}
