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
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.pac4j.core.context.WebContext;
import org.pac4j.oauth.client.BaseOAuth20StateClient;
import org.pac4j.oauth.profile.JsonHelper;
import org.pac4j.vertx.profile.TestOAuth2Profile;

/**
 * Dumbed down OAuth2 client just to be used for testing purposes.
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class TestOAuth2Client extends BaseOAuth20StateClient<TestOAuth2Profile> {

    private String authorizationUrlTemplate;

    @Override
    protected BaseApi<OAuth20Service> getApi() {
        return new TestOAuthWrapperApi20(this.getAuthorizationUrlTemplate());
    }

    @Override
    protected boolean hasBeenCancelled(WebContext context) {
        return false;
    }

    @Override
    protected String getProfileUrl(OAuth2AccessToken accessToken) {
        return "http://localhost:9292/profile";
    }

    @Override
    protected TestOAuth2Profile extractUserProfile(String body) {
        final TestOAuth2Profile profile = new TestOAuth2Profile();
        JsonNode json = JsonHelper.getFirstNode(body);
        if (json != null) {
            profile.setId(JsonHelper.getElement(json, "id"));
        }
        return profile;
    }

//    @Override
//    protected BaseClient<OAuthCredentials, TestOAuth2Profile> newClient() {
//        return new TestOAuth2Client();
//    }

    public String getAuthorizationUrlTemplate() {
        return authorizationUrlTemplate;
    }

    public void setAuthorizationUrlTemplate(String authorizationUrlTemplate) {
        this.authorizationUrlTemplate = authorizationUrlTemplate;
    }
}
