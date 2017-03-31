<p align="center">
  <img src="https://pac4j.github.io/pac4j/img/logo-vertx.png" width="300" />
</p>

The `vertx-pac4j` project is an **easy and powerful security library for Vert.x 3** web applications which supports authentication and authorization, but also application logout and advanced features like CSRF protection. It's available under the Apache 2 license and based on the **[pac4j security engine](https://github.com/pac4j/pac4j)**.

[**Main concepts and components:**](http://www.pac4j.org/docs/main-concepts-and-components.html)

1) A [**client**](http://www.pac4j.org/docs/clients.html) represents an authentication mechanism. It performs the login process and returns a user profile. An indirect client is for UI authentication while a direct client is for web services authentication:

&#9656; OAuth - SAML - CAS - OpenID Connect - HTTP - OpenID - Google App Engine - LDAP - SQL - JWT - MongoDB - Stormpath - IP address

2) An [**authorizer**](http://www.pac4j.org/docs/authorizers.html) is meant to check authorizations on the authenticated user profile(s) or on the current web context:

&#9656; Roles / permissions - Anonymous / remember-me / (fully) authenticated - Profile type, attribute -  CORS - CSRF - Security headers - IP address, HTTP method

3) The `SecurityHandler` protects an url by checking that the user is authenticated and that the authorizations are valid, according to the clients and authorizers configuration. If the user is not authenticated, it performs authentication for direct clients or starts the login process for indirect clients

4) The `CallbackHandler` finishes the login process for an indirect client

5) The `ApplicationLogoutHandler` logs out the user from the application.

For vert.x 2 and previous, use vertx-pac4j 1.1.x - this codebase can be found at [1.1.x](https://github.com/pac4j/vertx-pac4j/tree/vertx-pac4j-1.1.x)

## How to use it?

First, you need to add a dependency on this library as well as on the appropriate `pac4j` submodules. Then, you must define the [**clients**](http://www.pac4j.org/docs/clients.html) for authentication and the [**authorizers**](http://www.pac4j.org/docs/authorizers.html) to check authorizations.

Define the `CallbackHandler` to finish authentication processes if you use indirect clients (like Facebook). Supply a `CallbackHandlerOptions` to configure the handler.

Use the `SecurityHandler` to secure the urls of your web application (using the `clientName` parameter for authentication and the `authorizerName` parameter for authorizations). Supply a `SecurityHandlerOptions` to configure the handler.

Just follow these easy steps:

### 1) Add the required dependencies (`vertx-pac4j` + `pac4j-*` libraries)

You need to add a dependency on the `vertx-pac4j` library (<em>groupId</em>: **org.pac4j**, *version*: **2.1.0**) as well as on the appropriate `pac4j` submodules (<em>groupId</em>: **org.pac4j**, *version*: **1.9.4**): the `pac4j-oauth` dependency for OAuth support, the `pac4j-cas` dependency for CAS support, the `pac4j-ldap` module for LDAP authentication, ...

All released artifacts are available in the [Maven central repository](http://search.maven.org/#search%7Cga%7C1%7Cpac4j).

---

### 2) Define the configuration (`Config` + `Clients` + `XXXClient` + `Authorizer`)

Each authentication mechanism (Facebook, Twitter, a CAS server...) is defined by a client (implementing the `org.pac4j.core.client.Client` interface). All clients must be gathered in a `org.pac4j.core.client.Clients` class.

All the `Clients` and the authorizers must be gathered in a `Config` object (which can be itself build in a `org.pac4j.core.config.ConfigFactory`).  
For example:

    final OidcClient oidcClient = new OidcClient();
    oidcClient.setClientID("id");
    oidcClient.setSecret("secret");
    oidcClient.setDiscoveryURI("https://accounts.google.com/.well-known/openid-configuration");
    oidcClient.setUseNonce(true);
    oidcClient.addCustomParam("prompt", "consent");

    final SAML2ClientConfiguration cfg = new SAML2ClientConfiguration("resource:samlKeystore.jks", "pac4j-demo-passwd", "pac4j-demo-passwd", "resource:metadata-okta.xml");
    cfg.setMaximumAuthenticationLifetime(3600);
    cfg.setServiceProviderEntityId("http://localhost:8080/callback?client_name=SAML2Client");
    cfg.setServiceProviderMetadataPath("sp-metadata.xml");
    final SAML2Client saml2Client = new SAML2Client(cfg);

    final FacebookClient facebookClient = new FacebookClient("fbId", "fbSecret");
    final TwitterClient twitterClient = new TwitterClient("twId", "twSecret");
     
    final FormClient formClient = new FormClient("http://localhost:8080/theForm.jsp", new SimpleTestUsernamePasswordAuthenticator());
    final IndirectBasicAuthClient basicAuthClient = new IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());
     
    final CasClient casClient = new CasClient("http://mycasserver/login");
     
    final ParameterClient parameterClient = new ParameterClient("token", new JwtAuthenticator("salt"));
     
    Config config = new Config("http://localhost:8080/callback", oidcClient, saml2Client, facebookClient,
                                      twitterClient, formClient, basicAuthClient, casClient, parameterClient);
    config.addAuthorizer("admin", new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
    config.addAuthorizer("custom", new CustomAuthorizer());

"http://localhost:8080/callback" is the url of the callback endpoint (see below). It may not be defined for REST support / direct clients only.

Notice that you can define specific [matchers](http://www.pac4j.org/docs/matchers.html) via the `addMatcher(name, Matcher)` method.

---

### 3) Protect urls (`SecurityHandler`)

You can protect (authentication + authorizations) the urls of your J2E application by using the `SecurityHandler` and defining the appropriate mapping. It has the following behaviour:

1) If the HTTP request matches the `matchers` configuration (or no `matchers` are defined), the security is applied. Otherwise, the user is automatically granted access.

2) First, if the user is not authenticated (no profile) and if some clients have been defined in the `clients` parameter, a login is tried for the direct clients.

3) Then, if the user has a profile, authorizations are checked according to the `authorizers` configuration. If the authorizations are valid, the user is granted access. Otherwise, a 403 error page is displayed.

4) Finally, if the user is still not authenticated (no profile), he is redirected to the appropriate identity provider if the first defined client is an indirect one in the `clients` configuration. Otherwise, a 401 error page is displayed.


