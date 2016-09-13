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
