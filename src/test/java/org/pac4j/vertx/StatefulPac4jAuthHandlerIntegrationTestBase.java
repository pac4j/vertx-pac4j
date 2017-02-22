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
package org.pac4j.vertx;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.client.TestOAuth1Client;
import org.pac4j.vertx.client.TestOAuth2AuthorizationGenerator;
import org.pac4j.vertx.client.TestOAuth2Client;
import org.pac4j.vertx.handler.impl.CallbackDeployingPac4jAuthHandler;
import org.pac4j.vertx.handler.impl.CallbackHandlerOptions;
import org.pac4j.vertx.handler.impl.SecurityHandlerOptions;
import org.pac4j.vertx.http.DefaultHttpActionAdapter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Abstract superclass for stateful auth handler integration tests to share convenience functions between single
 * and multi profile versions of tests
 * @since 2.0.0
 */
public abstract class StatefulPac4jAuthHandlerIntegrationTestBase extends Pac4jAuthHandlerIntegrationTestBase {

    protected static final String TEST_OAUTH2_SUCCESS_URL = "http://localhost:9292/authSuccess";
    private static final Logger LOG = LoggerFactory.getLogger(StatefulPac4jAuthHandlerIntegrationTestBase.class);
    private static final String TEST_CLIENT_ID = "testClient";
    private static final String TEST_CLIENT_SECRET = "testClientSecret";
    private static final String TEST_OAUTH_2_CLIENT_NAME = "TestOAuth2Client";
    private static final String FIELD_ACCESS_TOKEN = "access_token";


    // This will be our session cookie header for use by requests
    private final AtomicReference<String> sessionCookie = new AtomicReference<>();

    private void validateProtectedResourceContentFollowingInitialLogin(final JsonObject jsonObject) {
        assertThat(jsonObject
                .getJsonObject(TEST_OAUTH_2_CLIENT_NAME)
                .getString(FIELD_ACCESS_TOKEN), is(notNullValue()));
    }

    protected void startOAuthProviderMimic(final String userName) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final OAuth2ProviderMimic mimic = getOAuthProviderMimic(userName);

