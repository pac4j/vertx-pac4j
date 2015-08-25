package org.pac4j.vertx.handler.impl;

/**
 * @author jez
 */
public class Pac4jAuthHandlerOptions {

  private final String clientName;
  private boolean isAjax = false;
  private String requireAnyRole = "";
  private String requireAllRoles = "";

  public Pac4jAuthHandlerOptions(String clientName) {
    if (clientName == null) {
      throw new IllegalArgumentException("clientName must not be null");
    }
    this.clientName = clientName;
  }

  public boolean isAjax() {
    return isAjax;
  }

  public Pac4jAuthHandlerOptions setIsAjax(boolean isAjax) {
    this.isAjax = isAjax;
    return this;
  }

  public String clientName() {
    return clientName;
  }
}
