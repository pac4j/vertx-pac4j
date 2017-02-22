package org.pac4j.vertx.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.scribejava.core.model.Token;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.oauth.config.OAuthConfiguration;
import org.pac4j.oauth.profile.JsonHelper;
import org.pac4j.oauth.profile.definition.OAuth20ProfileDefinition;

/**
 *
 */
public class TestOAuth2ProfileDefinition extends OAuth20ProfileDefinition {
    @Override
    public String getProfileUrl(Token accessToken, OAuthConfiguration configuration) {
        return "http://localhost:9292/profile";
    }

    @Override
    public CommonProfile extractUserProfile(String body) throws HttpAction {
        final TestOAuth2Profile profile = new TestOAuth2Profile();
        JsonNode json = JsonHelper.getFirstNode(body);
        if (json != null) {
            profile.setId(JsonHelper.getElement(json, "id"));
        }
        return profile;
    }

}
