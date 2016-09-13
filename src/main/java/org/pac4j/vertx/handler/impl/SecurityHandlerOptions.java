package org.pac4j.vertx.handler.impl;

import java.util.Objects;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class SecurityHandlerOptions {

    private String clients = "";
    private String authorizers = "";
    private String matchers = "";
    private boolean multiProfile = false;

    public SecurityHandlerOptions withClients(final String newClientNames) {
        Objects.requireNonNull(newClientNames);
        clients = newClientNames;
        return this;
    }

    public SecurityHandlerOptions withAuthorizers(final String newAuthorizerNames) {
        Objects.requireNonNull(newAuthorizerNames);
        authorizers = newAuthorizerNames;
        return this;
    }

    public SecurityHandlerOptions withMatchers(final String matcherNames) {
        Objects.requireNonNull(matcherNames);
        this.matchers = matcherNames;
        return this;
    }

    public SecurityHandlerOptions withMultiProfile(final boolean allowMultiProfile) {
        this.multiProfile = allowMultiProfile;
        return this;
    }

    public String clients() {
        return clients;
    }

    public String authorizers() {
        return authorizers;
    }

    public String matchers() {
        return matchers;
    }

    public boolean multiProfile() {
        return multiProfile;
    }
}
