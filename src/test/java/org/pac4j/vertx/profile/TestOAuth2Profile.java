package org.pac4j.vertx.profile;

import org.pac4j.oauth.profile.OAuth20Profile;

import static org.pac4j.vertx.TestUtils.isEqual;

/**
 * @since 2.0.0
 */
public class TestOAuth2Profile extends OAuth20Profile{

    private static final long serialVersionUID = 1347249873352825529L;

    private Integer hashCode = null;

    /**
     * Trivial override of equals to make test assertions easier. For our tests we only care about equality in the
     * following fields:-
     * id
     * accessToken
     * clientName
     * @param other - the object against which we are checking equality
     * @return boolean, true if we consider the other object equal to this one, false otherwise
     */
    @Override
    public boolean equals(final Object other) {

        if (! (other instanceof  TestOAuth2Profile)) {
            return false;
        }

        final TestOAuth2Profile that = (TestOAuth2Profile) other;

        return isEqual(this.getId(), that.getId()) &&
                (isEqual(this.getAccessToken(), that.getAccessToken()) &&
                (isEqual(this.getClientName(), that.getClientName())));
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = new StringBuilder()
                    .append(this.getAccessToken())
                    .append(this.getId())
                    .append(this.getClientName())
                    .toString()
                    .hashCode();
        }
        return hashCode;
    }

}
