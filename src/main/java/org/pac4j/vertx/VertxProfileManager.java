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
package org.pac4j.vertx;

import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.auth.Pac4jUser;

import java.util.Optional;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxProfileManager<T extends UserProfile> extends ProfileManager<T> {

    private VertxWebContext vertxWebContext;

    public VertxProfileManager(final VertxWebContext context) {
        super(context);
        this.vertxWebContext = context;
    }

    @Override
    public T get(boolean readFromSession) {
        return Optional.ofNullable(vertxWebContext.getVertxUser())
                .map(user -> (T)user.pac4jUserProfile())
                .orElse(null);
    }

    @Override
    public void remove(boolean removeFromSession) {
        vertxWebContext.removeVertxUser();
    }

    @Override
    public void save(boolean saveInSession, T profile) {
        final Pac4jUser pac4jUser = new Pac4jUser(profile);
        vertxWebContext.setVertxUser(pac4jUser);
    }
}
