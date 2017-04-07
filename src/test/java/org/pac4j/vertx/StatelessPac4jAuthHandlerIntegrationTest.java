package org.pac4j.vertx;

import io.vertx.core.buffer.Buffer;
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
import org.pac4j.vertx.handler.impl.SecurityHandler;
import org.pac4j.vertx.handler.impl.SecurityHandlerOptions;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.pac4j.vertx.TestConstants.UNAUTHORIZED_BODY;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class StatelessPac4jAuthHandlerIntegrationTest extends StatelessPac4jAuthHandlerIntegrationTestBase {

    private static final String BASIC_AUTH_PREFIX = "Basic ";
    private static final String TEST_USER_NAME = "testUser";
    private static final String TEST_BASIC_AUTH_HEADER = BASIC_AUTH_PREFIX + Base64.encodeBase64String((TEST_USER_NAME + ":testUser").getBytes());
    private static final String TEST_FAILING_BASIC_AUTH_HEADER = BASIC_AUTH_PREFIX + Base64.encodeBase64String((TEST_USER_NAME + ":testUser2").getBytes());
    private static final String BASIC_AUTH_CLIENT = "BasicAuthClient";
    private static final String USERNAME_FIELD = "username";
    private static final String EXCLUDED_PATH_MATCHER_NAME = "ExcludedPathMatcher";

    @Test
    public void testSuccessfulLogin() throws Exception {

        testProtectedResourceAccessWithAuthHeader(TEST_BASIC_AUTH_HEADER, 200,
                validateJsonBody(this::validateProtectedResourceContent));

    }

    @Test
    public void testFailedLogin() throws Exception {

        testProtectedResourceAccessWithAuthHeader(TEST_FAILING_BASIC_AUTH_HEADER, 401, unauthorizedContentValidator());

    }

    @Test
    public void testExcludedUrlIsAccessibleWithoutCredentials() throws Exception {
        testResourceAccessWithoutCredentials(EXCLUDED_PROTECTED_RESOURCE_URL, 200, unprotectedResourceContentValidator());
    }

    @Test
    public void testExcludedUrlIsAccessibleWithInvalidCredentials() throws Exception {
        testResourceAccess(EXCLUDED_PROTECTED_RESOURCE_URL, Optional.of(TEST_FAILING_BASIC_AUTH_HEADER), 200, unprotectedResourceContentValidator());
    }

    private void validateProtectedResourceContent(JsonObject jsonObject) {
        assertThat(jsonObject
                .getJsonObject(BASIC_AUTH_CLIENT)
                .getString(USERNAME_FIELD), is(TEST_USER_NAME));
    }

    private Consumer<Buffer> unprotectedResourceContentValidator() {
        return b -> assertThat(b.toString(), is(UNPROTECTED_RESOURCE_BODY));
    }

    private void testResourceAccessWithoutCredentials(final String url,
                                                      final int expectedHttpStatus,
                                                      final Consumer<Buffer> bodyValidator) throws Exception {
        testResourceAccess(url, Optional.empty(), expectedHttpStatus, bodyValidator);
    }

    private Consumer<Buffer> unauthorizedContentValidator() {
        return body -> assertEquals(UNAUTHORIZED_BODY, body.toString());
    }

    @Override
    protected void startWebServer() throws Exception {

        final Router router = Router.router(vertx);
        // Configure a pac4j stateless handler configured for basic http auth
        final Pac4jAuthProvider authProvider = new Pac4jAuthProvider();
        SecurityHandlerOptions options = new SecurityHandlerOptions()
                .setAuthorizers(REQUIRE_ALL_AUTHORIZER)
                .setClients(BASIC_AUTH_CLIENT)
                .setMatchers(EXCLUDED_PATH_MATCHER_NAME);
        final SecurityHandler handler =  new SecurityHandler(vertx, sessionStore, config(), authProvider, options);
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
