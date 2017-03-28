package org.pac4j.vertx;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.vertx.auth.Pac4jUser;
import org.pac4j.vertx.core.DefaultJsonConverter;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WebContext implementation for Vert.x 3.
 *
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxWebContext implements WebContext {

    private final RoutingContext routingContext;
    private final String method;
    private final String serverName;
    private final int serverPort;
    private final String fullUrl;
    private final String scheme;
    private final String remoteAddress;
    private final JsonObject headers;
    private final JsonObject parameters;
    private final Map<String, String[]> mapParameters;
    private final SessionStore<VertxWebContext> sessionStore;

    private boolean contentHasBeenWritten = false; // Need to set chunked before first write of any content

    public VertxWebContext(final RoutingContext routingContext, final SessionStore<VertxWebContext> sessionStore) {
        final HttpServerRequest request = routingContext.request();
        this.routingContext = routingContext;
        this.method = request.method().toString();
        this.sessionStore = sessionStore;

        this.fullUrl = request.absoluteURI();
        URI uri;
        try {
            uri = new URI(fullUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new InvalidParameterException("Request to invalid URL " + fullUrl + " while constructing VertxWebContext");
        }
        this.scheme = uri.getScheme();
        this.serverName = uri.getHost();
        this.serverPort = (uri.getPort() != -1) ? uri.getPort() : scheme.equals("http") ? 80 : 443;
        this.remoteAddress = request.remoteAddress().toString();

        headers = new JsonObject();
        for (String name : request.headers().names()) {
            headers.put(name, request.headers().get(name));
        }

        parameters = new JsonObject();
        for (String name : request.params().names()) {
            parameters.put(name, new JsonArray(Arrays.asList(request.params().getAll(name).toArray())));
        }

        mapParameters = new HashMap<>();
        for (String name : parameters.fieldNames()) {
            JsonArray params = parameters.getJsonArray(name);
            String[] values = new String[params.size()];
            int i = 0;
            for (Object o : params) {
                values[i++] = (String) o;
            }
            mapParameters.put(name, values);
        }

    }

    public void failResponse(final int status) {
        routingContext.fail(status);
    }

    public void completeResponse() {
        routingContext.response().end();
    }

    @Override
    public String getRequestParameter(String name) {
        JsonArray values = parameters.getJsonArray(name);
        if (values != null && values.size() > 0) {
            return values.getString(0);
        }
        return null;
    }

    @Override
    public Map<String, String[]> getRequestParameters() {
        return mapParameters;
    }

    @Override
    public Object getRequestAttribute(String s) {
        return routingContext.get(s);
    }

    @Override
    public void setRequestAttribute(String s, Object o) {
        routingContext.put(s, o);
    }

    @Override
    public String getRequestHeader(String name) {
        return headers.getString(name);
    }

    @Override
    public void setSessionAttribute(String name, Object value) {
        Session session = routingContext.session();
        if (session == null) {
            throw new IllegalStateException("Session required for use of getSessionAttribute");
        }
        // Need to convert to something that can be passed round a distributed vert.x session cleanly
        if (value == null) {
            session.remove(name);
        } else {
            session.put(name, DefaultJsonConverter.getInstance().encodeObject(value));
        }
    }

    @Override
    public Object getSessionAttribute(String name) {
        Session session = routingContext.session();
        if (session == null) {
            throw new IllegalStateException("Session required for use of getSessionAttribute");
        }
        return DefaultJsonConverter.getInstance().decodeObject(session.get(name));
    }

    @Override
    public String getSessionIdentifier() {
        return routingContext.session().id();
    }

    @Override
    public String getRequestMethod() {
        return method;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddress;
    }

    @Override
    public void writeResponseContent(String content) {
        if (content != null && !content.isEmpty()) {
            if (!contentHasBeenWritten) {
                routingContext.response().setChunked(true);
                contentHasBeenWritten = true;
            }
            routingContext.response().write(content);
        }
    }

    @Override
    public void setResponseStatus(int code) {
        routingContext.response().setStatusCode(code);
    }

    @Override
    public void setResponseHeader(String name, String value) {
        routingContext.response().putHeader(name, value);
    }

    public Map<String, String> getResponseHeaders() {
        return routingContext.response().headers().entries().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                    Map.Entry::getValue));
    }

    @Override
    public void setResponseContentType(String s) {
        routingContext.response().headers().add("Content-Type", s);
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public boolean isSecure() {
        return getScheme().equals("https");
    }

    @Override
    public String getFullRequestURL() {
        return fullUrl;
    }

    @Override
    public Collection<Cookie> getRequestCookies() {
        return routingContext.cookies().stream().map(cookie -> {
            final Cookie p4jCookie = new Cookie(cookie.getName(), cookie.getValue());
            p4jCookie.setDomain(cookie.getDomain());
            p4jCookie.setPath(cookie.getPath());
            return p4jCookie;
        }).collect(Collectors.toList());
    }

    @Override
    public void addResponseCookie(Cookie cookie) {
        routingContext.addCookie(io.vertx.ext.web.Cookie.cookie(cookie.getName(), cookie.getValue()));
    }

    @Override
    public String getPath() {
        return routingContext.request().path();
    }

    @Override
    public SessionStore getSessionStore() {
        return this.sessionStore;
    }

    @Override
    public void setSessionStore(SessionStore sessionStore) {
        throw new UnsupportedOperationException("Not possible to change the session store for VertxWebContext");
    }

    public Pac4jUser getVertxUser() {
        return (Pac4jUser) routingContext.user();
    }

    public void removeVertxUser() {
        routingContext.clearUser();
    }

    public void setVertxUser(final Pac4jUser pac4jUser) {
        routingContext.setUser(pac4jUser);
    }

    public Session getVertxSession() {
        return routingContext.session();
    }

}
