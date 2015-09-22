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
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

/**
 * Vert.x authprovider for pac4j libraries. In the case of this auth provider we will always
 * just delegate to pac4j via the handler, so this should be sufficient
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class Pac4jAuthProvider implements AuthProvider {
  @Override
  public void authenticate(JsonObject jsonObject, Handler<AsyncResult<User>> handler) {
    // We'll just let the handler delegate back to pac4j, we don't need to do any more than that
    handler.handle(Future.failedFuture("Delegate to pac4j subsystem for authentication"));
  }
}
