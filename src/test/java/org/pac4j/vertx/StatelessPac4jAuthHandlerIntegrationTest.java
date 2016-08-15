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

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.matching.ExcludedPathMatcher;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.handler.impl.Pac4jAuthHandlerOptions;
import org.pac4j.vertx.handler.impl.SecurityHandler;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class StatelessPac4jAuthHandlerIntegrationTest extends Pac4jAuthHandlerIntegrationTestBase {

    private static final String AUTH_HEADER_NAME = "Authorization";
    private static final String BASIC_AUTH_PREFIX = "Basic ";
    public static final String TEST_USER_NAME = "testUser";
    private static final String TEST_BASIC_AUTH_HEADER = BASIC_AUTH_PREFIX + Base64.encodeBase64String((TEST_USER_NAME + ":testUser").getBytes());
    private static final String TEST_FAILING_BASIC_AUTH_HEADER = BASIC_AUTH_PREFIX + Base64.encodeBase64String((TEST_USER_NAME + ":testUser2").getBytes());
    public static final String PROTECTED_RESOURCE_URL = "/private/success.html";
    public static final String BASIC_AUTH_CLIENT = "BasicAuthClient";
    private static final String USERNAME_FIELD = "username";
    public static final String EXCLUDED_PATH_MATCHER_NAME = "ExcludedPathMatcher";

    @Test
    public void testSuccessfulLogin() throws Exception {

        testProtectedResourceAccessWithCredentials(TEST_BASIC_AUTH_HEADER, 200, protectedResourceContentValidator());

    }

    @Test
    public void testFailedLogin() throws Exception {

        testProtectedResourceAccessWithCredentials(TEST_FAILING_BASIC_AUTH_HEADER, 401, unauthorizedContentValidator());

    }

    @Test
    public void testExcludedUrlIsAccessibleWithoutCredentials() throws Exception {
        testResourceAccessWithoutCredentials(EXCLUDED_PROTECTED_RESOURCE_URL, 200, unprotectedResourceContentValidator());
    }

    @Test
    public void testExcludedUrlIsAccessibleWithInvalidCredentials() throws Exception {
        testResourceAccess(EXCLUDED_PROTECTED_RESOURCE_URL, Optional.of(TEST_FAILING_BASIC_AUTH_HEADER), 200, unprotectedResourceContentValidator());
    }

    @Override
    protected void validateProtectedResourceContent(JsonObject jsonObject) {
        assertThat(jsonObject.getString(USERNAME_FIELD), is(TEST_USER_NAME));
    }

    protected Consumer<String> unprotectedResourceContentValidator() {
        return s -> s.equals(UNPROTECTED_RESOURCE_BODY);
    }

    private void testResourceAccessWithoutCredentials(final String url,
                                                      final int expectedHttpStatus,
                                                      final Consumer<String> bodyValidator) throws Exception {
        testResourceAccess(url, Optional.empty(), expectedHttpStatus, bodyValidator);
    }

    private void testProtectedResourceAccessWithCredentials(final String credentialsHeader, final int expectedHttpStatus, final Consumer<String> bodyValidator) throws Exception {
        testResourceAccess(PROTECTED_RESOURCE_URL, Optional.of(credentialsHeader), expectedHttpStatus, bodyValidator);
    }

    private void testResourceAccess(final String url,
                                    final Optional<String> credentialsHeader,
                                    final int expectedHttpStatus,
                                    final Consumer<String> bodyValidator) throws Exception {
        startWebServer();
        HttpClient client = vertx.createHttpClient();
        // Attempt to get a private url
        final HttpClientRequest request = client.get(8080, "localhost", url);
        credentialsHeader.ifPresent(header -> request.putHeader(AUTH_HEADER_NAME, header));
        // This should get the desired result straight away rather than operating through redirects
        request.handler(response -> {
            assertEquals(expectedHttpStatus, response.statusCode());
            response.bodyHandler(body -> {
                final String bodyContent = body.toString();
                bodyValidator.accept(bodyContent);
                testComplete();
            });
        });
        request.end();
        await(2, TimeUnit.SECONDS);

    }

    private Consumer<String> unauthorizedContentValidator() {
        return body -> assertEquals(UNAUTHORIZED_BODY, body);
    }

    private void startWebServer() throws Exception {

        final Router router = Router.router(vertx);
        // Configure a pac4j stateless handler configured for basic http auth
        final Pac4jAuthProvider authProvider = new Pac4jAuthProvider();
        Pac4jAuthHandlerOptions options = new Pac4jAuthHandlerOptions()
                .withAuthorizerName(REQUIRE_ALL_AUTHORIZER)
                .withClientName(BASIC_AUTH_CLIENT)
                .withMatcherName(EXCLUDED_PATH_MATCHER_NAME);
        final SecurityHandler handler =  new SecurityHandler(vertx, config(), authProvider, options);
        startWebServer(router, handler);

    }

    private Config config() {
        final Clients clients = new Clients(client());
        final Config config = new Config(clients, authorizers(new ArrayList<>()));
        config.setMatcher(new ExcludedPathMatcher("^/private/public/.*$"));
        return config;
    }

    private Client client() {
        DirectBasicAuthClient client = new DirectBasicAuthClient();
        client.setName("BasicAuthClient");
        client.setAuthenticator(new SimpleTestUsernamePasswordAuthenticator());
        return client;
    }


}