        vertx.deployVerticle(mimic, result -> latch.countDown());
        latch.await(2, TimeUnit.SECONDS);
        LOG.info("oAuthProviderMimic started");
    }

    /**
     * Start a web server to define a standard set of endpoints for testing, and provide extension points for both
     * decorating/configuring the auth handler, and also for configuring the router (for example adding additional
     * endpoints)
     * @param baseAuthUrl - the baseline authentication url
     * @param options - SecurityHandler options for configuring the main security handler
     * @param callbackHandlerOptions - CallbackHandlerOptions for configuring the callback handler
     * @param requiredPermissions - list of required permissions to access the protected endpoint
     * @param routerDecorator - modifications to the router (for example addition of custom endpoints for a test
     * @throws Exception - any exception when running this code should trigger test failure
     */
    protected void startWebServer(final String baseAuthUrl,
                                  final SecurityHandlerOptions options,
                                  final CallbackHandlerOptions callbackHandlerOptions,
                                  final List<String> requiredPermissions,
                                  final BiConsumer<Router, Config> routerDecorator) throws Exception {
        final Router router = Router.router(vertx);
        final Pac4jAuthProvider authProvider = new Pac4jAuthProvider();
        final SessionStore sessionStore = sessionStore();
        final Config config = config(clients(baseAuthUrl), requiredPermissions);
        config.setHttpActionAdapter(new DefaultHttpActionAdapter());

        router.route().handler(CookieHandler.create());
        router.route().handler(sessionHandler(sessionStore));
        router.route().handler(UserSessionHandler.create(authProvider));

        CallbackDeployingPac4jAuthHandler pac4jAuthHandler = authHandler(router,
                authProvider,
                baseAuthUrl,
                options,
                callbackHandlerOptions,
                requiredPermissions);

        routerDecorator.accept(router, config);

        startWebServer(router, pac4jAuthHandler);
    }

    private CallbackDeployingPac4jAuthHandler authHandler(final Router router,
                                                          final Pac4jAuthProvider authProvider,
                                                          final String baseAuthUrl,
                                                          final SecurityHandlerOptions options,
                                                          final CallbackHandlerOptions callbackHandlerOptions,
                                                          final List<String> requiredPermissions) {
        return new CallbackDeployingPac4jAuthHandler(vertx, sessionStore, config(clients(baseAuthUrl), requiredPermissions), router, authProvider, options, callbackHandlerOptions);
    }

    private SessionHandler sessionHandler(SessionStore sessionStore) {
        return SessionHandler.create(sessionStore).setSessionCookieName("oAuth2Consumer.session");
    }

    private LocalSessionStore sessionStore() {
        return LocalSessionStore.create(vertx);
    }

    private Config config(final Clients clients, List<String> requiredPermissions) {
        clients.setCallbackUrl("http://localhost:8080/authResult");
        return new  Config(clients, authorizers(requiredPermissions));
    }

    protected Optional<String> getSessionCookie() {
        return Optional.ofNullable(sessionCookie.get());
    }

    protected TestOAuth2Client oAuth2Client(final String baseAuthUrl) {
        TestOAuth2Client client = new TestOAuth2Client();
        client.setCallbackUrl("http://localhost:8080/authResult");
        client.setKey(TEST_CLIENT_ID);
        client.setSecret(TEST_CLIENT_SECRET);
        client.setName("TestOAuth2Client");
        client.setIncludeClientNameInCallbackUrl(true);
        client.setBaseAuthorizationUrl(baseAuthUrl);
        client.addAuthorizationGenerator(new TestOAuth2AuthorizationGenerator());
        return client;
    }

    TestOAuth1Client testOAuth1Client() {
        TestOAuth1Client client =  new TestOAuth1Client();
        client.setKey(TEST_CLIENT_ID);
        client.setSecret(TEST_CLIENT_SECRET);
        client.setName("TestOAuth1Client");
        return client;
    }

    protected SecurityHandlerOptions optionsWithBothNamesProvided() {
        return new SecurityHandlerOptions().setAuthorizers(REQUIRE_ALL_AUTHORIZER)
                .setClients(TEST_CLIENT_NAME);
    }

    void startWebServer(final String baseAuthUrl,
                        final SecurityHandlerOptions options,
                        final CallbackHandlerOptions callbackHandlerOptions,
                        final List<String> requiredPermissions) throws Exception {
        startWebServer(baseAuthUrl, options, callbackHandlerOptions, requiredPermissions, (router, consumer) -> {});
    }

    void loginSuccessfullyExpectingAuthorizedUser(final Consumer<Void> subsequentActions) throws Exception {
        loginSuccessfullyExpectingAuthorizedUser(vertx.createHttpClient(), subsequentActions);
    }

    protected void loginSuccessfullyExpectingAuthorizedUser(final HttpClient client, final Consumer<Void> subsequentActions) throws Exception {
        loginSuccessfully(client, finalRedirectResponse -> {
            assertEquals(200, finalRedirectResponse.statusCode());
            validateInitialLoginSuccessResponse(finalRedirectResponse, subsequentActions);
        });
    }

    void loginSuccessfully(final HttpClient client, final Handler<HttpClientResponse> finalResponseHandler) throws Exception {
        // Attempt to get a private url
        final HttpClientRequest successfulRequest = client.get(8080, "localhost", "/private/success.html");
        successfulRequest.handler(
                // redirect to auth handler
                expectAndHandleRedirect(client,
                        extractCookie(),
                        // redirect to auth response handler
                        expectAndHandleRedirect(client, clientResponse -> {},
                                // redirect to original url if authorized
                                expectAndHandleRedirect(client, httpClientResponse -> {},
                                        finalResponseHandler)))
        )
                .end();

    }

    private Consumer<HttpClientResponse> extractCookie() {
        return clientResponse -> {
            final String setCookie = clientResponse.headers().get("set-cookie");
            assertNotNull(setCookie);
            sessionCookie.set(setCookie); // We're going to want to use this subsequently
        };
    }

    protected Handler<HttpClientResponse> expectAndHandleRedirect(final HttpClient client,
                                                                final Consumer<HttpClientResponse> responseConsumer,
                                                                final Handler<HttpClientResponse> redirectResultHandler) {
        return response -> {
            assertEquals(302, response.statusCode());
            responseConsumer.accept(response);
            final String redirectToUrl = response.getHeader("location");
            LOG.info("Redirecting to " + redirectToUrl);
            redirectToUrl(redirectToUrl, client, redirectResultHandler);
        };
    }

    private void redirectToUrl(final String redirectUrl, final HttpClient client, final Handler<HttpClientResponse> resultHandler) {
        final HttpClientRequest request = client.getAbs(redirectUrl);
        getSessionCookie().ifPresent(cookie -> request.putHeader("cookie", cookie));
        request.handler(resultHandler);
        request.end();
    }

    private Consumer<String> protectedResourceContentValidator() {
        return body -> {
            final JsonObject json = new JsonObject(body);
            validateProtectedResourceContentFollowingInitialLogin(json);
        };
    }

    void validateInitialLoginSuccessResponse(final HttpClientResponse response, final Consumer<Void> subsequentActions) {
        response.bodyHandler(body -> {
            protectedResourceContentValidator().accept(body.toString());
            subsequentActions.accept(null);
        });
    }


    private OAuth2ProviderMimic getOAuthProviderMimic(final String userId) {
        return new OAuth2ProviderMimic(userId);
    }

    protected abstract CallbackHandlerOptions callbackHandlerOptions();
    protected abstract Clients clients(final String baseAuthUrl);

}
