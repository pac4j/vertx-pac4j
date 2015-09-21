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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;
import org.pac4j.core.profile.UserProfile;

/**
 * @author jez
 */
public class Pac4jUser extends AbstractUser {

  private final UserProfile userProfile;

  public Pac4jUser(UserProfile userProfile) {
    this.userProfile = userProfile;
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
