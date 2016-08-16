package org.pac4j.vertx.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuth1Token;
import com.github.scribejava.core.oauth.OAuth10aService;
import org.pac4j.core.context.WebContext;
import org.pac4j.oauth.client.BaseOAuth10Client;
import org.pac4j.oauth.profile.JsonHelper;
import org.pac4j.vertx.profile.TestOAuth1Profile;

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

    @Override
    protected void internalInit(final WebContext webContext) {
        super.internalInit(webContext);
        this.service = new OAuth10aService((DefaultApi10a) getApi(), buildOAuthConfig(webContext));
    }

    @Override
    protected BaseApi<OAuth10aService> getApi() {
        return new TestOAuthWrapperApi10();
    }

    @Override
    protected boolean hasBeenCancelled(WebContext context) {
        return false;
    }

    @Override
    protected String getProfileUrl(OAuth1Token accessToken) {
        return null;
    }

    @Override
    protected TestOAuth1Profile extractUserProfile(String body) {
        final TestOAuth1Profile profile = new TestOAuth1Profile();
        JsonNode json = JsonHelper.getFirstNode(body);
        if (json != null) {
            profile.setId(JsonHelper.getElement(json, "id"));
        }
        return profile;
    }

//    @Override
//    protected BaseClient<OAuthCredentials, TestOAuth1Profile> newClient() {
//        return new TestOAuth1Client();
//    }

    @Override
    protected String retrieveAuthorizationUrl(WebContext context) {
        // Intentional override of this since the test doesn't need to care about actually
        // getting a request token so let's not create an endpoint to do so
        OAuth1RequestToken token = new OAuth1RequestToken(TEST_REQUEST_TOKEN, TEST_SECRET);
        context.setSessionAttribute(getRequestTokenSessionAttributeName(), token);
        final String authorizationUrl = getAuthorizationUrl(token);
        logger.debug("authorizationUrl : {}", authorizationUrl);
        return authorizationUrl;
    }

    private String getAuthorizationUrl(OAuth1RequestToken token) {
        return TEST_AUTHORIZATION_URL + "?authToken=" + token.getToken();
    }
}