The following parameters are available (via a `SecurityHandlerOptions` instance you pass into your `SecurityHandler` constructoe):

1) `clients` (optional): the list of client names (separated by commas) used for authentication:
- in all cases, this filter requires the user to be authenticated. Thus, if the `clients` is blank or not defined, the user must have been previously authenticated
- if the `client_name` request parameter is provided, only this client (if it exists in the `clients`) is selected.

2) `authorizers` (optional): the list of authorizer names (separated by commas) used to check authorizations:
- if the `authorizers` is blank or not defined, no authorization is checked
- the following authorizers are available by default (without defining them in the configuration):
  * `isFullyAuthenticated` to check if the user is authenticated but not remembered, `isRemembered` for a remembered user, `isAnonymous` to ensure the user is not authenticated, `isAuthenticated` to ensure the user is authenticated (not necessary by default unless you use the `AnonymousClient`)
  * `hsts` to use the `StrictTransportSecurityHeader` authorizer, `nosniff` for `XContentTypeOptionsHeader`, `noframe` for `XFrameOptionsHeader `, `xssprotection` for `XSSProtectionHeader `, `nocache` for `CacheControlHeader ` or `securityHeaders` for the five previous authorizers
  * `csrfToken` to use the `CsrfTokenGeneratorAuthorizer` with the `DefaultCsrfTokenGenerator` (it generates a CSRF token and saves it as the `pac4jCsrfToken` request attribute and in the `pac4jCsrfToken` cookie), `csrfCheck` to check that this previous token has been sent as the `pac4jCsrfToken` header or parameter in a POST request and `csrf` to use both previous authorizers.

3) `matchers` (optional): the list of matcher names (separated by commas) that the request must satisfy to check authentication / authorizations

4) `multiProfile` (optional): it indicates whether multiple authentications (and thus multiple profiles) must be kept at the same time (`false` by default).

    Pac4jAuthProvider authProvider = new Pac4jAuthProvider();
    SecurityHandlerOptions options = new SecurityHandlerOptions().withClients(clientNames);
    if (authName != null) {
       options = options.withAuthorizers(authName);
    }
    router.get(url).handler(new RequiresAuthenticationHandler(vertx, config, authProvider, options));

---

### 4) Define the callback endpoint only for indirect clients (`CallbackFilter`)

For indirect clients (like Facebook), the user is redirected to an external identity provider for login and then back to the application.
Thus, a callback endpoint is required in the application. It is managed by the `CallbackHandler` which has the following behaviour:

1) the credentials are extracted from the current request to fetch the user profile (from the identity provider) which is then saved in the web session

2) finally, the user is redirected back to the originally requested url (or to the `defaultUrl`).

The following parameters are available (via the CallbackHandlerOptions class):

1) `defaultUrl` (optional): it's the default url after login if no url was originally requested (`/` by default)

