package org.pac4j.vertx.auth.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.SessionStore;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.Pac4jSessionAttributes;
import org.pac4j.vertx.auth.Pac4jUser;
import org.pac4j.vertx.auth.StatefulPac4jAuthProvider;
import org.pac4j.vertx.core.JsonConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author jez
 */
public class StatefulPac4jAuthProviderImpl implements org.pac4j.vertx.auth.StatefulPac4jAuthProvider {

  private static final String SESSION_ATTRIBUTES = "pac4j_session_attributes";
  private static final String KEY_ATTRIBUTES = "attributes";
  private static final String KEY_USER_PROFILE = "userProfile";

  /**
   * The session store in which to look for the session to interrogate
   */
  private final SessionStore sessionStore;
  private final JsonConverter jsonConverter;

  public StatefulPac4jAuthProviderImpl(final SessionStore sessionStore, final JsonConverter converter) {
    this.sessionStore = sessionStore;
    this.jsonConverter = converter;
  }

  @Override
  public Pac4jSessionAttributes getSessionAttributes(final RoutingContext ctx) {
    return  Optional.ofNullable(ctx.session())
      .<JsonObject>flatMap(session -> Optional.<JsonObject>ofNullable(session.get(SESSION_ATTRIBUTES)))
      .flatMap(json -> Optional.of(decodeSessionAttributesFromJson(json)))
        .orElse(new Pac4jSessionAttributes());
  }

  @Override
  public JsonObject getAuthInfo(final RoutingContext routingContext) {
    return new JsonObject().put("sessionId", routingContext.session().id());
  }

  @Override
  public void checkPrerequisites(final RoutingContext ctx) {
    final Session session = ctx.session();
    if (session == null) {
      throw new NullPointerException("No session - did you forget to include a SessionHandler?");
    }
  }

  @Override
  public void authenticate(final JsonObject authInfo, final Handler<AsyncResult<User>> resultHandler) {
    sessionStore.get(authInfo.getString("sessionId"), sessionResult -> {
      if (sessionResult.succeeded()) {
        final StatefulPac4jAuthProvider provider = this;

        Optional<Pac4jUser> user = Optional.ofNullable(sessionResult.result())
          .flatMap(session -> Optional.<JsonObject>ofNullable(session.get(SESSION_ATTRIBUTES)))
          .<Pac4jSessionAttributes>flatMap(json -> Optional.of(decodeSessionAttributesFromJson(json)))
          .flatMap(Pac4jSessionAttributes::getUserProfile)
          .flatMap(profile -> Optional.of(new Pac4jUser(profile, provider)));

        if (user.isPresent()) {
          resultHandler.handle(Future.succeededFuture());
        } else {
          resultHandler.handle(Future.failedFuture("Token not present"));
        }
      } else {
        resultHandler.handle(Future.failedFuture("Session could not be found"));
      }
    });
  }

  @Override
  public void saveSessionAttributes(final RoutingContext routingContext, final Pac4jSessionAttributes sessionAttributes) {
    routingContext.session().put(SESSION_ATTRIBUTES, encodeSessionAttributesToJson(sessionAttributes));
  }

  private JsonObject encodeSessionAttributesToJson(final Pac4jSessionAttributes sessionAttributes) {
    final JsonObject serializedSessionAttributes = new JsonObject();
    final JsonObject serializedCustomAttributes = new JsonObject();

    sessionAttributes.getCustomAttributes().forEach((key, val) ->
      serializedCustomAttributes.put(key, jsonConverter.encodeObject(val)));
    serializedSessionAttributes.put(KEY_ATTRIBUTES, serializedCustomAttributes);
    sessionAttributes.getUserProfile().ifPresent(profile ->
        serializedSessionAttributes.put(KEY_USER_PROFILE, jsonConverter.encodeObject(profile))
    );
    return serializedSessionAttributes;
  }

  private Pac4jSessionAttributes decodeSessionAttributesFromJson(final JsonObject json) {

    final JsonObject jsonObject = json.getJsonObject(KEY_ATTRIBUTES);
    final Map<String, Object> customAttributes = new HashMap<>();
    jsonObject.fieldNames().forEach(key -> customAttributes.put(key,
      jsonConverter.decodeObject(jsonObject.getValue(key))));

    final Pac4jSessionAttributes sessionAttributes = new Pac4jSessionAttributes(customAttributes);
    final Optional<JsonObject> profileOpt = Optional.ofNullable(json.getJsonObject(KEY_USER_PROFILE));
    profileOpt.ifPresent(profile -> sessionAttributes.setUserProfile((UserProfile) jsonConverter.decodeObject(profile)));
    return sessionAttributes;
  }

}
