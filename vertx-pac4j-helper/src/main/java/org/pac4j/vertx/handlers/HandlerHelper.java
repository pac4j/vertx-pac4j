/*
  Copyright 2014 - 2014 pac4j organization

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
package org.pac4j.vertx.handlers;

import org.pac4j.core.context.HttpConstants;
import org.pac4j.vertx.Constants;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * Callback handler for Vert.x pac4j binding. This handler finishes the stateful authentication process.
 * 
 * @author Michael Remond
 * @since 1.1.0
 *
 */
public class HandlerHelper {

    /**
     * Add form parsing capabilities to the wrapped handler.
     * 
     * @param toWrap
     * @return
     */
    public static Handler<HttpServerRequest> addFormParsing(final Handler<HttpServerRequest> toWrap) {
        return new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                // get form urlencoded data
                String contentType = req.headers().get(HttpConstants.CONTENT_TYPE_HEADER);
                if ("POST".equals(req.method()) && contentType != null
                        && Constants.FORM_URLENCODED_CONTENT_TYPE.equals(contentType)) {
                    req.expectMultiPart(true);
                    req.params().add(Constants.FORM_ATTRIBUTES, "true");
                    req.endHandler(new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            toWrap.handle(req);
                        }
                    });
                } else {
                    toWrap.handle(req);
                }
            }
        };
    }

}
