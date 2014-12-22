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

import org.pac4j.core.context.BaseConfig;
import org.pac4j.vertx.AuthHttpServerRequest;
import org.pac4j.vertx.HttpResponseHelper;
import org.pac4j.vertx.Pac4jHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import com.campudus.vertx.sessionmanager.java.SessionHelper;

/**
 * Callback handler for Vert.x pac4j binding. This handler finishes the stateful authentication process.
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class CallbackHandler extends RequiresAuthenticationHandler {

    public CallbackHandler(String clientName, Handler<HttpServerRequest> delegate, Pac4jHelper pac4jHelper,
            SessionHelper sessionHelper) {
        super(clientName, delegate, pac4jHelper, sessionHelper);
    }

    public CallbackHandler(Pac4jHelper pac4jHelper, SessionHelper sessionHelper) {
        this(null, null, pac4jHelper, sessionHelper);
    }

    protected static final Logger logger = LoggerFactory.getLogger(CallbackHandler.class);

    @Override
    protected void retrieveUserProfile(HttpServerRequest req, String sessionId, JsonObject sessionAttributes,
            Handler<Message<JsonObject>> handler) {

        super.authenticate(req, sessionId, sessionAttributes, handler);
    }

    @Override
    protected void authenticationSuccess(AuthHttpServerRequest req, String sessionId, JsonObject sessionAttributes) {

        redirectToTarget(req, sessionId, sessionAttributes);
    }

    @Override
    protected void authenticationFailure(HttpServerRequest req, String sessionId, JsonObject sessionAttributes) {

        redirectToTarget(req, sessionId, sessionAttributes);
    }

    private void redirectToTarget(final HttpServerRequest req, String sessionId, JsonObject sessionAttributes) {

        String requestedUrl = retrieveOriginalUrl(req, sessionAttributes);
        saveUrl(null, sessionAttributes);
        final String redirectUrl = defaultUrl(requestedUrl, BaseConfig.getDefaultSuccessUrl());

        saveSessionAttributes(sessionId, sessionAttributes, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                HttpResponseHelper.redirect(req, redirectUrl);
            }
        });
    }

    /**
     * This method returns the default url from a specified url compared with a default url.
     * 
     * @param url
     * @param defaultUrl
     * @return the default url
     */
    public static String defaultUrl(final String url, final String defaultUrl) {
        String redirectUrl = defaultUrl;
        if (url != null && !"".equals(url)) {
            redirectUrl = url;
        }
        logger.debug("defaultUrl : {}", redirectUrl);
        return redirectUrl;
    }

}
