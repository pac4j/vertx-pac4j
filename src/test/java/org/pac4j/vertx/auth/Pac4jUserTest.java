package org.pac4j.vertx.auth;

import io.vertx.core.buffer.Buffer;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.profile.TestOAuth1Profile;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jeremy Prime
 */
public class Pac4jUserTest {

    private static final String TEST_USER_ID = "testUserId";
    private static final String TEST_CLIENT = "testClient";

    @Test
    public void testClusterSerializationAndDeserializationSingleProfile() throws Exception {
        final CommonProfile profileToSerialize = new TestOAuth1Profile();
        profileToSerialize.setId(TEST_USER_ID);
        profileToSerialize.setClientName(TEST_CLIENT);
        final Pac4jUser userToSerialize = new Pac4jUser();
        userToSerialize.setUserProfile(TEST_CLIENT, profileToSerialize, false);
        final Buffer buf = Buffer.buffer();
        userToSerialize.writeToBuffer(buf);
        final Pac4jUser deserializedUser = new Pac4jUser();
        deserializedUser.readFromBuffer(0, buf);
        final CommonProfile profile = deserializedUser.pac4jUserProfiles().get(TEST_CLIENT);
        assertThat(profile.getId(), is(TEST_USER_ID));
        assertThat(deserializedUser.principal().encodePrettily(),
                is(userToSerialize.principal().encodePrettily()));
    }
}