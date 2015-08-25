package org.pac4j.vertx;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.UserProfile;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author jez
 */
public class Pac4jWrapper {

  private static final Logger LOG = LoggerFactory.getLogger(Pac4jWrapper.class);

  private final Clients clients;
  private final Vertx vertx;

  public Pac4jWrapper(final Vertx vertx, final Clients clients) {
    this.vertx = vertx;
    if (clients.findAllClients().size() == 0) {
      throw new RuntimeException("Pac4jWrapper requires at least one pac4j client to be used");
    }
    this.clients = clients;
  }

  public boolean requiresHttpAction(Pac4jResponse response) {
    return response.getCode() != 0;
  }

  /**
   * Send a redirect message to the pac4j manager. The response will contain the http information to send back to the user.
   *
   * @param routingContext the vert.x RoutingContext
   * @param sessionAttributes the session attributes
   * @param clientName the client name
   * @param protectedResource whether it is a protected resource
   * @param isAjax whether it is an AJAX call
   * @param resultHandler the handler
   */
  public void redirect(final RoutingContext routingContext, final String clientName,
                       final Pac4jSessionAttributes sessionAttributes, final boolean protectedResource,
                       final boolean isAjax, final Handler<Pac4jResponse> resultHandler) {
    // execute blocking call to doRedirect here
    vertx.<Pac4jResponse>executeBlocking(future -> {

      Objects.requireNonNull(clientName);
      Objects.requireNonNull(sessionAttributes);
      VertxWebContext webContext = new VertxWebContext(routingContext, sessionAttributes);

      Client client = clients.findClient(clientName);
      LOG.debug("client : " + client);

      try {
        client.redirect(webContext, protectedResource, isAjax);
      } catch (RequiresHttpAction e) {
        LOG.debug("extra HTTP action required : " + e.getCode());
      } catch (RuntimeException e) {
        LOG.error("unexpected exception during doRedirect", e);
        future.fail(e);
        return;
      }

      final Pac4jResponse response = new Pac4jResponse(webContext);

      completeWithOk(future, response);

    }, asyncResult -> {
      if (asyncResult.succeeded()) {
        resultHandler.handle(asyncResult.result());
      } else {
        // This should trigger a normal 500 failure so no need for special exception handling
        throw new RuntimeException("Redirection failure", asyncResult.cause());
      }
    });
  }



  @SuppressWarnings("rawtypes")
  public List<String> getRedirectUrls(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes,
                                      final String... clientNames) {
    final VertxWebContext webContext = new VertxWebContext(routingContext, sessionAttributes);
    Objects.requireNonNull(webContext);

    return Arrays.asList(clientNames).stream()
      .map(clientName -> clients.findClient((String) clientName))
      .filter(client -> client instanceof IndirectClient)
      .map(client -> (IndirectClient) client)
      .map(indirectClient -> {
        try {
          return Optional.ofNullable(indirectClient.getRedirectAction(webContext, false, false).getLocation());
        } catch (RequiresHttpAction requiresHttpAction) {
          return Optional.<String>empty();
        }
      }).filter(Optional::<String>isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());

  }


  /**
   * Send an authenticate message to the pac4j manager. The response will contain the user profile if the authentication succeeds or the
   * http information to send back to the user otherwise.
   *
   * @param routingContext the vertx-web routing context
   * @param sessionAttributes the session attributes
   * @param clientName the client name
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void authenticate(final RoutingContext routingContext, final String clientName,
                           final Pac4jSessionAttributes sessionAttributes, Handler<Pac4jAuthenticationResponse> resultHandler) {
    vertx.<Pac4jAuthenticationResponse>executeBlocking(future -> {

      VertxWebContext webContext = new VertxWebContext(routingContext, sessionAttributes);

      final Pac4jAuthenticationResponse authResponse = new Pac4jAuthenticationResponse(webContext);
      final Client client = (clientName != null) ? clients.findClient(clientName) : clients.findClient(webContext);

      try {
        Credentials credentials = client.getCredentials(webContext);
        UserProfile userProfile = client.getUserProfile(credentials, webContext);
        authResponse.setProfile(userProfile);
      } catch (RequiresHttpAction e) {
        LOG.debug("requires HTTP action : " + e.getCode());
      } catch (RuntimeException e) {
        LOG.error("unexpected exception during doAuthenticate", e);
        future.fail(e);
        return;
      }

      completeWithOk(future, authResponse);

    }, asyncResult -> {
      if (asyncResult.succeeded()) {
        resultHandler.handle(asyncResult.result());
      } else {
        sendErrorResponse(routingContext.response());
      }

    });
  }

  public void sendErrorResponse(HttpServerResponse serverResponse) {
    serverResponse.setStatusCode(500);
    serverResponse.end("Pac4J internal failure");
  }

  private <T extends Pac4jResponse> void completeWithOk(final Future<T> future, final T response) {
    Objects.requireNonNull(response);
    future.complete(response);
  }

}
