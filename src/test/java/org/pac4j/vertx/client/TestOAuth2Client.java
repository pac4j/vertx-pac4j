package org.pac4j.vertx.client;

import org.pac4j.core.context.WebContext;
import org.pac4j.oauth.client.OAuth20Client;
import org.pac4j.oauth.config.OAuth20Configuration;
import org.pac4j.vertx.profile.TestOAuth2Profile;
import org.pac4j.vertx.profile.TestOAuth2ProfileDefinition;

/**
 * Dumbed down OAuth2 client just to be used for testing purposes.
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class TestOAuth2Client extends OAuth20Client<TestOAuth2Profile> {

    private String baseAuthorizationUrl;

    public TestOAuth2Client() {
       setConfiguration(new OAuth20Configuration());
    }

    @Override
    protected void clientInit(WebContext context) {
        configuration.setApi(new TestOAuthWrapperApi20(getBaseAuthorizationUrl()));
        configuration.setProfileDefinition(new TestOAuth2ProfileDefinition());
        configuration.setWithState(true);
        super.clientInit(context);
    }

    public String getBaseAuthorizationUrl() {
        return baseAuthorizationUrl;
    }

    public void setBaseAuthorizationUrl(String baseAuthorizationUrl) {
        this.baseAuthorizationUrl = baseAuthorizationUrl;
    }
}
