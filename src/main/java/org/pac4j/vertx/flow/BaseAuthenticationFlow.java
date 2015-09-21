package org.pac4j.vertx.flow;

import io.vertx.core.Vertx;
import org.pac4j.core.client.Client;
import org.pac4j.vertx.handler.HttpActionHandler;
import org.pac4j.vertx.handler.impl.DefaultHttpActionHandler;

/**
 * @author jez
 */
public abstract class BaseAuthenticationFlow<T extends Client> implements AuthenticationFlow<T> {

  protected final HttpActionHandler httpActionHandler;
  protected final Vertx vertx;
  protected final T client;

  public BaseAuthenticationFlow(Vertx vertx, T client) {
    this.httpActionHandler = new DefaultHttpActionHandler();
    this.vertx = vertx;
    this.client = client;
  }
}
