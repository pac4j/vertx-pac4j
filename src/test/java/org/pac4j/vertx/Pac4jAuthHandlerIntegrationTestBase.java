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
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.test.core.VertxTestBase;
import org.pac4j.core.authorization.Authorizer;
import org.pac4j.core.authorization.RequireAllPermissionsAuthorizer;
import org.pac4j.vertx.profile.TestOAuth2Profile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public abstract class Pac4jAuthHandlerIntegrationTestBase extends VertxTestBase {

    protected static final String TEST_CLIENT_NAME = "TestOAuth2Client";
    protected static final String REQUIRE_ALL_AUTHORIZER = "requireAllAuthorizer";

    protected void startWebServer(Router router, Handler<RoutingContext> authHandler) throws Exception {
        HttpServer server = vertx.createHttpServer();

        router.route("/private/*").handler(authHandler);
        router.route().handler(StaticHandler.create());

        CountDownLatch latch = new CountDownLatch(1);
        server.requestHandler(router::accept).listen(8080, asyncResult -> {
            if (asyncResult.succeeded()) {
                latch.countDown();
            } else {
                fail("Http server failed to start so test could not proceed");
            }
        });
        assertTrue(latch.await(1L, TimeUnit.SECONDS));
    }

    protected Map<String, Authorizer> authorizers(final List<String> permissions) {
        return new HashMap<String, Authorizer>() {{
            put(REQUIRE_ALL_AUTHORIZER, authorizer(permissions));
        }};
    }

    private RequireAllPermissionsAuthorizer<TestOAuth2Profile> authorizer(final List<String> permissions) {
        final RequireAllPermissionsAuthorizer<TestOAuth2Profile> authorizer = new RequireAllPermissionsAuthorizer<TestOAuth2Profile>();
        authorizer.setPermissions(permissions);
        return authorizer;
    }
}
