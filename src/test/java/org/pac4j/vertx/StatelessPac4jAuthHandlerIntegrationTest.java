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

import io.vertx.core.VoidHandler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.ext.web.Router;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.handler.impl.Pac4jAuthHandlerOptions;
import org.pac4j.vertx.handler.impl.RequiresAuthenticationHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class StatelessPac4jAuthHandlerIntegrationTest extends Pac4jAuthHandlerIntegrationTestBase {

    private static final String AUTH_HEADER_NAME = "Authorization";
    private static final String BASIC_AUTH_PREFIX = "Basic ";
    private static final String TEST_BASIC_AUTH_HEADER = BASIC_AUTH_PREFIX + Base64.encodeBase64String("testUser:testUser".getBytes());

    @Test
    public void testSuccessfulLogin() throws Exception {

        VoidHandler test = new VoidHandler() {
            @Override
            protected void handle() {
                HttpClient client = vertx.createHttpClient();
                // Attempt to get a private url
                final HttpClientRequest successfulRequest = client.get(8080, "localhost", "/private/success.html")
                        .putHeader(AUTH_HEADER_NAME, TEST_BASIC_AUTH_HEADER);
                // This should get the desired result straight away rather than operating through redirects
                successfulRequest.handler(response -> {
                    assertEquals(200, response.statusCode());
                    response.bodyHandler(body -> {
                        assertEquals("authenticationSuccess", body.toString());
                        testComplete();
                    });
                });
                successfulRequest.end();
                await(1, TimeUnit.SECONDS);
            }
        };

        startWebServer();
        test.handle(null);


    }

    private void startWebServer() throws Exception {

        final Router router = Router.router(vertx);
        // Configure a pac4j stateless handler configured for basic http auth
        final Pac4jAuthProvider authProvider = new Pac4jAuthProvider();
        Pac4jAuthHandlerOptions options = new Pac4jAuthHandlerOptions("BasicAuthClient");
        final RequiresAuthenticationHandler handler =  new RequiresAuthenticationHandler(vertx, config(), authProvider, options);
        startWebServer(router, handler);

    }

    private Config config() {
        final Clients clients = new Clients(client());
        return new Config(clients);
    }

    private Client client() {
        DirectBasicAuthClient client = new DirectBasicAuthClient();
        client.setName("BasicAuthClient");
        client.setAuthenticator(new SimpleTestUsernamePasswordAuthenticator());
        return client;
    }


}
