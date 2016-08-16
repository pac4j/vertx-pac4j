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
