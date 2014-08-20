/*
  Copyright 2014 - 2014 Michael Remond

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
package org.pac4j.vertx;

import org.pac4j.core.context.HttpConstants;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * Helper class to generate some basic http responses.
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class HttpResponseHelper {

    public static void ok(HttpServerRequest req, String content) {
        req.response().setStatusCode(HttpConstants.OK);
        req.response().headers().add(HttpConstants.CONTENT_TYPE_HEADER, HttpConstants.HTML_CONTENT_TYPE);
        req.response().end(content);
    }

    public static void redirect(HttpServerRequest req, String location) {
        req.response().setStatusCode(HttpConstants.TEMP_REDIRECT);
        if (location != null) {
            req.response().putHeader(HttpConstants.LOCATION_HEADER, location);
        }
        req.response().end();
    }

    public static void redirect(HttpServerRequest req) {
        redirect(req, null);
    }

    public static void unauthorized(HttpServerRequest req, String page) {
        req.response().setStatusCode(HttpConstants.UNAUTHORIZED);
        req.response().end(page);
    }

    public static void forbidden(HttpServerRequest req, String page) {
        req.response().setStatusCode(HttpConstants.FORBIDDEN);
        req.response().end(page);
    }

}
