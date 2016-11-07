package org.pac4j.vertx;/*
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

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public abstract class StatelessPac4jAuthHandlerIntegrationTestBase extends Pac4jAuthHandlerIntegrationTestBase {

    private static final String PROTECTED_RESOURCE_URL = "/private/success.html";
    private static final String AUTH_HEADER_NAME = "Authorization";

    void testProtectedResourceAccessWithAuthHeader(final String authHeader,
                                                             final int expectedHttpStatus,
                                                             final Consumer<Buffer> bodyValidator) throws Exception {
        final Map<String, String> headers = new HashMap<>();
        headers.put(AUTH_HEADER_NAME, authHeader);
        testResourceAccess(PROTECTED_RESOURCE_URL, headers, expectedHttpStatus, bodyValidator);
    }

    protected void testProtectedResourceAccessWithHeaders(final Map<String, String> headers,
                                                          final int expectedHttpStatus,
                                                          final Consumer<Buffer> bodyValidator) throws Exception {
        testResourceAccess(PROTECTED_RESOURCE_URL, headers, expectedHttpStatus, bodyValidator);
    }

    void testResourceAccess(final String url,
                            final Optional<String> authHeader,
                            final int expectedHttpStatus,
                            final Consumer<Buffer> bodyValidator) throws Exception {

        final Map<String, String> headers = new HashMap<>();
        authHeader.ifPresent(header -> headers.put(AUTH_HEADER_NAME, header));
        testResourceAccess(url, headers, expectedHttpStatus, bodyValidator);
    }

    private void testResourceAccess(final String url,
                            final Map<String, String> headers,
                            final int expectedHttpStatus,
                            final Consumer<Buffer> bodyValidator) throws Exception {
        startWebServer();
        HttpClient client = vertx.createHttpClient();
        // Attempt to get a private url
        final HttpClientRequest request = client.get(8080, "localhost", url);
        headers.entrySet().stream()
                .filter(entry -> (entry.getValue() != null))
                .forEach(entry -> request.putHeader(entry.getKey(), entry.getValue()));
        // This should get the desired result straight away rather than operating through redirects
        request.handler(response -> {
            assertEquals(expectedHttpStatus, response.statusCode());
            response.bodyHandler(body -> {
                bodyValidator.accept(body);
                testComplete();
            });
        });
        request.end();
        await(2, TimeUnit.SECONDS);

    }


    protected abstract void startWebServer() throws Exception;

}
