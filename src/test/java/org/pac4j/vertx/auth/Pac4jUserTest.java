package org.pac4j.vertx.auth;

import io.vertx.core.buffer.Buffer;
import org.junit.Test;
import org.pac4j.vertx.profile.TestOAuth1Profile;
import org.pac4j.vertx.profile.TestOAuth2Profile;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jeremy Prime
 */
public class Pac4jUserTest {

    private static final String TEST_USER_ID_1 = "testUserId1";
    private static final String TEST_CLIENT_1 = "testClient1";
    private static final String TEST_SECRET_1 = "testSecret1";
    private static final String TEST_ACCESS_TOKEN_1 = "testAccessToken1";
    private static final String TEST_USER_ID_2 = "testUserId2";
    private static final String TEST_CLIENT_2 = "testClient2";
    private static final String TEST_ACCESS_TOKEN_2 = "testAccessToken2";

    @Test
    public void testClusterSerializationAndDeserializationSingleProfile() throws Exception {
        final TestOAuth1Profile profileToSerialize = getTestOAuth1Profile();
        final Pac4jUser userToSerialize = new Pac4jUser();
        userToSerialize.setUserProfile(TEST_CLIENT_1, profileToSerialize, false);
        final Buffer buf = Buffer.buffer();
        userToSerialize.writeToBuffer(buf);
        final Pac4jUser deserializedUser = new Pac4jUser();
        deserializedUser.readFromBuffer(0, buf);
        final TestOAuth1Profile profile = (TestOAuth1Profile) deserializedUser.pac4jUserProfiles().get(TEST_CLIENT_1);
        assertThat(profile, is(profileToSerialize));
        assertThat(deserializedUser.principal().encodePrettily(),
                is(userToSerialize.principal().encodePrettily()));
    }

    @Test
    public void testClusterSerializationAndDeserializationMultipleProfiles() throws Exception {
        final TestOAuth1Profile oAuth1Profile = getTestOAuth1Profile();
        final TestOAuth2Profile oAuth2Profile = getTestOAuth2Profile();
        final Pac4jUser userToSerialize = new Pac4jUser();
        userToSerialize.setUserProfile(TEST_CLIENT_1, oAuth1Profile, true);
        userToSerialize.setUserProfile(TEST_CLIENT_2, oAuth2Profile, true);

        final Buffer buf = Buffer.buffer();
        userToSerialize.writeToBuffer(buf);

        final Pac4jUser deserializedUser = new Pac4jUser();
        deserializedUser.readFromBuffer(0, buf);

        final TestOAuth1Profile deserializedOAuth1Profile =
                (TestOAuth1Profile) deserializedUser.pac4jUserProfiles().get(TEST_CLIENT_1);
        final TestOAuth2Profile deserializedOAuth2Profile =
                (TestOAuth2Profile) deserializedUser.pac4jUserProfiles().get(TEST_CLIENT_2);

        assertThat(deserializedOAuth1Profile, is(oAuth1Profile));
        assertThat(deserializedOAuth2Profile, is(oAuth2Profile));

    }

    private static TestOAuth1Profile getTestOAuth1Profile() {
        final TestOAuth1Profile profileToSerialize = new TestOAuth1Profile();
        profileToSerialize.setId(TEST_USER_ID_1);
        profileToSerialize.setClientName(TEST_CLIENT_1);
        profileToSerialize.setAccessSecret(TEST_SECRET_1);
        profileToSerialize.setAccessToken(TEST_ACCESS_TOKEN_1);
        return profileToSerialize;
    }

    private static TestOAuth2Profile getTestOAuth2Profile() {
        final TestOAuth2Profile profileToSerialize = new TestOAuth2Profile();
        profileToSerialize.setId(TEST_USER_ID_2);
        profileToSerialize.setClientName(TEST_CLIENT_2);
        profileToSerialize.setAccessToken(TEST_ACCESS_TOKEN_2);
        return profileToSerialize;
    }

}