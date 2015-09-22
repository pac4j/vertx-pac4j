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
package org.pac4j.vertx.flow;

import io.vertx.core.Vertx;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.DirectClient;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.VertxWebContext;

import java.util.function.Consumer;

/**
 * Interface representing an authentication flow, effectively wrapping the differences required
 * when initiating authentication for a direct or indirect client (the former actually needs to
 * carry out authentication, the latter initiates the authentication flow by issuing a redirect
 * response
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public interface AuthenticationFlow<T extends Client> {

  static AuthenticationFlow flowFor(final Vertx vertx, final Client client) {
    return (client instanceof IndirectClient) ?
      new IndirectClientAuthenticationFlow(vertx, (IndirectClient) client) :
      new DirectClientAuthenticationFlow(vertx, (DirectClient) client);
  }
  void initiate(final VertxWebContext webContext,
                final Consumer<UserProfile> authResultHandler);

  boolean useSession(final WebContext context);

}
