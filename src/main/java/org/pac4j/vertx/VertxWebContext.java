/*
  Copyright 2014 - 2014 Michael Remond

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

import java.util.HashMap;
import java.util.Map;

import org.pac4j.core.context.WebContext;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * WebContext implementation for Vert.x.
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class VertxWebContext implements WebContext {

    private final JsonObject sessionAttributes;
    private String method;
    private String serverName;
    private int serverPort;
    private String fullUrl;
    private String scheme;
    private JsonObject headers;
    private JsonObject parameters;
    private Map<String, String[]> mapParameters;

    private JsonObject outHeaders = new JsonObject();
    private StringBuilder sb = new StringBuilder();
    private int code;

    public VertxWebContext(String method, String serverName, int serverPort, String fullUrl, String scheme,
            JsonObject headers, JsonObject parameters, JsonObject sessionAttributes) {
        this.method = method;
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.fullUrl = fullUrl;
        this.scheme = scheme;
        this.headers = headers;
        this.parameters = parameters;
        this.sessionAttributes = sessionAttributes;
        mapParameters = new HashMap<>();
        for (String name : parameters.getFieldNames()) {
            JsonArray params = parameters.getArray(name);
            String[] values = new String[params.size()];
            int i = 0;
            for (Object o : params) {
                values[i++] = (String) o;
            }
            mapParameters.put(name, values);
        }

    }

    @Override
    public String getRequestParameter(String name) {
        JsonArray values = parameters.getArray(name);
        if (values != null && values.size() > 0) {
            return values.get(0);
        }
        return null;
    }

    @Override
    public Map<String, String[]> getRequestParameters() {
        return mapParameters;
    }

    @Override
    public String getRequestHeader(String name) {
        return headers.getString(name);
    }

    @Override
    public void setSessionAttribute(String name, Object value) {
        sessionAttributes.putValue(name, EventBusObjectConverter.encode(value));
    }

    @Override
    public Object getSessionAttribute(String name) {
        return EventBusObjectConverter.decode(sessionAttributes.getValue(name));
    }

    public JsonObject getSessionAttributes() {
        return sessionAttributes;
    }

    @Override
    public String getRequestMethod() {
        return method;
    }

    @Override
    public void writeResponseContent(String content) {
        sb.append(content);
    }

    public String getResponseContent() {
        return sb.toString();
    }

    @Override
    public void setResponseStatus(int code) {
        this.code = code;
    }

    public int getResponseStatus() {
        return code;
    }

    @Override
    public void setResponseHeader(String name, String value) {
        outHeaders.putString(name, value);
    }

    public JsonObject getResponseHeaders() {
        return outHeaders;
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
    public String getFullRequestURL() {
        return fullUrl;
    }

}
