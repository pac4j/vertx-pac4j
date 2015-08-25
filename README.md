Pac4j module for Vert.x [![Build Status](https://travis-ci.org/pac4j/vertx-pac4j.png?branch=master)](https://travis-ci.org/pac4j/vertx-pac4j)
=======================

**vertx-pac4j** is a Profile & Authentication Client, it's a general security library to authenticate users, get their profiles, manage their authorizations in order to secure Vert.x web applications.

### Vert.x version compatibility
For vert.x 2 and previous use vertx-pac4j 1.1.x
For vert.x 3 and subsequent use vertx-pac4j 2.0.x

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

vertx-pac4j consists of two maven modules:
* **[Pac4j Manager module for Vertx](#pac4j-manager-module)** to deploy as a busmod
* **[Pac4j Vertx Helper module](#pac4j-vertx-helper)** to import as a dependency in your application Verticles

<img src="http://www.pac4j.org/img/vertx-pac4j-diagram.png" />

# Pac4j Manager Module

This is the pac4j manager module that allows an easy integration to all pac4j supported authentication providers (OAuth, Facebook, Twitter, CAS, SAML...). 

In order to facilitate the communication between your application verticles and the module, some Java helper classes are provided (see [Pac4j Vertx Helper](#Pac4j Vertx Helper). Feel free to develop and submit helpers in other Vert.x supported languages.

A stateful authentication process requires an http session, we suggest you then to use the excellent session-manager from campudus.

## Dependencies

When using a stateful authentication mechanism (OAuth, CAS...), this busmod requires some session mechanism like the session-manager from campudus.

## Name

The module name is `org.pac4j~vertx-pac4j-module~1.1.0`.

## Configuration

This busmod takes the following configuration:

    {
        "address": <address>,
        "ebConverter": <event bus object converter class>,
        "clientsConfig": {
            "callbackUrl": <callbackUrl>,
            "clients": {
                <clients>
            }
        }
    }
    
For example:

    {
        "address": "vertx.pac4j-manager",
        "clientsConfig": {
            "callbackUrl": "http://localhost:8080/callback",
            "clients": {
                "facebook": {
                    "class": "org.pac4j.oauth.client.FacebookClient",
                    "props": {
                        "key": "145278422258960",
                        "secret": "be21409ba8f39b5dae2a7de525484da8"
                    }
                }
            }
        }
    }
    
Let's take a look at each field in turn:

* `address` The main address for the busmod. Optional field. Default value is `vertx.pac4j-manager`
* `ebConverter` The class of the event bus object converter required to exchange session attributes and user profile between the application verticles and the pac4j manager module (see [Serialization Methods](#Serialization Methods)). Optional field. Default value is the Java Serialization converter 
* `clientsConfig` contains two fields: the pac4j callback url and the clients list.
* `clients` each pair describes how to configure one pac4j client. A client has a `class` and a list of `props`. The props relies on the setter methods of the client. In the previous example, `key` indicates that the corresponding value will be set on the FacebookClient object with the method `setKey`.

## Operations

The following operations relies on a common object representing the context of the current http request: the `webcontext`. The web context must have the following structure:

    {
        "method": <http method>, (GET, POST...)
        "serverName": <server name>, (localhost)
        "serverPort": <server port>, (8080)
        "fullUrl": <full url>, (http://localhost:8080/index.html)
        "scheme": <scheme>, (http)
        "headers": <headers>, ( { "Host": "localhost:8080" } )
        "parameters": <parameters>, ( { "ids": ["foo", "bar"] } )
        "sessionAttributes": <session attributes> ( { "userId": "foobar" } )
    }

The `parameters` are multi-valued and must contain GET and POST urlencoded parameters if any.
The `sessionAttributes` is a list of the attributes stored in session; they must be serialized in some way (see [Serialization Methods](#Serialization Methods)).

### Redirect

Redirect to the given authentication provider.

To redirect, send a JSON message to the address given by the main address of the busmod + `.redirect`. For example if the main address is `test.pac4jManager`, you send the message to `test.pac4jManager.redirect`.

The JSON message should have the following structure:

    {
        "clientName": <clientName>, ("FacebookClient")
        "protected": <protected>, (true)
        "isAjax": <isAjax>, (false)
        "webContext": <webContext>
    }
    
If redirect is successful a reply will be returned:

    {
        "status": "ok",
        "code": <response code>, (302)
        "headers": <response headers>, ( { "Location": "http://foobar" } )
        "content": <response content>, ( "hello foobar" )
        "sessionAttributes": <all session attributes>
    }
    
This message has to be used to build the http response and update the session attributes.
    
### Redirect Urls

Build redirect urls for being authenticated by the required providers.

To login, send a JSON message to the address given by the main address of the busmod + `.redirectUrls`.

The JSON message should have the following structure:

    {
        "clients": <clients list>, ( ["FacebookClient", "TwitterClient"] )
        "webContext": <webContext>
    }
 
If getting the redirect urls is successful the following reply will be returned:

    {
        "status": "ok",
        <Client Name>: <redirect url>,
        "sessionAttributes": <all session attributes>
    }
    
### Authenticate

Authenticate the current request.

To authenticate, send a JSON message to the address given by the main address of the busmod + `.authenticate`.

The JSON message should have the following structure:

    {
        "webContext": <webContext>
    }   
 
If the authentication information in the web context are valid the following reply will be returned:

    {
        "status": "ok",
        "userProfile": <user profile>, ( { "id": "foobar"} )
        
        "code": <response code>, (302)
        "headers": <response headers>, ( { "Location": "http://foobar" } )
        "content": <response content>, ( "hello foobar" )
        "sessionAttributes": <all session attributes>
    } 
    
Where `userProfile` is a Json Object representing the authenticated user; it must be serialized in some way (see [Serialization Methods](#Serialization Methods)).

## Serialization Methods

The Vert.x architecture requires some objects to be (de)serialized between the front verticles serving the HTTP requests and the pac4j manager module. These objects are the session attributes and the User Profile.
By default, a serialization based on Jackson is used (`org.pac4j.vertx.DefaultEventBusObjectConverter`).
In order to pass in a custom converter, add the `ebConverter` parameter to the pac4j manager module:

    {
        "ebConverter": "org.pac4j.vertx.MyCustomObjectConverter"
    }

# Pac4j Vertx Helper

In order to facilitate the integration with the pac4j manager busmod, you can use the Pac4j Vertx Helper module.

## Dependencies

Import the following dependency in your Vertx project:

    <dependency>
        <groupId>org.pac4j</groupId>
        <artifactId>vertx-pac4j-helper</artifactId>
        <version>1.1.0-SNAPSHOT</version>
    </dependency>

## Main Classes

* **org.pac4j.vertx.Pac4jHelper** this class has methods to send messages to the busmod based on the `HttpServerRequest` object
* **org.pac4j.vertx.AuthHttpServerRequest** this class wraps a standard `HttpServerRequest` object and allows to attach the current User Profile. This is useful in the handlers behind the `RequiresAuthenticationHandler` in order to retrieve the User Profile
* **org.pac4j.vertx.handlers.HandlerHelper** this class allows to detect a form POST request and forward to the next handler when the data are available. It is recommended to add this handler at the root of your server if you target to use form based authentication or SAML
* **org.pac4j.vertx.handlers.RequiresAuthenticationHandler** this class encapsulates another handler. It forwards the request to the handler if the user is already authenticated or redirects the user to the authentication provider otherwise
* **org.pac4j.vertx.handlers.CallbackHandler** this class finishes the authentication process if stateful, by validating the authentication information (e.g. a form with username and password) and storing the user profile in session
* **org.pac4j.vertx.handlers.LogoutHandler** this class removes the user profile from the session

The last three classes inherit from the `org.pac4j.vertx.handlers.SessionAwareHandler` which uses a modified version of the session-manager helper from campudus.

# Integration example

## Stateful application
Start the required modules and verticles:

    public class Pac4jDemo extends Verticle {
    @Override
    public void start() {
        // deploy session manager
        container.deployModule("com.campudus~session-manager~2.0.1-final",
                container.config().getObject("sessionManager"));
        // deploy pac4j manager
        container.deployModule("org.pac4j~vertx-pac4j-module~1.1.0-SNAPSHOT", container.config()
                .getObject("pac4jManager"));
        // deploy main verticle
        container.deployVerticle("org.pac4j.vertx.DemoServerVerticle");
        
    }}
    

Define the application verticle:

    public class DemoServerVerticle extends Verticle {
    @Override
    public void start() {

        // build session and pac4j helpers
        SessionHelper sessionHelper = new SessionHelper(vertx);
        Pac4jHelper pac4jHelper = new Pac4jHelper(vertx);

        RouteMatcher rm = new RouteMatcher();
        // the route '/facebook/index.html' requires an authentication and facebook will be used
        rm.get("/facebook/index.html", new RequiresAuthenticationHandler("FacebookClient", new Handler(),
                pac4jHelper, sessionHelper));
        // register the callback handler
        Handler<HttpServerRequest> callback = new CallbackHandler(pac4jHelper, sessionHelper);
        rm.get("/callback", callback);
        rm.post("/callback", callback);
        // index page
        rm.get("/", new Handler());

        vertx.createHttpServer().requestHandler(HandlerHelper.addFormParsing(rm)).listen(8080, "localhost");
    }}

## Stateless application
Start the required modules and verticles:

    public class Pac4jDemo extends Verticle {
    @Override
    public void start() {
        
        // deploy pac4j manager
        container.deployModule("org.pac4j~vertx-pac4j-module~1.1.0-SNAPSHOT", container.config()
                .getObject("pac4jManager"));
        // deploy main verticle
        container.deployVerticle("org.pac4j.vertx.DemoRestServerVerticle");
        
    }}

Define the application verticle:

    public class DemoRestServerVerticle extends Verticle {

    @Override
    public void start() {

        Pac4jHelper pac4jHelper = new Pac4jHelper(vertx);

        RouteMatcher rm = new RouteMatcher();

        rm.get("/", new RequiresAuthenticationHandler("BasicAuthClient", new DemoHandlers.AuthenticatedJsonHandler(),
                pac4jHelper, true));

        vertx.createHttpServer().requestHandler(rm).listen(8080, "localhost");

    }

### Demo

A demo with Facebook, Twitter, CAS, form authentication and basic auth authentication providers is available with [vertx-pac4j-demo](https://github.com/pac4j/vertx-pac4j-demo).


## Versions

The current version **1.1.1-SNAPSHOT** is under development. It's available on the [Sonatype snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/org/pac4j) as a Maven dependency:

The latest release of the **vertx-pac4j** project is the **1.1.0** version:

    <dependency>
        <groupId>org.pac4j</groupId>
        <artifactId>vertx-pac4j-helper</artifactId>
        <version>1.1.0</version>
    </dependency>

See the [release notes](https://github.com/pac4j/vertx-pac4j/wiki/Release-notes).


## Contact

If you have any question, please use the following mailing lists:
- [pac4j users](https://groups.google.com/forum/?hl=en#!forum/pac4j-users)
- [pac4j developers](https://groups.google.com/forum/?hl=en#!forum/pac4j-dev)
