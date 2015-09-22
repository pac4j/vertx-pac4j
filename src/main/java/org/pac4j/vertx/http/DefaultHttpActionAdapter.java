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

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.vertx.VertxWebContext;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class DefaultHttpActionAdapter implements HttpActionAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultHttpActionAdapter.class);

  @Override
  public void handle(int code, VertxWebContext context) {

  }

  @Override
  public void handleRedirect(final RedirectAction action, final VertxWebContext webContext)
  {
    switch (action.getType()) {

      case REDIRECT:
        // Send a redirect response
        webContext.setResponseStatus(302);
        webContext.setResponseHeader("location", action.getLocation());
        webContext.completeResponse();
        break;

      case SUCCESS:
        break;

      default:
        throw new TechnicalException("Unsupported RedirectAction type: " + action.getType());
    }
  }
}
