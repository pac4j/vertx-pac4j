package org.pac4j.vertx;

import org.junit.Test;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.oidc.profile.OidcProfile;
import org.scribe.model.Token;

import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

public class EventBusObjectConverterTest {

    @Test
    public void testEncode() {

        DefaultEventBusObjectConverter conv = new DefaultEventBusObjectConverter();

        String s1 = "Hello";
        Integer i1 = 42;
        Boolean b1 = true;
        Object[] arr1 = { s1, i1, b1 };
        State state = new State();
        FacebookProfile fbp = new FacebookProfile();
        Token t1 = new Token("token", "secret");
        OidcProfile p1 = new OidcProfile(new BearerAccessToken("Bearer"));

        System.out.println(s1 + ": " + conv.encodeObject(s1));
        System.out.println(i1 + ": " + conv.encodeObject(i1));
        System.out.println(b1 + ": " + conv.encodeObject(b1));
        System.out.println(arr1 + ": " + conv.encodeObject(arr1));
        System.out.println(state + ": " + conv.encodeObject(state));
        System.out.println(fbp + ": " + conv.encodeObject(fbp));
        System.out.println(t1 + ": " + conv.encodeObject(t1));

        System.out.println(s1 + ": " + conv.decodeObject(conv.encodeObject(s1)));
        System.out.println(i1 + ": " + conv.decodeObject(conv.encodeObject(i1)));
        System.out.println(b1 + ": " + conv.decodeObject(conv.encodeObject(b1)));
        System.out.println(arr1 + ": " + conv.decodeObject(conv.encodeObject(arr1)));
        System.out.println(state + ": " + conv.decodeObject(conv.encodeObject(state)));
        System.out.println(fbp + ": " + conv.decodeObject(conv.encodeObject(fbp)));
        System.out.println(t1 + ": " + conv.decodeObject(conv.encodeObject(t1)));
        System.out.println(p1 + ": " + conv.decodeObject(conv.encodeObject(p1)));

    }

}
