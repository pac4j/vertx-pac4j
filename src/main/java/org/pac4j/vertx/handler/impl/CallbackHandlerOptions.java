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

/**
 * Vert.x-style options class for a callback handler
 * @since 2.0.0
 */
public class CallbackHandlerOptions {

    private String defaultUrl;
    private Boolean multiProfile;
    private Boolean renewSession;

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public CallbackHandlerOptions setDefaultUrl(String defaultUrl) {
        this.defaultUrl = defaultUrl;
        return this;
    }

    public Boolean getMultiProfile() {
        return multiProfile;
    }

    public CallbackHandlerOptions setMultiProfile(Boolean multiProfile) {
        this.multiProfile = multiProfile;
        return this;
    }

    public Boolean getRenewSession() {
        return renewSession;
    }

    public CallbackHandlerOptions setRenewSession(Boolean renewSession) {
        this.renewSession = renewSession;
        return this;
    }
}
