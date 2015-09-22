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
package org.pac4j.vertx.http;

import org.pac4j.core.client.RedirectAction;
import org.pac4j.vertx.VertxWebContext;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public interface HttpActionAdapter {

  /**
   * Handle HTTP action.
   *
   * @param code the HTTP status code
   * @param context the web context
   * @return void
   */
  void handle(final int code, final VertxWebContext context);

  /**
   * Handle HTTP action for redirection use cases.
   *
   * @param action the pac4j action to perform
   * @return void
   */
  void handleRedirect(final RedirectAction action, final VertxWebContext webContext);
}
