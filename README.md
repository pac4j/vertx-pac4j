vertx-pac4j
===========

vertx-pac4j consists of two maven modules:
* **[Pac4j Manager module for Vertx](#pac4j-manager-module)** to deploy as a busmod
* **[Pac4j Vertx Helper module](#pac4j-vertx-helper)** to import as a dependency in your application Verticles

# Pac4j Manager Module

This is the pac4j manager module that allows an easy integration to all pac4j supported authentication providers (OAuth, Facebook, Twitter, CAS, SAML...). 

In order to facilitate the communication between your application verticles and the module, some Java helper classes are provided (see [Pac4j Vertx Helper](#Pac4j Vertx Helper). Feel free to develop and submit helpers in other Vert.x supported languages.

Since the authentication process requires an http session, we suggest you to use the excellent session-manager from campudus.

You can look at a complete integration example in the [pac4j vertx demo app](https://github.com/pac4j/vertx-pac4j-demo/).

## Dependencies

This busmod requires some session mechanism like the session-manager from campudus.

## Name

The module name is `org.pac4j~vertx-pac4j-module~1.0-SNAPSHOT`.

## Configuration

This busmod takes the following configuration:

    {
        "address": <address>,
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

The parameters are multi-valued and must contain GET and POST urlencoded parameters if any.

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
    
Where `userProfile` is a Json Object representing the authenticated user. It is the application responsibility to store the user profile in session in order not to ask the user for authentication for the next requests.
    
# Pac4j Vertx Helper

In order to facilitate the integration with the pac4j manager busmod, you can use the Pac4j Vertx Helper module.

## Dependencies

Import the following dependency in your Vertx project:

    <dependency>
        <groupId>org.pac4j</groupId>
        <artifactId>vertx-pac4j-helper</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>

## Main Classes

* **org.pac4j.vertx.Pac4jHelper** this class has methods to send messages to the busmod based on the `HttpServerRequest` object.
* **org.pac4j.vertx.handlers.RequiresAuthenticationHandler** this class encapsulates another handler. It fowards the request to the handler if the user is already authenticated or redirects the user to the authentication provider otherwise.
* **org.pac4j.vertx.handlers.CallbackHandler** this class finishes the authentication process by validating the authentication information (e.g. a form with username and password) to the busmod and stores the user profile in session
* **org.pac4j.vertx.handlers.LogoutHandler** this class removes the user profile from the session

All these classes inherit from the `org.pac4j.vertx.handlers.SessionAwareHandler` which uses a modified version of the session-manager helper from campudus.

# Integration example

Define an application verticle:

    public class Pac4jDemoVerticle extends Verticle {
    @Override
    public void start() {
        // deploy session manager
        container.deployModule("com.campudus~session-manager~2.0.1-final",
                container.config().getObject("sessionManager"));
        // deploy pac4j manager
        container.deployModule("org.pac4j~vertx-pac4j-module~1.0-SNAPSHOT", container.config()
                .getObject("pac4jManager"));

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

        vertx.createHttpServer().requestHandler(rm).listen(8080, "localhost");
    }}

