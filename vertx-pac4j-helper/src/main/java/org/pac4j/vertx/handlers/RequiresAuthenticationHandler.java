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
package org.pac4j.vertx.handlers;

import org.pac4j.core.context.HttpConstants;
import org.pac4j.vertx.Pac4jHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import com.campudus.vertx.sessionmanager.java.SessionHelper;

/**
 * Wrapper handler acting as security barrier. If the user is authenticated, the next handler in the chain is called.
 * Otherwise the user is redirected to the pac4j client security provider.
 * 
 *  The pac4j client to use is selected with the clientName attributes. 
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class RequiresAuthenticationHandler extends SessionAwareHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequiresAuthenticationHandler.class);

    private final String clientName;

    private final Boolean isAjax;

    private final Handler<HttpServerRequest> delegate;

    private final Pac4jHelper pac4jHelper;

    public RequiresAuthenticationHandler(String clientName, Handler<HttpServerRequest> delegate,
            Pac4jHelper pac4jHelper, SessionHelper sessionHelper) {
        this(clientName, false, delegate, pac4jHelper, sessionHelper);
    }

    public RequiresAuthenticationHandler(String clientName, boolean isAjax, Handler<HttpServerRequest> delegate,
            Pac4jHelper pac4jHelper, SessionHelper sessionHelper) {
        super(sessionHelper);
        this.pac4jHelper = pac4jHelper;
        this.clientName = clientName;
        this.delegate = delegate;
        this.isAjax = isAjax;
    }

    @Override
    protected void doHandle(final HttpServerRequest req, final String sessionId, final JsonObject sessionAttributes) {
        if (sessionAttributes.getValue(HttpConstants.USER_PROFILE) != null) {
            delegate.handle(req);
        } else {
            final String requestedUrlToSave = pac4jHelper.getFullRequestURL(req);
            sessionAttributes.putString(HttpConstants.REQUESTED_URL, requestedUrlToSave);
            logger.debug("requestedUrlToSave : {}", requestedUrlToSave);

            pac4jHelper.redirect(req, sessionAttributes, clientName, true, isAjax, new Handler<Message<JsonObject>>() {

                @Override
                public void handle(final Message<JsonObject> msg) {
                    final JsonObject response = msg.body();
                    JsonObject sessionAttributes = pac4jHelper.getSessionAttributes(response);

                    saveSessionAttributes(sessionId, sessionAttributes, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject event) {
                            pac4jHelper.sendResponse(req.response(), response);
                        }
                    });
                }
            });

        }

    }

}
