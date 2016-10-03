package org.pac4j.vertx.profile;

import org.pac4j.oauth.profile.OAuth10Profile;

import static org.pac4j.vertx.TestUtils.isEqual;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class TestOAuth1Profile extends OAuth10Profile {

    private static final long serialVersionUID = 1347249873352825529L;

    private Integer hashCode = null;

    /**
     * Convenient override of equals() for test assertions
     * @param other, Object - expected to be a TestOAuth1Profile
     * @return true if equals contract is met, otherwise false
     *
     * For the purposes of the test code we consider two instances of this class equal if they have the same
     * access secret, access token, id and client name.
     */
    @Override
    public boolean equals(final Object other) {

        if (other == null || !(other instanceof TestOAuth1Profile)) {
            return false;
        }

        final TestOAuth1Profile that = (TestOAuth1Profile) other;

        return isEqual(this.getAccessToken(), that.getAccessToken()) &&
                isEqual(this.getId(), that.getId()) &&
                isEqual(this.getAccessSecret(), that.getAccessSecret()) &&
                isEqual(this.getClientName(), that.getClientName());
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = new StringBuilder().append(this.getAccessSecret())
                    .append(this.getAccessToken())
                    .append(this.getId())
                    .append(this.getClientName())
                    .toString()
                    .hashCode();
        }
        return hashCode;
    }

}
