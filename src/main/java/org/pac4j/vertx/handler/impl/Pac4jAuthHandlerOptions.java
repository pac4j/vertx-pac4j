package org.pac4j.vertx.handler.impl;

/**
 * @author jez
 */
public class Pac4jAuthHandlerOptions {

  private final String clientName;
  private boolean allowDynamicClientSelection = false;

  public Pac4jAuthHandlerOptions(String clientName) {
    if (clientName == null) {
      throw new IllegalArgumentException("clientName must not be null");
    }
    this.clientName = clientName;
  }

  public String clientName() {
    return clientName;
  }

  public Pac4jAuthHandlerOptions setAllowDynamicClientSelection(final boolean allowDynamicClientSelection) {
    this.allowDynamicClientSelection = allowDynamicClientSelection;
    return this;
  }

  public boolean allowDynamicClientSelection() {
    return allowDynamicClientSelection;
  }
}
