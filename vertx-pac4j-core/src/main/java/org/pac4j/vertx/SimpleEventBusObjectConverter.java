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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.pac4j.core.profile.UserProfile;
import org.scribe.model.Token;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * {@link EventBusObjectConverter} implementation using registered converters to transform Java object into Vert.x {@link JsonObject}.<br>
 * Custom converters can be registered with the addConverter method. 
 * 
 * @author Michael Remond
 * @since 1.1.0
 *
 */
public class SimpleEventBusObjectConverter implements EventBusObjectConverter {

    private final Map<String, Converter<? extends Object>> map;

    public SimpleEventBusObjectConverter() {
        map = new HashMap<>();
        map.put("org.scribe.model.Token", new TokenConverter());
        map.put("org.pac4j.core.profile.UserProfile", new UserProfileConverter());
    }

    /**
     * Add the given converter for encoding/decoding the given class name. 
     * 
     * @param className
     * @param converter
     */
    public void addConverter(String className, Converter<? extends Object> converter) {
        if (!map.containsKey(className)) {
            map.put(className, converter);
        }
    }

    /**
     * Encode the given object using the corresponding encoder if available. Returns the String representation otherwise. 
     * 
     * @param value
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object encodeObject(Object value) {
        if (value == null) {
            return null;
        }
        String className = value.getClass().getName();
        Converter converter = getConverter(value);
        if (converter != null) {
            return new JsonObject().putString("class", className).putValue("value", converter.encode(value));
        } else {
            return value.toString();
        }
    }

    /**
     * Decode given object using the corresponding decoder if available. Returns a String representation otherwise.
     * 
     * @param value
     * @return
     */
    @Override
    public Object decodeObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonObject) {
            JsonObject json = (JsonObject) value;
            Converter<? extends Object> converter = getConverter(json.getString("class"));
            if (converter != null) {
                return converter.decode(json.getValue("value"));
            }
        }
        return value.toString();
    }

    private Converter<? extends Object> getConverter(Object value) {
        // Try to find an exact match
        for (Entry<String, Converter<? extends Object>> entry : map.entrySet()) {
            try {
                if (Class.forName(entry.getKey()).equals(value.getClass())) {
                    return entry.getValue();
                }
            } catch (ClassNotFoundException e) {
            }
        }
        // Try to find a compatible match
        for (Entry<String, Converter<? extends Object>> entry : map.entrySet()) {
            try {
                if (Class.forName(entry.getKey()).isInstance(value)) {
                    return entry.getValue();
                }
            } catch (ClassNotFoundException e) {
            }
        }
        return null;
    }

    private Converter<? extends Object> getConverter(String className) {
        // Try to find an exact match
        for (Entry<String, Converter<? extends Object>> entry : map.entrySet()) {
            try {
                if (entry.getKey().equals(className)) {
                    return entry.getValue();
                }
            } catch (Exception e) {
            }
        }
        // Try to find a compatible match
        for (Entry<String, Converter<? extends Object>> entry : map.entrySet()) {
            try {
                if (Class.forName(entry.getKey()).isAssignableFrom(Class.forName(className))) {
                    return entry.getValue();
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    /**
     * The converter interface for type T.
     *
     * @param <T>
     */
    public static interface Converter<T extends Object> {

        Object encode(T t);

        T decode(Object o);

    }

    /**
     * Converter for the scribe Token type.
     */
    private class TokenConverter implements Converter<Token> {

        @Override
        public Object encode(Token t) {
            return new JsonObject().putString("token", t.getToken()).putString("secret", t.getSecret());
        }

        @Override
        public Token decode(Object o) {
            JsonObject json = (JsonObject) o;
            return new Token(json.getString("token"), json.getString("secret"));
        }

    }

    /**
     * Converter for the UserProfile type.
     */
    private class UserProfileConverter implements Converter<UserProfile> {

        @Override
        public Object encode(UserProfile t) {
            JsonObject json = new JsonObject().putString("id", t.getId()).putBoolean("remembered", t.isRemembered())
                    .putArray("permissions", new JsonArray(t.getPermissions().toArray()))
                    .putArray("roles", new JsonArray(t.getRoles().toArray()));
            Map<String, Object> attributes = t.getAttributes();
            JsonObject jsonAttr = new JsonObject();
            for (Entry<String, Object> entry : attributes.entrySet()) {
                jsonAttr.putValue(entry.getKey(), encodeObject(entry.getValue()));
            }
            json.putObject("attributes", jsonAttr);
            return json;
        }

        @Override
        public UserProfile decode(Object o) {
            JsonObject json = (JsonObject) o;
            UserProfile profile = new UserProfile();
            profile.setId(json.getString("id"));
            profile.setRemembered(json.getBoolean("remembered"));
            for (Object perm : json.getArray("permissions")) {
                profile.addPermission((String) perm);
            }
            for (Object role : json.getArray("roles")) {
                profile.addRole((String) role);
            }
            JsonObject jsonAttributes = json.getObject("attributes");
            for (String name : jsonAttributes.getFieldNames()) {
                profile.addAttribute(name, decodeObject(jsonAttributes.getValue(name)));
            }
            return profile;
        }

    }

}
