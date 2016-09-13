package org.pac4j.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Http server implementation to mimic an OAuth2 provider's possible authentication outcomes. By default this will
 * be based on the endpoint used for authentication, and we will construct different handlers in the tests to
 * redirect to different endpoints to mimic the possible outcomes.
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class OAuth2ProviderMimic extends AbstractVerticle {

    public static final int OAUTH2PROVIDER_PORT = 9292;
    public static final String OAUTH2_PROVIDER_SUCCESS_ENDPOINT = "/authSuccess";
    public static final String OAUTH2_PROVIDER_TOKEN_ENDPOINT = "/authToken";
    public static final String OAUTH2_PROVIDER_PROFILE_ENDPOINT = "/profile";

    private Map<String, String> pendingCodes = new HashMap<>();
    private final String userIdToReturn;

    public OAuth2ProviderMimic(final String userIdToReturn) {
        this.userIdToReturn = userIdToReturn;
    }

    @Override
    public void start() throws Exception {
        vertx.createHttpServer().requestHandler(router()::accept).listen(OAUTH2PROVIDER_PORT);
    }

    private Router router() {
        Router router = Router.router(vertx);
        router.route(HttpMethod.GET, OAUTH2_PROVIDER_SUCCESS_ENDPOINT).handler(authSuccessHandler());
        router.route(HttpMethod.POST, OAUTH2_PROVIDER_TOKEN_ENDPOINT).handler(bodyHandlerForTokenRequest());
        router.route(HttpMethod.GET, OAUTH2_PROVIDER_TOKEN_ENDPOINT).handler(tokenRequestHandler());
        router.route(HttpMethod.GET, OAUTH2_PROVIDER_PROFILE_ENDPOINT).handler(profileHandler());
        return router;
    }

    private Handler<RoutingContext> authSuccessHandler() {
        // Create a full URL for redirection

        return rc -> {
            final MultiMap requestParams = rc.request().params();
            final String redirectUrl = requestParams.get("redirect_uri");
            final String responseType = requestParams.get("response_type");
            final String clientId = requestParams.get("client_id");
            final String state = requestParams.get("state");
            // We're not going to be sent scope as we default that
            final StringBuilder sb = new StringBuilder(redirectUrl);
            sb.append(redirectUrl.contains("?") ? "&" : "?");
            sb.append("state").append("=").append(state);
            sb.append("&").append("code").append("=").append(newCode(clientId, redirectUrl));
            rc.response().putHeader("location", sb.toString()).setStatusCode(302).end();
        };
    }

    private Handler<RoutingContext> bodyHandlerForTokenRequest() {
        return BodyHandler.create();
    }

    private Handler<RoutingContext> tokenRequestHandler() {
        return rc -> {
            // Extract parameters from request body - body handler should make these accessible from params collection
            final Optional<String> grantType = Optional.ofNullable(rc.request().getParam("grant_type"));
            final String code = rc.request().getParam("code");
            final String redirectUri = rc.request().getParam("redirect_uri");
            final String clientId = rc.request().getParam("client_id");

            Optional<String> token = grantType.flatMap(s -> {
                if (code == null || redirectUri == null || clientId == null) {
                    return Optional.empty();
                }
                return Optional.of(s.equals("authorization_code")).flatMap(b ->
                                b ? accessToken(clientId, redirectUri) : Optional.empty()
                ) ;
            });

            if (token.isPresent()) {
                JsonObject responseBody = new JsonObject().put("access_token", token.get())
                        .put("token_type", "Bearer")
                        .put("expires_in", 5000);
                rc.response().setStatusCode(200).end(responseBody.toString());
            } else {
                rc.fail(401); // We couldn't resolve to a token
            }


        };
    }

    private Handler<RoutingContext> profileHandler() {
        return rc -> {
            final JsonObject responseBody = new JsonObject().put("id", userIdToReturn);
            rc.response().setStatusCode(200).end(responseBody.toString());
        };
    }



    private Optional<String> accessToken(final String clientId, final String redirectUri) {
        Optional<String> token = Optional.ofNullable(pendingCodes.get(getKey(clientId, redirectUri))).flatMap(code ->
                        Optional.of(UUID.randomUUID().toString())
        );
        return token;
    }

    private String getKey(final String clientId, final String redirectUri) {
        return clientId + "::" + redirectUri;
    }

    private String newCode(final String clientId, final String redirectUri) {
        final String newCode = UUID.randomUUID().toString();
        pendingCodes.put(getKey(clientId, redirectUri), newCode);
        return newCode;
    }
}
