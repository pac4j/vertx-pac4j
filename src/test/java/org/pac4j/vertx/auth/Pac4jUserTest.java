package org.pac4j.vertx.auth;

import io.vertx.core.buffer.Buffer;
import org.junit.Test;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.profile.TestOAuth1Profile;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jeremy Prime
 */
public class Pac4jUserTest {

    private static final String TEST_USER_ID = "testUserId";

    @Test
    public void testClusterSerializationAndDeserialization() throws Exception {
        final UserProfile profileToSerialize = new TestOAuth1Profile();
        profileToSerialize.setId(TEST_USER_ID);
        final Pac4jUser userToSerialize = new Pac4jUser(profileToSerialize);
        final Buffer buf = Buffer.buffer();
        userToSerialize.writeToBuffer(buf);
        final Pac4jUser deserializedUser = new Pac4jUser();
        deserializedUser.readFromBuffer(0, buf);
        assertThat(deserializedUser.pac4jUserProfile().getId(), is(TEST_USER_ID));
        assertThat(deserializedUser.principal().encodePrettily(),
                is(userToSerialize.principal().encodePrettily()));
    }
}