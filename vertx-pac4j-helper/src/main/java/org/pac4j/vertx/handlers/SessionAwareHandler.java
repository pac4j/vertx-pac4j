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

import org.pac4j.vertx.Constants;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.campudus.vertx.sessionmanager.java.SessionHelper;

/**
 * <p>Wrapper handler providing all session attributes for the next handler in the chain.</p>
 * <p>This handler uses the sessionHelper from campudus for session creation, attributes saving and retrieval.
 * It is the next handler responsibility to save the updated session attributes by calling the saveSessionAttributes method.</p>
 * <p>The stateless field indicates wether we should really relies on a session management system:
 * <ul>
 * <li>stateless = false requires a valid sessionHelper and a deployed session manager module</li>
 * <li>stateless = true does not require any sessionHelper neither a session manager module</li>
 * </ul>
 * </p>
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public abstract class SessionAwareHandler implements Handler<HttpServerRequest> {

    private static final String DATA_ATTRIBUTE = "data";

    private static final String SESSION_ATTRIBUTES = "session_attributes";

    private boolean stateless = false;

    protected SessionHelper sessionHelper;

    public SessionAwareHandler(boolean stateless) {
        this.stateless = stateless;
    }

    public SessionAwareHandler(SessionHelper sessionHelper) {
        this.sessionHelper = sessionHelper;
    }

    @Override
    public void handle(final HttpServerRequest req) {

        if (isStateless()) {
            doHandle(req, null, new JsonObject());
        } else {
            sessionHelper.withSessionData(req, new JsonArray(new Object[] { SESSION_ATTRIBUTES }),
                    new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject event) {
                            if (Constants.ERROR_STATUS.equals(event.getString(Constants.STATUS_ATTRIBUTE))
                                    && "SESSION_GONE".equals(event.getString(Constants.ERROR_STATUS))) {
                                sessionHelper.startSession(req, new Handler<String>() {

                                    @Override
                                    public void handle(String sessionId) {
                                        doHandle(req, sessionId, new JsonObject());
                                    }
                                });
                            } else if (Constants.SUCCESS_STATUS.equals(event.getString(Constants.STATUS_ATTRIBUTE))) {
                                JsonObject data = event.getObject(DATA_ATTRIBUTE).getObject(SESSION_ATTRIBUTES);
                                if (data == null) {
                                    data = new JsonObject();
                                }
                                doHandle(req, sessionHelper.getSessionId(req), data);
                            }
                        }
                    });
        }

    }

    protected void saveSessionAttributes(String sessionId, JsonObject sessionAttributes, Handler<JsonObject> handler) {
        sessionHelper.putSessionData(sessionId, new JsonObject().putObject(SESSION_ATTRIBUTES, sessionAttributes),
                handler);
    }

    protected boolean isStateless() {
        return stateless;
    }

    protected abstract void doHandle(HttpServerRequest req, String sessionId, JsonObject sessionAttributes);

}
