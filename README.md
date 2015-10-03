<p align="center">
  <img src="https://pac4j.github.io/pac4j/img/logo-vertx.png" width="50%" height="50%" />
</p>

Pac4j module for Vert.x [![Build Status](https://travis-ci.org/pac4j/vertx-pac4j.png?branch=master)](https://travis-ci.org/pac4j/vertx-pac4j)
=======================

**vertx-pac4j** is a Profile & Authentication Client, it's a general security library to authenticate users, get their profiles, manage their authorizations in order to secure Vert.x web applications.

### Vert.x version compatibility
For vert.x 2 and previous use vertx-pac4j 1.1.x - this codebase can be found at [1.1.x](https://github.com/pac4j/vertx-pac4j/tree/vertx-pac4j-1.1.x)

For vert.x 3 and subsequent use vertx-pac4j 2.0.x. Please note that vertx-pac4j 2.0.x is currently work in progress and
the codebase should be treated with caution at present. In addition, many parts of this README are yet to be updated for
vert.x 3 and vertx-pac4j 2.0.x. This will be updated over the coming days

### Supported authentication methods

Although **pac4j** historically targets external authentication protocols, it supports direct authentication methods as well. See the [authentication flows](https://github.com/pac4j/pac4j/wiki/Authentication-flows).

#### External/stateful authentication protocols

1. From the client application, save the requested url and redirect the user to the identity provider for authentication (HTTP 302)
2. After a successful authentication, redirect back the user from the identity provider to the client application (HTTP 302) and get the user credentials
3. With these credentials, get the profile of the authenticated user (direct call from the client application to the identity provider)
4. Redirect the user to the originally requested url and allow or disallow the access.

Supported protocols are:

1. OAuth (1.0 & 2.0)
2. CAS (1.0, 2.0, SAML, logout & proxy)
3. HTTP (form & basic auth authentications)
4. OpenID
5. SAML (2.0)
6. Google App Engine UserService
7. OpenID Connect 1.0

#### Stateless authentication protocols (REST operations)

The current HTTP request contains the required credentials to validate the user identity and retrieve his profile. It works from a basic authentication.

It relies on specific **Authenticator** to validate user credentials and **ProfileCreator** to create user profiles.

[Authentication flows](https://github.com/pac4j/pac4j/wiki/Authentication-flows)


## Providers supported

<table>
<tr><th>Provider</th><th>Protocol</th><th>Maven dependency</th><th>Client class</th><th>Profile class</th></tr>
<tr><td>CAS server</td><td>CAS</td><td>pac4j-cas</td><td>CasClient & CasProxyReceptor</td><td>CasProfile</td></tr>
<tr><td>CAS server using OAuth Wrapper</td><td>OAuth 2.0</td><td>pac4j-oauth</td><td>CasOAuthWrapperClient</td><td>CasOAuthWrapperProfile</td></tr>
<tr><td>DropBox</td><td>OAuth 1.0</td><td>pac4j-oauth</td><td>DropBoxClient</td><td>DropBoxProfile</td></tr>
<tr><td>Facebook</td><td>OAuth 2.0</td><td>pac4j-oauth</td><td>FacebookClient</td><td>FacebookProfile</td></tr>
<tr><td>GitHub</td><td>OAuth 2.0</td><td>pac4j-oauth</td><td>GitHubClient</td><td>GitHubProfile</td></tr>
<tr><td>Google</td><td>OAuth 2.0</td><td>pac4j-oauth</td><td>Google2Client</td><td>Google2Profile</td></tr>
<tr><td>LinkedIn</td><td>OAuth 1.0 & 2.0</td><td>pac4j-oauth</td><td>LinkedInClient & LinkedIn2Client</td><td>LinkedInProfile & LinkedIn2Profile</td></tr>
<tr><td>Twitter</td><td>OAuth 1.0</td><td>pac4j-oauth</td><td>TwitterClient</td><td>TwitterProfile</td></tr>
<tr><td>Windows Live</td><td>OAuth 2.0</td><td>pac4j-oauth</td><td>WindowsLiveClient</td><td>WindowsLiveProfile</td></tr>
<tr><td>WordPress</td><td>OAuth 2.0</td><td>pac4j-oauth</td><td>WordPressClient</td><td>WordPressProfile</td></tr>
<tr><td>Yahoo</td><td>OAuth 1.0</td><td>pac4j-oauth</td><td>YahooClient</td><td>YahooProfile</td></tr>
<tr><td>PayPal</td><td>OAuth 2.0</td><td>pac4j-oauth</td><td>PayPalClient</td><td>PayPalProfile</td></tr>
<tr><td>Vk</td><td>OAuth 2.0</td><td>pac4j-oauth</td><td>VkClient</td><td>VkProfile</td></tr>
<tr><td>Foursquare</td><td>OAuth 2.0</td><td>pac4j-oauth</td><td>FoursquareClient</td><td>FoursquareProfile</td></tr>
<tr><td>Bitbucket</td><td>OAuth 1.0</td><td>pac4j-oauth</td><td>BitbucketClient</td><td>BitbucketProfile</td></tr>
<tr><td>ORCiD</td><td>OAuth 2.0</td><td>pac4j-oauth</td><td>OrcidClient</td><td>OrcidProfile</td></tr>
<tr><td>Strava</td><td>OAuth 2.0</td><td>pac4j-oauth</td><td>StravaClient</td><td>StravaProfile</td></tr>
<tr><td>Web sites with basic auth authentication</td><td>HTTP</td><td>pac4j-http</td><td>BasicAuthClient</td><td>HttpProfile</td></tr>
<tr><td>Web sites with form authentication</td><td>HTTP</td><td>pac4j-http</td><td>FormClient</td><td>HttpProfile</td></tr>
<tr><td>Google - Deprecated</td><td>OpenID</td><td>pac4j-openid</td><td>GoogleOpenIdClient</td><td>GoogleOpenIdProfile</td></tr>
<tr><td>Yahoo</td><td>OpenID</td><td>pac4j-openid</td><td>YahooOpenIdClient</td><td>YahooOpenIdProfile</td></tr>
<tr><td>SAML Identity Provider</td><td>SAML 2.0</td><td>pac4j-saml</td><td>Saml2Client</td><td>Saml2Profile</td></tr>
<tr><td>Google App Engine User Service</td><td>Gae User Service Mechanism</td><td>pac4j-gae</td><td>GaeUserServiceClient</td><td>GaeUserServiceProfile</td></tr>
<tr><td>OpenID Connect Provider</td><td>OpenID Connect 1.0</td><td>pac4j-oidc</td><td>OidcClient</td><td>OidcProfile</td></tr>
</table>

# Technical description

vertx-pac4j consists of a single maven module vertx-pac4j which you should import as a maven dependency. It is designed specifically for use
in the vertx-web framework which sits atop vert.x

# Auth providers
# Auth handlers
# The stateful provider/handler combination requires a session. This can be achieved using a vertx-web SessionHandler. 


## Dependencies

When using stateful session handling, you need to use a vertx-web SessionHandler (and to enable this you need to use a Cookie handler, to enable
session cookies)

## Configuration

At present the file-based configuration option is deprecated in favour of type-safe code-based assembly of a Clients object.

### Redirect
    
### Redirect Urls

### Authenticate

## Serialization Methods

For distributed session management to work correctly, we serialize all Pac4j objects stored in the session into vertx JsonObjects. We now operate a
type-safe "SessionAttributes" class, which can hold a UserProfile and a set of custom attributes.

By default, a serialization based on Jackson is used (`org.pac4j.vertx.DefaultEventBusObjectConverter`).

## Dependencies

Import the following dependency in your Vertx project:

    <dependency>
        <groupId>org.pac4j</groupId>
        <artifactId>vertx-pac4j</artifactId>
        <version>2.0.0-SNAPSHOT</version>
    </dependency>

## Main Classes

* **org.pac4j.vertx.impl.Pac4jAuthProvider** implementation of the vert.x AuthProvider interface for Pac4j handling. It simply delegates everything to Pac4j
* **org.pac4j.vertx.handler.impl.RequiresAuthenticationHandler** this class implements the vert.x AuthHandler interface for Pac4j authentication.
* **org.pac4j.vertx.handler.impl.CallbackDeployingPac4jAuthHandler** this class extends RequiresAuthenticationHandler to auto-deploy a callback handler rather than writing additional code to do so
* **org.pac4j.vertx.flow.DirectClientAuthenticationFlow** abstracts the authentication flow for direct clients
* **org.pac4j.vertx.flow.IndirectClientAuthenticationFlow** abstracts the beginning of the authentication flow for indirect clients
* **org.pac4j.vertx.handler.impl.CallbackHandler** this class finishes the authentication process if stateful, by validating the authentication information (e.g. a form with username and password) and storing the user profile in session
* **org.pac4j.vertx.handler.impl.LogoutHandler** this class removes the user profile from the session

The last three classes inherit from the `org.pac4j.vertx.handler.impl.BasePac4jAuthHandler` which uses a modified version of the session-manager helper from campudus.

# Integration example

## Stateful application 

Please note that this is subject to change - the vertx-pac4j code is still evolving

Define the application verticle:

    public class DemoServerVerticle extends Verticle {
    @Override
    public void start() {
    
        Router router = Router.router(vertx);
        
        // Set up session handling in the vertx-web router
        // Note that if you want to use clustered session storage you need to use a ClusteredSessionStore
        // The default json encoder/decoder handles clustered session storage fine
        // It is necessary to use the same session store in the session handler and the auth provider
        SessionStore sessionStore = LocalSessionStore.create(vertx); 
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(sessionStore).setSessionCookieName("oAuth2Consumer.session"));
        
        // Construct pac4j Clients object and wrap in a Config
        Clients clients = ...; 
        Config config = ...;

        DefaultJsonConverter ebConverter = new DefaultJsonConverter();
        Pac4jAuthProvider authProvider = new Pac4jAuthProvider();
        Pac4jAuthHandlerOptions options = new Pac4jAuthHandlerOptions(TEST_CLIENT_NAME);
        // The next line could be separated into deployment of a separate RequiresAuthenticationHandler
        // and CallbackHandler if preferred. 
        CallbackDeployingPac4jAuthHandler authHandler = CallbackDeployingPac4jAuthHandler(vertx, config, router, authProvider, options);
        // Note that use of a stateful handler automatically configures the callback url
        router.route(HttpMethod.GET, "/facebook/index.html").handler(authHandler);
        // index page
        router.route(HttpMethod.GET, "/").handler(new Handler());
                
        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(8080, "localhost");
        
    }}

Please note that in the above code it is perfectly legitimate to use a RequiresAuthenticationHandler and deploy a CallbackHandler
separately. The CallbackDeployingPac4jAuthHandler is a convenience class which automatically deploys a CallbackHandler at the
callback url specified in the config supplied to it. It therefore saves a small amount of code when using a single callback
handler, but could prove useful for simple indirect authentication configurations. If you intend to use the same callback handler
for multiple paths with different RequiresAuthenticationHandler, it would make sense to explicitly deploy the CallbackHandler.

## Stateless application

Define the application verticle:

    public class DemoRestServerVerticle extends Verticle {

    @Override
    public void start() {

        Router router = Router.router(vertx);
        // Construct pac4j Clients object and wrap in a Config
        Clients clients = ...; 
        Config config = ...;

        final Pac4jAuthProvider authProvider = new Pac4jAuthProvider();
        final Pac4jAuthHandlerOptions options = new Pac4jAuthHandlerOptions("BasicAuthClient");
        final RequiresAuthenticationHandler handler =  new RequiresAuthenticationHandler(vertx, config), authProvider, options);
        router.route(HttpMethod.GET, "/").handler(handler);

        RouteMatcher rm = new RouteMatcher();

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(8080, "localhost");

    }

### Demo

A demo with Facebook, Twitter, CAS, form authentication and basic auth authentication providers will shortly be available with [vertx-pac4j-demo](https://github.com/pac4j/vertx-pac4j-demo).


## Versions

The current version **2.0.0-SNAPSHOT** is under development. It's available on the [Sonatype snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/org/pac4j) as a Maven dependency:

The latest release of the **vertx-pac4j** project is the **1.1.0** version:

    <dependency>
        <groupId>org.pac4j</groupId>
        <artifactId>vertx-pac4j</artifactId>
        <version>1.1.0</version>
    </dependency>

See the [release notes](https://github.com/pac4j/vertx-pac4j/wiki/Release-notes).


## Contact

If you have any question, please use the following mailing lists:
- [pac4j users](https://groups.google.com/forum/?hl=en#!forum/pac4j-users)
- [pac4j developers](https://groups.google.com/forum/?hl=en#!forum/pac4j-dev)
