package org.pac4j.vertx;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.junit.Test;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.vertx.handler.impl.LogoutHandler;
import org.pac4j.vertx.handler.impl.LogoutHandlerOptions;
import org.pac4j.vertx.handler.impl.CallbackHandlerOptions;
import org.pac4j.vertx.handler.impl.SecurityHandlerOptions;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.pac4j.vertx.TestConstants.FORBIDDEN_BODY;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
@SuppressWarnings("RedundantThrows")
public class StatefulPac4jAuthHandlerSingleProfileIntegrationTest extends StatefulPac4jAuthHandlerIntegrationTestBase {

    private static final String LOGOUT_URL_FOR_CLIENT = "/logout?url=/";

    private static final Logger LOG = LoggerFactory.getLogger(StatefulPac4jAuthHandlerSingleProfileIntegrationTest.class);

    @Test
    public void testSuccessfulOAuth2LoginWithoutAuthorities() throws Exception {

        LOG.info("testSuccessfulOAuth2LoginWithoutAuthorities");
        LOG.debug("Starting auth provider mimic");
        startOAuthProviderMimic("testUser1");
        // Start a web server with no required authorities (i.e. only authentication required) for the secured resource
        startWebServer(TEST_OAUTH2_SUCCESS_URL, optionsWithBothNamesProvided(), callbackHandlerOptions(), null);
        loginSuccessfullyExpectingAuthorizedUser(Void -> testComplete());
        await(2, TimeUnit.SECONDS);
    }

    @Test
    public void testSuccessfulOAuth2LoginWithoutAuthorizerName() throws Exception {
        // This should let any user access a protected resource - we'll set up with a user who doesn't have
        // adequate permissions if the authorizer is used
        startOAuthProviderMimic("testUser2");
        final String[] permissions = {
                "permission1", "permission2"
        };
        startWebServer(TEST_OAUTH2_SUCCESS_URL, new SecurityHandlerOptions().setClients(TEST_CLIENT_NAME), callbackHandlerOptions(), Arrays.asList(permissions));
        loginSuccessfullyExpectingAuthorizedUser(Void -> testComplete());
        await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testSuccessfulOAuth2LoginWithInsufficientAuthorities() throws Exception {
        startOAuthProviderMimic("testUser2");
        final String[] permissions = {
                "permission1", "permission2"
        };
        startWebServer(TEST_OAUTH2_SUCCESS_URL, optionsWithBothNamesProvided(), callbackHandlerOptions(), Arrays.asList(permissions));
        loginSuccessfullyExpectingUnauthorizedUser(Void -> testComplete());
        await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testSuccessfulOAuth2LoginWithSufficientAuthorities() throws Exception {
        startOAuthProviderMimic("testUser2");
        final String[] permissions = {"permission1"};
        startWebServer(TEST_OAUTH2_SUCCESS_URL, optionsWithBothNamesProvided(), callbackHandlerOptions(),  Arrays.asList(permissions));
        loginSuccessfullyExpectingAuthorizedUser(Void -> testComplete());
        await(1, TimeUnit.SECONDS);
    }

    // Test that subsequent access following successful login doesn't require another set of redirects, assuming session
    // is maintained
    @Test
    public void testSubsequentAccessFollowingSuccessfulLogin() throws Exception {
        startOAuthProviderMimic("testUser1");
        // Start a web server with no required authorities (i.e. only authentication required) for the secured resource
        startWebServer(TEST_OAUTH2_SUCCESS_URL, optionsWithBothNamesProvided(), callbackHandlerOptions(), null);
        HttpClient client = vertx.createHttpClient();
        loginSuccessfullyExpectingAuthorizedUser(client, Void -> {

            final HttpClientRequest successfulRequest = client.get(8080, "localhost", "/private/success.html");
            getSessionCookie().ifPresent(cookie -> successfulRequest.putHeader("cookie", cookie));
            successfulRequest.handler(resp -> {

                assertEquals(200, resp.statusCode());
                validateInitialLoginSuccessResponse(resp, v -> testComplete());
            }).end();
        });
        await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testLogoutRequiresSubsequentReauthentication() throws Exception {
        startOAuthProviderMimic("testUser1");
        // Start a web server with no required authorities (i.e. only authentication required) for the secured resource
        startWebServer(TEST_OAUTH2_SUCCESS_URL, optionsWithBothNamesProvided(), callbackHandlerOptions(), null,
                (router, config) -> router.route(HttpMethod.GET, "/logout")
                        .handler(new LogoutHandler(vertx, sessionStore, new LogoutHandlerOptions(), config)));

        HttpClient client = vertx.createHttpClient();
        loginSuccessfullyExpectingAuthorizedUser(client, Void -> {
            LOG.info("Successfully logged in, now about to logout");
            logout(client, response -> {
                // We don't need to actually bother to redirect to the url, we've validated that we're directed to following
                // logout, what we do need to do, is with our session established, try and connect to a protected url again
                // and ensure we get a redirect to the auth provider as expected
                final HttpClientRequest successfulRequest = client.get(8080, "localhost", "/private/success.html");
                getSessionCookie().ifPresent(cookie -> successfulRequest.putHeader("cookie", cookie));
                successfulRequest.handler(resp -> {
                    assertEquals(302, resp.statusCode());
                    final String redirectToUrl = resp.getHeader("location");
                    LOG.info("RedirectTo: " + redirectToUrl);
                    // Check we're redirecting to a url derived from the auth provider url we passed to the web server
                    assertTrue(redirectToUrl.startsWith(TEST_OAUTH2_SUCCESS_URL));
                    testComplete();
                })
                .end();


            });
        });
        await(1, TimeUnit.SECONDS);
    }

    @Override
    protected CallbackHandlerOptions callbackHandlerOptions() {
        return new CallbackHandlerOptions().setDefaultUrl(Pac4jConstants.DEFAULT_URL).setMultiProfile(false);
    }

    @Override
    protected Clients clients(final String baseAuthUrl) {
        return new Clients(oAuth2Client(baseAuthUrl), testOAuth1Client());
    }

    private void loginSuccessfullyExpectingUnauthorizedUser(final Consumer<Void> subsequentActions) throws Exception {
        loginSuccessfully(finalRedirectResponse -> {
            assertEquals(403, finalRedirectResponse.statusCode());
            finalRedirectResponse.bodyHandler(body -> {
                assertEquals(FORBIDDEN_BODY, body.toString());
                subsequentActions.accept(null);
            });
        });
    }

    private void loginSuccessfully(final Handler<HttpClientResponse> finalResponseHandler) throws Exception {
        HttpClient client = vertx.createHttpClient();
        loginSuccessfully(client, finalResponseHandler);
    }

    private void logout(final HttpClient client, final Handler<HttpClientResponse> postLogoutActions) {
        final HttpClientRequest logoutRequest = client.get(8080, "localhost", LOGOUT_URL_FOR_CLIENT);
        getSessionCookie().ifPresent(cookie -> logoutRequest.putHeader("cookie", cookie));
        logoutRequest.handler(response -> {
            assertEquals(302, response.statusCode());
            final String redirectToUrl = response.getHeader("location");
            assertEquals(redirectToUrl, "/");
            postLogoutActions.handle(response);
        }).end();
    }

}
