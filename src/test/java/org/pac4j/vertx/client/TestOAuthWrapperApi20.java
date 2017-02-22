package org.pac4j.vertx.client;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.Verb;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class TestOAuthWrapperApi20 extends DefaultApi20 {

    private final String baseAuthorizationUrl;

    public TestOAuthWrapperApi20(final String baseAuthorizationUrl) {
        this.baseAuthorizationUrl = baseAuthorizationUrl;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return "http://localhost:9292/authToken?grant_type=authorization_code";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return this.baseAuthorizationUrl;
    }

    @Override
    public Verb getAccessTokenVerb() {
        return Verb.GET;
    }
}
