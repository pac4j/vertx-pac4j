package org.pac4j.vertx.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import org.pac4j.core.profile.UserProfile;

/**
 * @author jez
 */
public class Pac4jUser extends AbstractUser {

  private final UserProfile userProfile;
  private final Pac4jAuthProvider authProvider;

  public Pac4jUser(UserProfile userProfile, Pac4jAuthProvider authProvider) {
    this.userProfile = userProfile;
    this.authProvider = authProvider;
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
    return null;
  }

  @Override
  public void setAuthProvider(AuthProvider authProvider) {

  }

  public UserProfile pac4jUserProfile() {
    return userProfile;
  }
}
