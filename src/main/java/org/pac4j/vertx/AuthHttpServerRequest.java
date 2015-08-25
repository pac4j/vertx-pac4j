package org.pac4j.vertx;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.pac4j.core.profile.UserProfile;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

/**
 * Wrapper around an {@link HttpServerRequest} that carries a User Profile.
 *
 * @author Michael Remond
 * @since 1.1.0
 *
 */
public class AuthHttpServerRequest implements HttpServerRequest {

  private final HttpServerRequest req;
  private UserProfile profile;

  public AuthHttpServerRequest(HttpServerRequest req) {
    this.req = req;
  }

  public UserProfile getProfile() {
    return profile;
  }

  public void setProfile(UserProfile profile) {
    this.profile = profile;
  }


  @Override
  public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
    return req.exceptionHandler(handler);
  }

  @Override
  public HttpServerRequest handler(Handler<Buffer> handler) {
    return req.handler(handler);
  }

  @Override
  public HttpServerRequest pause() {
    return req.pause();
  }

  @Override
  public HttpServerRequest resume() {
    return req.resume();
  }

  @Override
  public HttpServerRequest endHandler(Handler<Void> handler) {
    return req.endHandler(handler);
  }

  @Override
  public HttpVersion version() {
    return req.version();
  }

  @Override
  public HttpMethod method() {
    return req.method();
  }

  @Override
  public String uri() {
    return req.uri();
  }

  @Override
  public String path() {
    return req.path();
  }

  @Override
  public String query() {
    return req.query();
  }

  @Override
  public HttpServerResponse response() {
    return req.response();
  }

  @Override
  public MultiMap headers() {
    return req.headers();
  }

  @Override
  public String getHeader(String s) {
    return req.getHeader(s);
  }

  @Override
  public MultiMap params() {
    return req.params();
  }

  @Override
  public String getParam(String s) {
    return req.getParam(s);
  }

  @Override
  public SocketAddress remoteAddress() {
    return req.remoteAddress();
  }

  @Override
  public SocketAddress localAddress() {
    return req.localAddress();
  }

  @Override
  public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
    return req.peerCertificateChain();
  }

  @Override
  public String absoluteURI() {
    return req.absoluteURI();
  }

  @Override
  public HttpServerRequest bodyHandler(Handler<Buffer> handler) {
    return req.bodyHandler(handler);
  }

  @Override
  public NetSocket netSocket() {
    return req.netSocket();
  }

  @Override
  public HttpServerRequest setExpectMultipart(boolean b) {
    return req.setExpectMultipart(b);
  }

  @Override
  public boolean isExpectMultipart() {
    return req.isExpectMultipart();
  }

  @Override
  public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> handler) {
    return req.uploadHandler(handler);
  }

  @Override
  public MultiMap formAttributes() {
    return req.formAttributes();
  }

  @Override
  public String getFormAttribute(String s) {
    return req.getFormAttribute(s);
  }

  @Override
  public ServerWebSocket upgrade() {
    return req.upgrade();
  }

  @Override
  public boolean isEnded() {
    return req.isEnded();
  }
}
