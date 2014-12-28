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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.scribe.model.Token;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

/**
 * <p>Default eventbus object converter</p>
 * <p>The serialization strategy is:
 * <ul>
 * <li>For primitive types (String, Number and Boolean), return as is</li>
 * <li>For arrays, convert to JsonArray</li>
 * <li>Otherwise, convert to a JsonObject with the class name in the "class" attribute and the serialized form with Jackson in the "value" attribute.
 * Custom deserializers can be registered using the <code>addDeserializer(type, deserializer)</code> method</li>
 * </ul>
 * </p>
 * 
 * @author Michael Remond
 * @since 1.1.0
 *
 */
public class DefaultEventBusObjectConverter implements EventBusObjectConverter {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SimpleModule module = new SimpleModule();

    public DefaultEventBusObjectConverter() {
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);

        module.addDeserializer(Token.class, new TokenDeserializer());
        module.addDeserializer(BearerAccessToken.class, new BearerAccessTokenDeserializer());
        mapper.registerModule(module);
    }

    public <T> void addDeserializer(Class<T> type, JsonDeserializer<? extends T> deser) {
        module.addDeserializer(type, deser);
        mapper.registerModule(module);
    }

    @Override
    public Object encodeObject(Object value) {
        if (value == null) {
            return null;
        } else if (isPrimitiveType(value)) {
            return value;
        } else if (value instanceof Object[]) {
            Object[] src = ((Object[]) value);
            List<Object> list = new ArrayList<>(src.length);
            for (Object object : src) {
                list.add(encodeObject(object));
            }
            return new JsonArray(list);
        } else {
            try {
                return new JsonObject().putString("class", value.getClass().getName())
                        .putString("value", encode(value));
            } catch (Exception e) {
                throw new RuntimeException("Error while encoding object", e);
            }
        }
    }

    @Override
    public Object decodeObject(Object value) {
        if (value == null) {
            return null;
        } else if (isPrimitiveType(value)) {
            return value;
        } else if (value instanceof JsonArray) {
            JsonArray src = (JsonArray) value;
            List<Object> list = new ArrayList<>(src.size());
            for (Object object : src) {
                list.add(decodeObject(object));
            }
            return list.toArray();
        } else if (value instanceof JsonObject) {
            JsonObject src = (JsonObject) value;
            try {
                return decode(src.getString("value"), Class.forName(src.getString("class")));
            } catch (Exception e) {
                throw new RuntimeException("Error while decoding object", e);
            }
        }
        return null;
    }

    private boolean isPrimitiveType(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    private String encode(Object value) throws JsonGenerationException, JsonMappingException, IOException {
        return mapper.writeValueAsString(value);
    }

    @SuppressWarnings("unchecked")
    private <T> T decode(String string, Class<?> clazz) throws JsonParseException, JsonMappingException, IOException {
        return (T) mapper.readValue(string, clazz);
    }

    private static class TokenDeserializer extends JsonDeserializer<Token> {

        @Override
        public Token deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            String token = node.get("token").asText();
            String secret = node.get("secret").asText();
            String rawResponse = node.get("rawResponse").asText();
            return new Token(token, secret, rawResponse);
        }

    }

    private static class BearerAccessTokenDeserializer extends JsonDeserializer<BearerAccessToken> {

        @Override
        public BearerAccessToken deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            return new BearerAccessToken(node.get("value").asText(), node.get("lifetime").asLong(), new Scope());
        }

    }

}
