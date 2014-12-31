package org.pac4j.vertx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.scribe.model.Token;
import org.vertx.java.core.json.JsonObject;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.Scope.Value.Requirement;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

public class DefaultEventBusObjectConverterTest {

    static DefaultEventBusObjectConverter conv = new DefaultEventBusObjectConverter();

    @Test
    public void testEncodeAndDecode() {

        String s1 = "Hello";
        Integer i1 = 42;
        Boolean b1 = true;
        Object[] arr1 = { s1, i1, b1 };
        State state = new State();
        FacebookProfile fbp = new FacebookProfile();
        Token t1 = new Token("token", "secret");
        Scope scope1 = new Scope();
        scope1.add(new Scope.Value("hello", Requirement.REQUIRED));
        scope1.add(new Scope.Value("foo"));
        BearerAccessToken bat = new BearerAccessToken("Bearer", 3600, scope1);

        assertEquals(s1, convert(s1));
        assertEquals(i1, convert(i1));
        assertEquals(b1, convert(b1));
        assertTrue(Arrays.equals(arr1, (Object[]) convert(arr1)));
        assertEquals(state, convert(state));
        // test to improve when UserProfile will implement equals
        assertEquals(fbp.toString(), convert(fbp).toString());
        assertEquals(t1, convert(t1));
        assertEquals(bat, convert(bat));

    }

    @Test
    public void testEncode() {
        Scope scope1 = new Scope();
        scope1.add(new Scope.Value("hello", Requirement.REQUIRED));
        scope1.add(new Scope.Value("foo"));
        BearerAccessToken bat = new BearerAccessToken("Bearer", 3600, scope1);
        JsonObject e = (JsonObject) conv.encodeObject(bat);

        JsonObject sbat = e.getObject("value");
        assertEquals(3600, sbat.getValue("lifetime"));
        assertEquals("Bearer", sbat.getValue("value"));
        assertEquals(2, sbat.getArray("scope").size());

    }

    private Object convert(Object o) {
        return conv.decodeObject(conv.encodeObject(o));
    }

}
