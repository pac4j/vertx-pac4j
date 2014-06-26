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
import java.util.Map.Entry;

import org.pac4j.core.profile.UserProfile;
import org.scribe.model.Token;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Utility class for encoding and decoding objects for the event bus. This is required for
 * session attributes and the user profile.<br>
 * Custom converters can be registered with the addConverter method. 
 * 
 * @author Michael Remond
 * @since 1.0.0
 *
 */
public class EventBusObjectConverter {

    private static Map<String, Converter<? extends Object>> map;

    static {
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
    public static void addConverter(String className, Converter<? extends Object> converter) {
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
    public static Object encode(Object value) {
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

    private static Converter<? extends Object> getConverter(Object value) {
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

    /**
     * Decode given object using the corresponding decoder if available. Returns a String representation otherwise.
     * 
     * @param value
     * @return
     */
    public static Object decode(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonObject) {
            JsonObject json = (JsonObject) value;
            Converter<? extends Object> converter = map.get(json.getString("class"));
            if (converter != null) {
                return converter.decode(json.getValue("value"));
            }
        }
        return value.toString();
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
    private static class TokenConverter implements Converter<Token> {

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
    private static class UserProfileConverter implements Converter<UserProfile> {

        @Override
        public Object encode(UserProfile t) {
            JsonObject json = new JsonObject().putString("id", t.getId()).putBoolean("remembered", t.isRemembered())
                    .putArray("permissions", new JsonArray(t.getPermissions().toArray()))
                    .putArray("roles", new JsonArray(t.getRoles().toArray()));
            Map<String, Object> attributes = t.getAttributes();
            JsonObject jsonAttr = new JsonObject();
            for (Entry<String, Object> entry : attributes.entrySet()) {
                jsonAttr.putValue(entry.getKey(), EventBusObjectConverter.encode(entry.getValue()));
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
                profile.addAttribute(name, EventBusObjectConverter.decode(jsonAttributes.getValue(name)));
            }
            return profile;
        }

    }

}
