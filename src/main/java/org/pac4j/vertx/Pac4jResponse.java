package org.pac4j.vertx;

import io.vertx.core.json.JsonObject;

/**
 * Mostly immutable, type-safe class for passing results of Pac4j handling back to the handler or handlers (or helper)
 * The intent is to reduce the use of JsonObject which is essentially dynamically typed, to marginalise it as far as
 * possible.
 */
public class Pac4jResponse {

  private final int code;
  private final Pac4jSessionAttributes sessionAttributes;
  private final JsonObject headers;
  private final String content;

  // Constructor from fields provided in web context
  public Pac4jResponse(final VertxWebContext webContext) {
    sessionAttributes = new Pac4jSessionAttributes(webContext.getSessionAttributes());
    headers = webContext.getResponseHeaders().copy();
    code = webContext.getResponseStatus();
    content = webContext.getResponseContent();
  }

  public int getCode() {
    return code;
  }

  public Pac4jSessionAttributes getSessionAttributes() {
    return sessionAttributes;
  }

  public String getContent() {
    return content;
  }

  public JsonObject getHeaders() {
    return headers;
  }
}
