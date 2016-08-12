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

import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.model.OAuth1RequestToken;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class TestOAuthWrapperApi10 extends DefaultApi10a {

    public static final String ACCESS_TOKEN_ENDPOINT = "http://localhost:9292/accessToken";
    public static final String REQUEST_TOKEN_ENDPOINT = "http://localhost:9292/requestToken";
    public static final String TEST_AUTHORIZATION_URL = "http://localhost:9292/authenticate";

    @Override
    public String getAccessTokenEndpoint() {
        return ACCESS_TOKEN_ENDPOINT;
    }

    @Override
    public String getAuthorizationUrl(OAuth1RequestToken requestToken) {
        return TEST_AUTHORIZATION_URL;
    }

    @Override
    public String getRequestTokenEndpoint() {
        return REQUEST_TOKEN_ENDPOINT;
    }
}
