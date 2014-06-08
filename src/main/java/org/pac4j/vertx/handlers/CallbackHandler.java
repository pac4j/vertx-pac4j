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

import org.apache.commons.lang3.StringUtils;
import org.pac4j.vertx.Config;
import org.pac4j.vertx.Constants;
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
 * Callback handler for Vert.x pac4j binding. This handler finishes the authentication process.
 * <br>
 * This handler is in two parts:
 * <ul>
 * <li>the real handler which is called either directly if there's no data in the request (e.g. GET)
 *  or by the request endHanlder otherwise (e.g. POST)</li>
 *  <li>the handler which makes the decision based on the HTTP method and the Content-Type header</li>
 *  </ul>
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class CallbackHandler implements Handler<HttpServerRequest> {

    protected static final Logger logger = LoggerFactory.getLogger(CallbackHandler.class);

    private final Handler<HttpServerRequest> handler;

    public CallbackHandler(Pac4jHelper pac4jHelper, SessionHelper sessionHelper) {
        this.handler = new CBHandler(pac4jHelper, sessionHelper);
    }

    @Override
    public void handle(final HttpServerRequest req) {
        // get form urlencoded data
        String contentType = req.headers().get(Constants.CONTENT_TYPE_HEADER);
        if ("POST".equals(req.method()) && contentType != null
                && Constants.FORM_URLENCODED_CONTENT_TYPE.equals(contentType)) {
            req.expectMultiPart(true);
            req.params().add(Constants.FORM_ATTRIBUTES, "true");
            req.endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    handler.handle(req);
                }
            });
        } else {
            handler.handle(req);
        }
    }

    private static class CBHandler extends SessionAwareHandler {

        private final Pac4jHelper pac4jHelper;

        public CBHandler(Pac4jHelper pac4jHelper, SessionHelper sessionHelper) {
            super(sessionHelper);
            this.pac4jHelper = pac4jHelper;
        }

        @Override
        protected void doHandle(final HttpServerRequest req, final String sessionId, final JsonObject sessionAttributes) {
            this.pac4jHelper.authenticate(req, sessionAttributes, new Handler<Message<JsonObject>>() {

                @Override
                public void handle(final Message<JsonObject> msg) {
                    final JsonObject response = msg.body();
                    JsonObject sessionAttributes = pac4jHelper.getSessionAttributes(response);
                    if (pac4jHelper.isRequiresHttpAction(response)) {
                        saveSessionAttributes(sessionId, sessionAttributes, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject event) {
                                pac4jHelper.sendResponse(req.response(), response);
                            }
                        });
                    }
                    Object userProfile = pac4jHelper.getUserProfile(response);
                    if (userProfile != null) {
                        sessionAttributes.putValue(Constants.USER_PROFILE, userProfile);
                    }
                    final String requestedUrl = sessionAttributes.getString(Constants.REQUESTED_URL);
                    sessionAttributes.putString(Constants.REQUESTED_URL, null);
                    final String redirectUrl = defaultUrl(requestedUrl, Config.getDefaultSuccessUrl());

                    saveSessionAttributes(sessionId, sessionAttributes, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject event) {
                            HttpResponseHelper.redirect(req, redirectUrl);
                        }
                    });
                }
            });
        }
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
        if (StringUtils.isNotBlank(url)) {
            redirectUrl = url;
        }
        logger.debug("defaultUrl : {}", redirectUrl);
        return redirectUrl;
    }

}
