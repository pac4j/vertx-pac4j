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
import org.pac4j.oauth.client.BaseOAuth20StateClient;
import org.pac4j.oauth.credentials.OAuthCredentials;
import org.pac4j.oauth.profile.JsonHelper;
import org.pac4j.vertx.profile.TestOAuth2Profile;
import org.scribe.builder.api.StateApi20;
import org.scribe.model.OAuthConfig;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.oauth.StateOAuth20ServiceImpl;

/**
 * Dumbed down OAuth2 client just to be used for testing purposes.
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class TestOAuth2Client extends BaseOAuth20StateClient<TestOAuth2Profile> {

    protected StateApi20 api20;
    private String authorizationUrlTemplate;

    @Override
    protected void internalInit(final WebContext webContext) {
        super.internalInit(webContext);
        this.api20 = new TestOAuthWrapperApi20(this.getAuthorizationUrlTemplate());
        this.service = new StateOAuth20ServiceImpl(this.api20, new OAuthConfig(this.key, this.secret,
                this.callbackUrl,
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
        return "http://localhost:9292/profile";
    }

    @Override
    protected TestOAuth2Profile extractUserProfile(String body) {
        final TestOAuth2Profile profile = new TestOAuth2Profile();
        JsonNode json = JsonHelper.getFirstNode(body);
        if (json != null) {
            profile.setId(JsonHelper.get(json, "id"));
        }
        return profile;
    }

    @Override
    protected BaseClient<OAuthCredentials, TestOAuth2Profile> newClient() {
        return new TestOAuth2Client();
    }

    public String getAuthorizationUrlTemplate() {
        return authorizationUrlTemplate;
    }

    public void setAuthorizationUrlTemplate(String authorizationUrlTemplate) {
        this.authorizationUrlTemplate = authorizationUrlTemplate;
    }
}
