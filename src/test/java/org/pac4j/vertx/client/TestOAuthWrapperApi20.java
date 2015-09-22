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

import org.scribe.builder.api.StateApi20;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.utils.OAuthEncoder;


/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class TestOAuthWrapperApi20 extends StateApi20 {

    private static final String AUTHORIZE_URL_WITH_STATE = "http://localhost:9292/authSuccess?client_id=%s&redirect_uri=%s&state=%s";

    private final String authenticationUrlTemplate;

    public TestOAuthWrapperApi20(final String authenticationUrlTemplate) {
        this.authenticationUrlTemplate = authenticationUrlTemplate;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return "http://localhost:9292/authToken?grant_type=authorization_code";
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config, String state) {


        return String.format(this.authenticationUrlTemplate, config.getApiKey(),
                OAuthEncoder.encode(config.getCallback()), OAuthEncoder.encode(state));
    }

    @Override
    public AccessTokenExtractor getAccessTokenExtractor() {
        return new JsonTokenExtractor();
    }
}
