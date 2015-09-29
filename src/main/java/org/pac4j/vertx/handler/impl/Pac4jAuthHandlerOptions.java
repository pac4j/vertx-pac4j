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
package org.pac4j.vertx.handler.impl;

import java.util.Objects;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class Pac4jAuthHandlerOptions {

    private final String clientName;
    private final String authorizerName;

    public Pac4jAuthHandlerOptions(final String clientName, final String authorizerName) {
        Objects.requireNonNull(clientName);
        Objects.requireNonNull(authorizerName);
        this.clientName = clientName;
        this.authorizerName = authorizerName;
    }

    public String clientName() {
        return clientName;
    }

    public String authorizerName() {
        return authorizerName;
    }
}
