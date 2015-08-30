package org.pac4j.vertx.client;

import org.pac4j.core.authorization.AuthorizationGenerator;
import org.pac4j.vertx.profile.TestOAuth2Profile;

/**
 * @author jez
 */
public class TestOAuth2AuthorizationGenerator implements AuthorizationGenerator<TestOAuth2Profile> {

  @Override
  public void generate(TestOAuth2Profile testOAuth2Profile) {
    switch (testOAuth2Profile.getId()) {
      case "testUser1":
        // Don't add any authorities
        break;

      case "testUser2":
        testOAuth2Profile.addPermission("permission1");
        break;

      case "testUser3":
        testOAuth2Profile.addRole("role1");
        break;

      case "testUser4":
        testOAuth2Profile.addPermission("permission2");
        break;

      default:

    }
  }

}

