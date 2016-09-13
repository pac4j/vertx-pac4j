package org.pac4j.vertx.client;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.utils.OAuthEncoder;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class TestOAuthWrapperApi20 extends DefaultApi20 {

    private final String authenticationUrlTemplate;

    public TestOAuthWrapperApi20(final String authenticationUrlTemplate) {
        this.authenticationUrlTemplate = authenticationUrlTemplate;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return "http://localhost:9292/authToken?grant_type=authorization_code";
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config) {


        return String.format(this.authenticationUrlTemplate, config.getApiKey(),
                OAuthEncoder.encode(config.getCallback()), OAuthEncoder.encode(config.getState()));
    }

    @Override
    public Verb getAccessTokenVerb() {
        return Verb.GET;
    }
}
