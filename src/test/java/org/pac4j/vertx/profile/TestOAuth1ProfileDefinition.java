package org.pac4j.vertx.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.scribejava.core.model.OAuth1Token;
import org.pac4j.oauth.config.OAuth10Configuration;
import org.pac4j.oauth.profile.JsonHelper;
import org.pac4j.oauth.profile.definition.OAuth10ProfileDefinition;

/**
 *
 */
public class TestOAuth1ProfileDefinition extends OAuth10ProfileDefinition<TestOAuth1Profile> {
    @Override
    public String getProfileUrl(OAuth1Token accessToken, OAuth10Configuration configuration) {
        return null;
    }

    @Override
    public TestOAuth1Profile extractUserProfile(String body) {
        final TestOAuth1Profile profile = new TestOAuth1Profile();
        JsonNode json = JsonHelper.getFirstNode(body);
        if (json != null) {
            profile.setId(JsonHelper.getElement(json, "id"));
        }
        return profile;
    }
}
