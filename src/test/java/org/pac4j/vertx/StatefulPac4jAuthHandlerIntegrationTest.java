package org.pac4j.vertx;

import ext.apex.handler.oauth2.OAuth2ProviderMimic;
import io.vertx.core.Handler;
import io.vertx.core.VoidHandler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.junit.Test;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.vertx.auth.Pac4jAuthProvider;
import org.pac4j.vertx.auth.StatefulPac4jAuthProvider;
import org.pac4j.vertx.client.TestOAuth2Client;
import org.pac4j.vertx.core.DefaultJsonConverter;
import org.pac4j.vertx.handler.impl.Pac4jAuthHandlerOptions;
import org.pac4j.vertx.handler.impl.StatefulPac4jAuthHandler;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * User: jez
 */
public class StatefulPac4jAuthHandlerIntegrationTest extends Pac4jAuthHandlerIntegrationTestBase {

  private static final String TEST_CLIENT_ID = "testClient";
  private static final String TEST_CLIENT_SECRET = "testClientSecret";
  private static final String TEST_OAUTH2_SUCCESS_URL = "http://localhost:9292/authSuccess";
  private static final String TEST_OAUTH2_TOKEN_URL = "http://localhost:9292/authToken";
  public static final String APPLICATION_SERVER = "http://localhost:8080";
  private static final String AUTH_RESULT_HANDLER_URL = APPLICATION_SERVER + "/authResult";
  private static final String SESSION_PARAM_TOKEN = "testOAuth2Token";

  // This will be our session cookie header for use by requests
  protected AtomicReference<String> sessionCookie = new AtomicReference<>();

  public void startOAuth2ProviderMimic(final String userIdToReturn) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    final OAuth2ProviderMimic mimic = new OAuth2ProviderMimic(userIdToReturn);

    vertx.deployVerticle(mimic, result -> latch.countDown());
    latch.await(2, TimeUnit.SECONDS);
  }

  @Test
  public void testSuccessfulOAuth2LoginWithoutAuthorities() throws Exception {

    startOAuth2ProviderMimic("testUser1");
    startWebServer(TEST_OAUTH2_SUCCESS_URL);
    loginSuccessfullyExpectingAuthorizedUser(new VoidHandler() {
      @Override
      protected void handle() {
        testComplete();

      }
    });

    await(1, TimeUnit.SECONDS);
  }
  
  private void loginSuccessfullyExpectingAuthorizedUser(final VoidHandler subsequentActions) throws Exception {
    loginSuccessfully(finalRedirectResponse -> {
      assertEquals(200, finalRedirectResponse.statusCode());
      finalRedirectResponse.bodyHandler(body -> {
        assertEquals("authenticationSuccess", body.toString());
        subsequentActions.handle(null);
      });
    });
  }

  private void loginSuccessfullyExpectingUnauthorizedUser(final VoidHandler subsequentActions) throws Exception {
    loginSuccessfully(finalRedirectResponse -> {
      assertEquals(403, finalRedirectResponse.statusCode());
      subsequentActions.handle(null);
    });
  }

  private void loginSuccessfully(final Handler<HttpClientResponse> finalResponseHandler) throws Exception {
    HttpClient client = vertx.createHttpClient();
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
            finalResponseHandler::handle)))
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

  private Handler<HttpClientResponse> expectAndHandleRedirect(final HttpClient client,
                                                              final Consumer<HttpClientResponse> responseConsumer,
                                                              final Handler<HttpClientResponse> redirectResultHandler) {
    return response -> {
      assertEquals(302, response.statusCode());
      responseConsumer.accept(response);
      final String redirectToUrl = response.getHeader("location");
      redirectToUrl(redirectToUrl, client, redirectResultHandler);
    };
  }

  private void startWebServer(final String baseAuthUrl) {
    startWebServer(baseAuthUrl, handler -> {
    });
  }

  private void startWebServer(final String baseAuthUrl, final Consumer<AuthHandler> handlerDecorator) {
    Router router = Router.router(vertx);
    SessionStore sessionStore = sessionStore();

    router.route().handler(CookieHandler.create());
    router.route().handler(sessionHandler(sessionStore));

    StatefulPac4jAuthHandler pac4jAuthHandler = authHandler(router, sessionStore, baseAuthUrl);
    handlerDecorator.accept(pac4jAuthHandler);

    startWebServer(router, pac4jAuthHandler);
  }

  private StatefulPac4jAuthHandler authHandler(final Router router, final SessionStore sessionStore, final String baseAuthUrl) {
    DefaultJsonConverter ebConverter = new DefaultJsonConverter();
    Pac4jWrapper wrapper = new Pac4jWrapper(vertx, clients(client(baseAuthUrl)));
    Pac4jAuthProvider authProvider = StatefulPac4jAuthProvider.create(sessionStore, ebConverter);
    Pac4jAuthHandlerOptions options = new Pac4jAuthHandlerOptions(TEST_CLIENT_NAME);
    return new StatefulPac4jAuthHandler(router, wrapper, authProvider, options);
  }

  private void redirectToUrl(final String redirectUrl, final HttpClient client, final Handler<HttpClientResponse> resultHandler) {
    final HttpClientRequest request = client.getAbs(redirectUrl);
    getSessionCookie().ifPresent(cookie -> request.putHeader("cookie", cookie));
    request.handler(resultHandler);
    request.end();
  }

  private Optional<String> getSessionCookie() {
    return Optional.ofNullable(sessionCookie.get());
  }


  private JsonObject clientConfig(final String baseAuthUrl) {
    return new JsonObject()
      .put("callbackUrl", "http://localhost:8080/authResult")
      .put("clients", clients(baseAuthUrl));
  }

  private JsonObject clients(final String baseAuthUrl) {
    return new JsonObject()
      .put("testClient", new JsonObject()
        .put("class", TestOAuth2Client.class.getName())
        .put("props", testClientProps(baseAuthUrl)));
  }

  private JsonObject testClientProps(final String baseAuthUrl) {
    return new JsonObject()
      .put("key", TEST_CLIENT_ID)
      .put("secret", TEST_CLIENT_SECRET)
      .put("authorizationUrlTemplate", baseAuthUrl + "?client_id=%s&redirect_uri=%s&state=%s");
//      .put("authorizationUrlTemplate", "http://localhost:9292/authSuccess?client_id=%s&redirect_uri=%s&state=%s");
  }

  private Clients clients(final Client client) {
    Clients clients = new Clients();
    clients.setClients(client);
    return clients;
  }

  private TestOAuth2Client client(final String baseAuthUrl) {
    TestOAuth2Client client = new TestOAuth2Client();
    client.setCallbackUrl("http://localhost:8080/authResult");
    client.setKey(TEST_CLIENT_ID);
    client.setSecret(TEST_CLIENT_SECRET);
    client.setAuthorizationUrlTemplate(baseAuthUrl + "?client_id=%s&redirect_uri=%s&state=%s");
    return client;
  }

  private SessionHandler sessionHandler(SessionStore sessionStore) {
    return SessionHandler.create(sessionStore).setSessionCookieName("oAuth2Consumer.session");
  }

  private LocalSessionStore sessionStore() {
    return LocalSessionStore.create(vertx);
  }

}
