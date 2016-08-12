/*
  Copyright 2015 - 2015 pac4j organization

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.pac4j.vertx.client;

import org.pac4j.core.authorization.generator.AuthorizationGenerator;
import org.pac4j.vertx.profile.TestOAuth2Profile;

/**
 * @author Jeremy Prime
 * @since 2.0.0
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

