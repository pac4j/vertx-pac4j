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
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.AuthHttpServerRequest;
import org.pac4j.vertx.HttpResponseHelper;
import org.pac4j.vertx.Pac4jHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.impl.JsonObjectMessage;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import com.campudus.vertx.sessionmanager.java.SessionHelper;

/**
 * <p>Wrapper handler acting as a security barrier. If the user is authenticated, the next handler in the chain is called.
 * Otherwise the user is redirected to the pac4j client security provider if stateful or an unauthorized response is sent if stateless.</p>
 * <p>The pac4j client to use is selected with the clientName attributes.</p> 
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class RequiresAuthenticationHandler extends SessionAwareHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequiresAuthenticationHandler.class);

    private final Handler<HttpServerRequest> delegate;

    private final String clientName;

    private final Boolean isAjax;

    private String requireAnyRole;

    private String requireAllRoles;

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
        this.isAjax = isAjax;
        this.delegate = delegate;
    }

    public RequiresAuthenticationHandler(String clientName, Handler<HttpServerRequest> delegate,
            Pac4jHelper pac4jHelper, boolean stateless) {
        super(stateless);
        this.pac4jHelper = pac4jHelper;
        this.clientName = clientName;
        isAjax = false;
        this.delegate = delegate;
    }

    @Override
    protected void doHandle(final HttpServerRequest req, final String sessionId, final JsonObject sessionAttributes) {

        retrieveUserProfile(req, sessionId, sessionAttributes, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(Message<JsonObject> event) {
                final JsonObject response = event.body();
                JsonObject sessionAttributes = pac4jHelper.getSessionAttributes(response);
                // HttpAction is required ?
                if (pac4jHelper.isRequiresHttpAction(response)) {
                    if (isStateless()) {
                        pac4jHelper.sendResponse(req.response(), response);
                    } else {
                        saveSessionAttributes(sessionId, sessionAttributes, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject event) {
                                pac4jHelper.sendResponse(req.response(), response);
                            }
                        });
                    }
                    return;
                }

                // authentication success or failure strategy
                Object profile = pac4jHelper.getUserProfileFromResponse(response);
                if (profile == null) {
                    authenticationFailure(req, sessionId, sessionAttributes);
                } else {
                    AuthHttpServerRequest authReq = saveUserProfile(profile, req, sessionAttributes);
                    authenticationSuccess(authReq, sessionId, sessionAttributes);
                }

            }
        });

    }

    /**
     * Returns the User Profile from the session if stateful or from the credentials if stateless.
     * 
     * @param req
     * @param sessionId
     * @param sessionAttributes
     * @param handler
     */
    protected void retrieveUserProfile(HttpServerRequest req, String sessionId, JsonObject sessionAttributes,
            Handler<Message<JsonObject>> handler) {
        if (isStateless()) {
            authenticate(req, sessionId, sessionAttributes, handler);
        } else {
            final Object profile = sessionAttributes.getValue(HttpConstants.USER_PROFILE);
            logger.debug("profile : {}", profile);
            handler.handle(new JsonObjectMessage(false, null, pac4jHelper.buildUserProfileResponse(profile,
                    sessionAttributes)));
        }
    }

    /**
     * Authenticates the given request.
     * 
     * @param req
     * @param sessionId
     * @param sessionAttributes
     * @param handler
     */
    protected void authenticate(HttpServerRequest req, String sessionId, JsonObject sessionAttributes,
            Handler<Message<JsonObject>> handler) {

        pac4jHelper.authenticate(req, sessionAttributes, clientName, handler);

    }

    /**
     * Default authentication success strategy; forwards the request to the next handler if the user has a granted access,
     * sends a 403 forbidden response otherwise.
     * 
     * @param req
     * @param sessionId
     * @param sessionAttributes
     */
    protected void authenticationSuccess(AuthHttpServerRequest req, String sessionId, JsonObject sessionAttributes) {
        if (hasAccess(req.getProfile(), req)) {
            delegate.handle(req);
        } else {
            HttpResponseHelper.forbidden(req, "forbidden");
        }
    }

    /**
     * Default authentication failure strategy; save the original url in session and redirects to the Identity Provider if stateful.
     * Sends an unauthorized response if stateless.
     * 
     * @param req
     * @param sessionId
     * @param sessionAttributes
     */
    protected void authenticationFailure(HttpServerRequest req, String sessionId, JsonObject sessionAttributes) {
        if (isStateless()) {
            HttpResponseHelper.unauthorized(req, "unauthorized");
        } else {
            // no authentication tried -> redirect to provider
            // keep the current url
            saveOriginalUrl(req, sessionAttributes);
            // compute and perform the redirection
            redirectToIdentityProvider(req, sessionId, sessionAttributes);
        }
    }

    /**
     * Default access strategy based on the hasAccess method from the {@link UserProfile}.
     * 
     * @param profile
     * @param req
     * @return
     */
    protected boolean hasAccess(UserProfile profile, HttpServerRequest req) {
        return profile.hasAccess(requireAnyRole, requireAllRoles);
    }

    /**
     * Save User Profile in session if stateful. Wraps and returns the current {@link HttpServerRequest} in an {@link AuthHttpServerRequest}.
     * 
     * @param profile
     * @param req
     * @param sessionAttributes
     * @return
     */
    protected AuthHttpServerRequest saveUserProfile(Object profile, HttpServerRequest req, JsonObject sessionAttributes) {
        AuthHttpServerRequest authReq = new AuthHttpServerRequest(req);
        authReq.setProfile(pac4jHelper.decodeUserProfile(profile));
        if (!isStateless()) {
            pac4jHelper.saveUserProfileInSession(profile, sessionAttributes);
        }
        return authReq;
    }

    /**
     * Redirects to the configured Identity Provider.
     * 
     * @param req
     * @param sessionId
     * @param sessionAttributes
     */
    protected void redirectToIdentityProvider(final HttpServerRequest req, final String sessionId,
            JsonObject sessionAttributes) {
        pac4jHelper.redirect(req, sessionAttributes, clientName, true, isAjaxRequest(req),
                new Handler<Message<JsonObject>>() {

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

    /**
     * Is the current request an Ajax request.
     * 
     * @param req
     * @return
     */
    protected boolean isAjaxRequest(HttpServerRequest req) {
        return isAjax;
    }

    /**
     * Save the original url in session if the request is not Ajax.
     * 
     * @param req
     * @param sessionAttributes
     */
    protected void saveOriginalUrl(HttpServerRequest req, JsonObject sessionAttributes) {
        if (!isAjaxRequest(req)) {
            final String requestedUrlToSave = pac4jHelper.getFullRequestURL(req);
            saveUrl(requestedUrlToSave, sessionAttributes);
        }
    }

    protected void saveUrl(String requestedUrlToSave, JsonObject sessionAttributes) {
        sessionAttributes.putString(HttpConstants.REQUESTED_URL, requestedUrlToSave);
        logger.debug("requestedUrlToSave : {}", requestedUrlToSave);
    }

    protected String retrieveOriginalUrl(HttpServerRequest req, JsonObject sessionAttributes) {
        return sessionAttributes.getString(HttpConstants.REQUESTED_URL);
    }

    public String getRequireAnyRole() {
        return requireAnyRole;
    }

    public void setRequireAnyRole(String requireAnyRole) {
        this.requireAnyRole = requireAnyRole;
    }

    public String getRequireAllRoles() {
        return requireAllRoles;
    }

    public void setRequireAllRoles(String requireAllRoles) {
        this.requireAllRoles = requireAllRoles;
    }

}
