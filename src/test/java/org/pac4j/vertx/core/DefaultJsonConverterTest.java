package org.pac4j.vertx.core;

import com.github.scribejava.core.model.OAuth1RequestToken;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Simple test for DefaultJsonConverter
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class DefaultJsonConverterTest {

    @Test
    public void testOAuth1RequestTokenDeserialization() throws Exception {
        final OAuth1RequestToken originalToken = new OAuth1RequestToken("testToken", "testSecret",
                false, "testRawResponse");

        final JsonObject encoded = (JsonObject) DefaultJsonConverter.getInstance().encodeObject(originalToken);

        final OAuth1RequestToken decodedToken = (OAuth1RequestToken) DefaultJsonConverter.getInstance().decodeObject(encoded);
        assertThat(decodedToken, is(originalToken));
    }

}