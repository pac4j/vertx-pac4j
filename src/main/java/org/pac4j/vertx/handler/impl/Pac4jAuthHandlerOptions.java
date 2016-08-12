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

    private String clientName = "";
    private String authorizerName = "";
    private String matcherName = "";
    private boolean multiProfile = false;

    public Pac4jAuthHandlerOptions withClientName(final String newName) {
        Objects.requireNonNull(newName);
        clientName = newName;
        return this;
    }

    public Pac4jAuthHandlerOptions withAuthorizerName(final String newName) {
        Objects.requireNonNull(newName);
        authorizerName = newName;
        return this;
    }

    public Pac4jAuthHandlerOptions withMatcherName(final String matcherName) {
        Objects.requireNonNull(matcherName);
        this.matcherName = matcherName;
        return this;
    }

    public Pac4jAuthHandlerOptions withMultiProfile(final boolean allowMultiProfile) {
        this.multiProfile = allowMultiProfile;
        return this;
    }

    public String clientName() {
        return clientName;
    }

    public String authorizerName() {
        return authorizerName;
    }

    public String matcherName() {
        return matcherName;
    }

    public boolean multiProfile() {
        return multiProfile;
    }
}
