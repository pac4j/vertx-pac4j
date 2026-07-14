package org.pac4j.vertx.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.buffer.Buffer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.profile.definition.CommonProfileDefinition;

public class Pac4jUserSerializableTest {

  @Test
  public void testEmptyObjectClusterSerializable() {
    final Pac4jUser user = new Pac4jUser();
    final Buffer buffer = Buffer.buffer();
    user.writeToBuffer(buffer);

    final Pac4jUser userFromBuffer = new Pac4jUser();
    userFromBuffer.readFromBuffer(0, buffer);

    assertEquals(user, userFromBuffer);
  }

  @Test
  public void tesClusterSerializable() {
    final UserProfile userProfile = new CommonProfile();
    userProfile.setId("id");
    userProfile.setClientName("clientName");
    userProfile.setLinkedId("linkedId");
    userProfile.setRemembered(true);
    userProfile.addAttribute(CommonProfileDefinition.EMAIL, "email@vertx.org");
    userProfile.addAttribute(CommonProfileDefinition.DISPLAY_NAME, "displayName");
    userProfile.addAttribute(CommonProfileDefinition.FAMILY_NAME, "familyName");
    userProfile.addAttribute(CommonProfileDefinition.FIRST_NAME, "firstName");
    userProfile.addAttribute(CommonProfileDefinition.GENDER, "gender");
    userProfile.addAttribute(CommonProfileDefinition.LOCALE, "locale");
    userProfile.addAttribute(CommonProfileDefinition.LOCATION, "location");
    userProfile.addAttribute(CommonProfileDefinition.PICTURE_URL, "pictureUrl");
    userProfile.addAttribute(CommonProfileDefinition.PROFILE_URL, "profileUrl");

    final Pac4jUser user = new Pac4jUser(List.of(userProfile));
    final Buffer buffer = Buffer.buffer();
    user.writeToBuffer(buffer);

    final Pac4jUser userFromBuffer = new Pac4jUser();
    userFromBuffer.readFromBuffer(0, buffer);

    assertEquals(user, userFromBuffer);
  }

}
