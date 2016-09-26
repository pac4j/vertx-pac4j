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
 * Class with fluent API to wrap options which can be supplied to ApplicationLogoutHandler. This approach is consistent
 * with existing vert.x configuration mechanisms for optional configuration.
 * @since 2.1.0
 */
public class ApplicationLogoutHandlerOptions {

    private String defaultUrl = null;
    private String logoutUrlPattern = null;

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public ApplicationLogoutHandlerOptions setDefaultUrl(String defaultUrl) {
        this.defaultUrl = defaultUrl;
        return this;
    }

    public String getLogoutUrlPattern() {
        return logoutUrlPattern;
    }

    public ApplicationLogoutHandlerOptions setLogoutUrlPattern(String logoutUrlPattern) {
        this.logoutUrlPattern = logoutUrlPattern;
        return this;
    }
}
