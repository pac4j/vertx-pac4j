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

import org.pac4j.core.context.BaseConfig;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.vertx.HttpResponseHelper;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import com.campudus.vertx.sessionmanager.java.SessionHelper;

/**
 * Logout handler which remove the user profile from the session.
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class LogoutHandler extends SessionAwareHandler {

    public LogoutHandler(SessionHelper sessionHelper) {
        super(sessionHelper);
    }

    @Override
    protected void doHandle(final HttpServerRequest req, final String sessionId, final JsonObject sessionAttributes) {
        sessionAttributes.putValue(HttpConstants.USER_PROFILE, null);

        saveSessionAttributes(sessionId, sessionAttributes, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject event) {
                HttpResponseHelper.redirect(req, BaseConfig.getDefaultLogoutUrl());
            }
        });
    }

}
