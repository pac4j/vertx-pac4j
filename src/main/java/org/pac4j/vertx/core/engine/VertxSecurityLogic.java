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
package org.pac4j.vertx.core.engine;

import org.pac4j.core.engine.DefaultSecurityLogic;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxSecurityLogic extends DefaultSecurityLogic<Void, VertxWebContext> {

    @Override
    protected ProfileManager getProfileManager(VertxWebContext context) {
        return new VertxProfileManager(context);
    }
}
