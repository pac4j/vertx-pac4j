package org.pac4j.vertx.auth;

import io.vertx.ext.web.sstore.SessionStore;
import org.pac4j.vertx.auth.impl.StatefulPac4jAuthProviderImpl;
import org.pac4j.vertx.core.JsonConverter;

/**
 * @author jez
 */
public interface StatefulPac4jAuthProvider extends Pac4jAuthProvider {

  static StatefulPac4jAuthProvider create(final SessionStore sessionStore, final JsonConverter jsonConverter) {
    return new StatefulPac4jAuthProviderImpl(sessionStore, jsonConverter);
  }

}
