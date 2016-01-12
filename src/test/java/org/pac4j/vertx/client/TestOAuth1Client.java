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
package org.pac4j.vertx.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.oauth.client.BaseOAuth10Client;
import org.pac4j.oauth.credentials.OAuthCredentials;
import org.pac4j.oauth.profile.JsonHelper;
import org.pac4j.vertx.profile.TestOAuth1Profile;
import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthConfig;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.oauth.ProxyOAuth10aServiceImpl;

/**
 * Dumbed down OAuth2 client just to be used for testing purposes.
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class TestOAuth1Client extends BaseOAuth10Client<TestOAuth1Profile> {

    public static final String TEST_REQUEST_TOKEN = "testRequestToken";
    public static final String TEST_SECRET = "testSecret";
    public static final String TEST_AUTHORIZATION_URL = "http://localhost:9292/authenticate";
    public static final String TEST_PROFILE_URL = "http://localhost:9292/profile";

    @Override
    protected void internalInit(final WebContext webContext) {
        super.internalInit(webContext);
        final DefaultApi10a api = new TestOAuthWrapperApi10();
        this.service = new ProxyOAuth10aServiceImpl(api, new OAuthConfig(this.key, this.secret, this.callbackUrl,
                SignatureType.Header, null, null),
                this.connectTimeout, this.readTimeout, this.proxyHost,
                this.proxyPort);
    }

    @Override
    protected boolean hasBeenCancelled(WebContext context) {
        return false;
    }

    @Override
    protected String getProfileUrl(Token accessToken) {
        return TEST_PROFILE_URL;
    }

    @Override
    protected TestOAuth1Profile extractUserProfile(String body) {
        final TestOAuth1Profile profile = new TestOAuth1Profile();
        JsonNode json = JsonHelper.getFirstNode(body);
        if (json != null) {
            profile.setId(JsonHelper.get(json, "id"));
        }
        return profile;
    }

    @Override
    protected BaseClient<OAuthCredentials, TestOAuth1Profile> newClient() {
        return new TestOAuth1Client();
    }

    @Override
    protected String retrieveAuthorizationUrl(WebContext context) {
        // Intentional override of this since the test doesn't need to care about actually
        // getting a request token so let's not create an endpoint to do so
        Token token = new Token(TEST_REQUEST_TOKEN, TEST_SECRET);
        context.setSessionAttribute(getRequestTokenSessionAttributeName(), token);
        final String authorizationUrl = getAuthorizationUrl(token);
        logger.debug("authorizationUrl : {}", authorizationUrl);
        return authorizationUrl;
    }

    private String getAuthorizationUrl(Token token) {
        return TEST_AUTHORIZATION_URL + "?authToken=" + token.getToken();
    }
}
