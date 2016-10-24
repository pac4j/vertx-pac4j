package org.pac4j.vertx;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.test.core.VertxTestBase;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.authorization.authorizer.RequireAllPermissionsAuthorizer;
import org.pac4j.vertx.profile.TestOAuth2Profile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public abstract class Pac4jAuthHandlerIntegrationTestBase extends VertxTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(Pac4jAuthHandlerIntegrationTestBase.class);

    static final String EXCLUDED_PROTECTED_RESOURCE_URL = "/private/public/success.html";
    static final String UNPROTECTED_RESOURCE_BODY = "Unprotected resource";
    static final String TEST_CLIENT_NAME = "TestOAuth2Client";
    static final String REQUIRE_ALL_AUTHORIZER = "requireAllAuthorizer";
    static final String FORBIDDEN_BODY = "Forbidden to access this resource";
    static final String UNAUTHORIZED_BODY = "Unauthorized for resource";

    void startWebServer(Router router, Handler<RoutingContext> authHandler) throws Exception {
        HttpServer server = vertx.createHttpServer();

        router.route("/private/*").handler(authHandler);
        router.route(EXCLUDED_PROTECTED_RESOURCE_URL).handler(rc -> rc.response().end(UNPROTECTED_RESOURCE_BODY));
        router.route("/private/success.html").handler(loginSuccessHandler()); // Spit out the user
        router.route().failureHandler(rc -> {
            rc.response().setStatusCode(rc.statusCode());
            switch(rc.statusCode()) {

                case 401:
                    rc.response().end(UNAUTHORIZED_BODY);
                    break;

                case 403:
                    rc.response().end(FORBIDDEN_BODY);
                    break;

                default:
                    rc.response().end("Unexpected error");
            }
        });

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

    private Handler<RoutingContext> loginSuccessHandler() {
        // Just write out the routing context's user principal, we can then validate against this
        return rc -> {
            LOG.info("Login success");
            final User user = rc.user();
            final JsonObject json = user != null ? user.principal() : new JsonObject();
            rc.response().end(json.encodePrettily());
        };
    }

    Map<String, Authorizer> authorizers(final List<String> permissions) {
        return new HashMap<String, Authorizer>() {{
            put(REQUIRE_ALL_AUTHORIZER, authorizer(permissions));
        }};
    }

    private RequireAllPermissionsAuthorizer<TestOAuth2Profile> authorizer(final List<String> permissions) {
        final RequireAllPermissionsAuthorizer<TestOAuth2Profile> authorizer = new RequireAllPermissionsAuthorizer<>();
        authorizer.setElements(permissions);
        return authorizer;
    }

    Consumer<String> protectedResourceContentValidator() {
        return body -> {
            final JsonObject json = new JsonObject(body);
            validateProtectedResourceContentFollowingInitialLogin(json);
        };
    }

    protected void validateInitialLoginSuccessResponse(final HttpClientResponse response, final Consumer<Void> subsequentActions) {
        response.bodyHandler(body -> {
            protectedResourceContentValidator().accept(body.toString());
            subsequentActions.accept(null);
        });
    }

    protected abstract void validateProtectedResourceContentFollowingInitialLogin(final JsonObject jsonObject);
}
