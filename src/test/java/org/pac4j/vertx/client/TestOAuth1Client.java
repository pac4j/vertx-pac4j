package org.pac4j.vertx.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuth1Token;
import com.github.scribejava.core.oauth.OAuth10aService;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.redirect.RedirectAction;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.oauth.client.OAuth10Client;
import org.pac4j.oauth.profile.JsonHelper;
import org.pac4j.oauth.profile.twitter.TwitterProfileDefinition;
import org.pac4j.vertx.profile.TestOAuth1Profile;

/**
 * Dumbed down OAuth2 client just to be used for testing purposes.
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class TestOAuth1Client extends OAuth10Client<TestOAuth1Profile> {

    public static final String TEST_REQUEST_TOKEN = "testRequestToken";
    public static final String TEST_SECRET = "testSecret";
    public static final String TEST_AUTHORIZATION_URL = "http://localhost:9292/authenticate";

    @Override
    protected void clientInit(final WebContext context) {
        configuration.setApi(getApi());
        configuration.setProfileDefinition(new TwitterProfileDefinition());
        configuration.setHasBeenCancelledFactory(ctx -> {
            final String denied = ctx.getRequestParameter("denied");
            if (CommonHelper.isNotBlank(denied)) {
                return true;
            } else {
                return false;
            }
        });
        setConfiguration(configuration);
        setLogoutActionBuilder((ctx, profile, targetUrl) -> RedirectAction.redirect("http://localhost:9292/logout"));

        super.clientInit(context);
    }

    protected BaseApi<OAuth10aService> getApi() {
        return new TestOAuthWrapperApi10();
    }


//    @Override
//    protected String retrieveAuthorizationUrl(WebContext context) {
//        // Intentional override of this since the test doesn't need to care about actually
//        // getting a request token so let's not create an endpoint to do so
//        OAuth1RequestToken token = new OAuth1RequestToken(TEST_REQUEST_TOKEN, TEST_SECRET);
//        context.setSessionAttribute(getRequestTokenSessionAttributeName(), token);
//        final String authorizationUrl = getAuthorizationUrl(token);
//        logger.debug("authorizationUrl : {}", authorizationUrl);
//        return authorizationUrl;
//    }
//

    private String getAuthorizationUrl(OAuth1RequestToken token) {
        return TEST_AUTHORIZATION_URL + "?authToken=" + token.getToken();
    }
}
