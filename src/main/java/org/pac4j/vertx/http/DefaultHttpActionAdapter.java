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
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.http.HttpActionAdapter;
import org.pac4j.vertx.VertxWebContext;

import java.util.Optional;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class DefaultHttpActionAdapter implements HttpActionAdapter<Void, VertxWebContext> {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultHttpActionAdapter.class);

    @Override
    public Void adapt(final int code, final VertxWebContext context) {
        if (code == HttpConstants.UNAUTHORIZED) {
            sendFailureResponse(context, HttpConstants.UNAUTHORIZED);
        } else if (code == HttpConstants.FORBIDDEN) {
            sendFailureResponse(context, HttpConstants.FORBIDDEN);
        } else if (code == HttpConstants.TEMP_REDIRECT) {
            final Optional<String> location = getLocation(context);
            // This is clunkier than it should be due to Java 8 Optional limitation
            location.orElseThrow(() -> new TechnicalException("Redirect without a location header"));
            location.ifPresent(l -> redirect(l, context));
        } else if (code == HttpConstants.OK) {
            // Content should already have been written
            context.setResponseStatus(HttpConstants.OK);
            context.setResponseHeader(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.HTML_CONTENT_TYPE);
            context.completeResponse();
        } else {
            final String message = "Unsupported HTTP action: " + code;
            LOG.error(message);
            throw new TechnicalException(message);
        }
        return null;
    }

    private Optional<String> getLocation(final VertxWebContext webContext) {
        return Optional.ofNullable(webContext.getResponseHeaders().get(HttpConstants.LOCATION_HEADER));

    }

    protected void redirect(final String location, final VertxWebContext webContext) {
        webContext.setResponseStatus(HttpConstants.TEMP_REDIRECT);
        webContext.setResponseHeader(HttpConstants.LOCATION_HEADER, location);
        webContext.completeResponse();
    }

    protected void sendFailureResponse(final VertxWebContext webContext, final int code) {
        webContext.failResponse(code);
    }

}
