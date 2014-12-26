/*
  Copyright 2014 - 2014 pac4j organization

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package org.pac4j.vertx;

import java.net.InetSocketAddress;
import java.net.URI;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import org.pac4j.core.profile.UserProfile;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.net.NetSocket;

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
    public HttpServerRequest endHandler(Handler<Void> endHandler) {
        return req.endHandler(endHandler);
    }

    @Override
    public HttpServerRequest dataHandler(Handler<Buffer> handler) {
        return req.dataHandler(handler);
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
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        return req.exceptionHandler(handler);
    }

    @Override
    public HttpVersion version() {
        return req.version();
    }

    @Override
    public String method() {
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
    public MultiMap params() {
        return req.params();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return req.remoteAddress();
    }

    @Override
    public InetSocketAddress localAddress() {
        return req.localAddress();
    }

    @Override
    public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
        return req.peerCertificateChain();
    }

    @Override
    public URI absoluteURI() {
        return req.absoluteURI();
    }

    @Override
    public HttpServerRequest bodyHandler(Handler<Buffer> bodyHandler) {
        return req.bodyHandler(bodyHandler);
    }

    @Override
    public NetSocket netSocket() {
        return req.netSocket();
    }

    @Override
    public HttpServerRequest expectMultiPart(boolean expect) {
        return req.expectMultiPart(expect);
    }

    @Override
    public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> uploadHandler) {
        return req.uploadHandler(uploadHandler);
    }

    @Override
    public MultiMap formAttributes() {
        return req.formAttributes();
    }

}
