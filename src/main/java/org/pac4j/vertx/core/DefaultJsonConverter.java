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

package org.pac4j.vertx.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.Scope.Value.Requirement;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.scribe.model.Token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Default eventbus object converter</p>
 * <p>The serialization strategy is:</p>
 * <ul>
 * <li>For primitive types (String, Number and Boolean), return as is</li>
 * <li>For arrays, convert to JsonArray</li>
 * <li>Otherwise, convert to a JsonObject with the class name in the "class" attribute and the serialized form with Jackson in the "value" attribute.
 * The (de)serialization Jackson process can be customized using the <code>addMixIn(target, mixinSource)</code> method</li>
 * </ul>
 * 
 * @author Michael Remond
 * @since 1.1.0
 *
 */
public class DefaultJsonConverter implements JsonConverter {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final DefaultJsonConverter INSTANCE = new DefaultJsonConverter();

    public static final JsonConverter getInstance() {
        return INSTANCE;
    }

    public DefaultJsonConverter() {
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);

        addMixIn(BearerAccessToken.class, BearerAccessTokenMixin.class);
        addMixIn(Scope.Value.class, ValueMixin.class);
        addMixIn(Token.class, TokenMixin.class);
    }

    public void addMixIn(Class<?> target, Class<?> mixinSource) {
        mapper.addMixInAnnotations(target, mixinSource);
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
                return new JsonObject().put("class", value.getClass().getName()).put("value",
                        new JsonObject(encode(value)));
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
                return decode(src.getJsonObject("value").encode(), Class.forName(src.getString("class")));
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

    public static class BearerAccessTokenMixin {
        @JsonIgnore
        private AccessTokenType type;

        @JsonCreator
        public BearerAccessTokenMixin(@JsonProperty("value") String value, @JsonProperty("lifetime") long lifetime,
                @JsonProperty("scope") Scope scope) {
        };
    }

    public static class ValueMixin {
        @JsonCreator
        public ValueMixin(@JsonProperty("value") String value, @JsonProperty("requirement") Requirement requirement) {
        };
    }

    public static class TokenMixin {
        @JsonCreator
        public TokenMixin(@JsonProperty("token") String token, @JsonProperty("secret") String secret,
                @JsonProperty("rawResponse") String rawResponse) {
        };
    }

}