2) `multiProfile` (optional): it indicates whether multiple authentications (and thus multiple profiles) must be kept at the same time (`false` by default)

3) `renewSession` (optional): it indicates whether the web session must be renewed after login, to avoid session hijacking (`true` by default). Currently vert.x does not provide a session renewal mechanism so this flag affects nothing, but it has been left in place for consistency.

    final CallbackHandlerOptions = new CallbackHandlerOptions().setDefaultUrl("/loginSuccess").setMultiProfile(false);
    final CallbackHandler callbackHandler = new CallbackHandler(vertx, config, options);
    router.get("/callback").handler(callbackHandler);
    router.post("/callback").handler(BodyHandler.create().setMergeFormAttributes(true));
    router.post("/callback").handler(callbackHandler);
    
---

### 5) Get the user profile (`VertxProfileManager`)

You can get the profile of the authenticated user using `VertxProfileManager.get(true)` (`false` not to use the session, but only the current HTTP request).
You can test if the user is authenticated using `VertxProfileManager.isAuthenticated()`.
You can get all the profiles of the authenticated user (if ever multiple ones are kept) using `VertxProfileManager.getAll(true)`.

Note that the above are all standard `ProfileManager` methods but the `VertxProfileManager` is an implementation which is integrated with vertx-web including session and user support.

    ProfileManager<CommonProfile> profileManager = new VertxProfileManager<>(new VertxWebContext(rc));
    Optional<CommonProfile> profile = profileManager.get(true);

The retrieved profile is at least a `CommonProfile`, from which you can retrieve the most common properties that all profiles share. But you can also cast the user profile to the appropriate profile according to the provider used for authentication. For example, after a Facebook authentication:
 
    FacebookProfile facebookProfile = (FacebookProfile) commonProfile;

---

### 6) Logout

You can log out the current authenticated user using the `ApplicationLogoutHandler`. It has the following behaviour (configured via an `ApplicationLogoutHandlerOptions` object):

1) after logout, the user is redirected to the url defined by the `url` request parameter if it matches the `logoutUrlPattern`

2) or the user is redirected to the `defaultUrl` if it is defined

3) otherwise, a blank page is displayed.

To perfom the logout, you must call the /logout url. A blank page is displayed by default unless an *url* request parameter is provided. In that case, the user will be redirected to this specified url (if it matches the logout url pattern defined) or to the default logout url otherwise.

The following parameters can be defined on the `ApplicationLogoutHandler`via an `ApplicationLogoutHandlerOptions` object:

- `defaultUrl` (optional): the default logout url if the provided *url* parameter does not match the `logoutUrlPattern` (by default: /)
- `logoutUrlPattern` (optional): the logout url pattern that the logout url must match (it's a security check, only relative urls are allowed by default).

Example: 

    final ApplicationLogoutHandlerOptions options = new ApplicationLogoutHandlerOptions();
    router.get("/logout").handler(new ApplicationLogoutHandler(vertx, options, config));
    
---

## Demo

The demo webapp: [vertx-pac4j-demo](https://github.com/pac4j/vertx-pac4j-demo) is available for tests and implement many authentication mechanisms: Facebook, Twitter, form, basic auth, CAS, SAML, OpenID Connect, Strava, JWT...


## Release notes

See the [release notes](https://github.com/pac4j/vertx-pac4j/wiki/Release-Notes). Learn more by browsing the [vertx-pac4j Javadoc](http://www.javadoc.io/doc/org.pac4j/vertx-pac4j/2.1.0) and the [pac4j Javadoc](http://www.pac4j.org/apidocs/pac4j/1.9.5/index.html).


## Need help?

If you have any question, please use the following mailing lists:

- [pac4j users](https://groups.google.com/forum/?hl=en#!forum/pac4j-users)
- [pac4j developers](https://groups.google.com/forum/?hl=en#!forum/pac4j-dev)

## Development

The version 3.0.0-SNAPSHOT is under development.

Maven artifacts are built via Travis: [![Build Status](https://travis-ci.org/pac4j/vertx-pac4j.png?branch=master)](https://travis-ci.org/pac4j/vertx-pac4j) and available in the [Sonatype snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/org/pac4j). This repository must be added in the Maven *pom.xml* file for example:

    <repositories>
      <repository>
        <id>sonatype-nexus-snapshots</id>
        <name>Sonatype Nexus Snapshots</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <releases>
          <enabled>false</enabled>
        </releases>
        <snapshots>
          <enabled>true</enabled>
        </snapshots>
      </repository>
    </repositories>
