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
package org.pac4j.vertx.cas;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.sstore.SessionStore;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxLocalSharedDataLogoutHandler extends VertxSharedDataLogoutHandler {

    public VertxLocalSharedDataLogoutHandler(final Vertx vertx, final SessionStore sessionStore) {
        this(vertx, sessionStore, 1);
    }

    public VertxLocalSharedDataLogoutHandler(final Vertx vertx, final SessionStore sessionStore, final int blockingTimeoutSeconds) {
        super(vertx, sessionStore, blockingTimeoutSeconds);
    }

    @Override
    protected void doRecordSession(String sessionId, String ticket) {
        final LocalMap<String, String> localMap = vertx.sharedData().getLocalMap(PAC4J_CAS_SHARED_DATA_KEY);
        localMap.put(ticket, sessionId);
    }

    @Override
    protected void doDestroySession(String ticket) {
        final LocalMap<String, String> cache =vertx.sharedData().getLocalMap(PAC4J_CAS_SHARED_DATA_KEY);
        cache.remove(ticket);
    }

    @Override
    protected String getSessionId(String ticket) {
        return vertx.sharedData().<String, String>getLocalMap(PAC4J_CAS_SHARED_DATA_KEY).get(ticket);
    }
}
