package com.campudus.vertx.sessionmanager.java;

import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.ServerCookieEncoder;

import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * This Java wrapper class helps using the session manager from Java. It provides useful functions
 * to handle storing and retrieving session data more easily.
 * 
 * @author <a href="http://www.campudus.com/">Joern Bernhardt</a>
 */
public class SessionHelper {

    private final EventBus eventBus;
    private final String smAddress;
    private final String cookieField;
    private final String cookiePath;

    /**
     * Creates a session helper for this Vertx instance.
     * 
     * @param vertx
     *            The Vertx instance to use this session helper. The Vertx is used for the
     *            communication over the event bus.
     */
    public SessionHelper(Vertx vertx) {
        this(vertx, "campudus.session", "sessionId", "/");
    }

    /**
     * Creates a session helper for this Vertx instance.
     * 
     * @param vertx
     *            The Vertx instance to use this session helper. The Vertx is used for the
     *            communication over the event bus.
     * @param sessionManagerAddress
     *            The address on which the session manager is listening on.
     */
    public SessionHelper(Vertx vertx, String sessionManagerAddress) {
        this(vertx, sessionManagerAddress, "sessionId", "/");
    }

    /**
     * Creates a session helper for this Vertx instance with a custom cookie field name.
     * 
     * @param vertx
     *            The Vertx instance to use this session helper. The Vertx is used for the
     *            communication over the event bus.
     * @param sessionManagerAddress
     *            The address on which the session manager is listening on.
     * @param cookieField
     *            The name of the generated field in the cookie for the client.
     */
    public SessionHelper(Vertx vertx, String sessionManagerAddress, String cookieField, String cookiePath) {
        this.smAddress = sessionManagerAddress;
        this.cookieField = cookieField;
        this.cookiePath = cookiePath;
        this.eventBus = vertx.eventBus();
    }

    /**
     * Gets a single data field from the session storage by using an HttpServerRequest.
     * 
     * @param req
     *            The http server request.
     * @param requiredField
     *            The wanted field from the session storage.
     * @param handler
     *            A handler for the received data.
     */
    public void withSessionData(final HttpServerRequest req, final String requiredField,
            final Handler<JsonObject> handler) {
        withSessionData(req, new JsonArray().addString(requiredField), handler);
    }

    /**
     * Gets multiple data fields from the session storage by using an HttpServerRequest.
     * 
     * @param req
     *            The http server request.
     * @param requiredFields
     *            The wanted fields from the session storage.
     * @param handler
     *            A handler A handler for the received data.
     */
    public void withSessionData(final HttpServerRequest req, final JsonArray requiredFields,
            final Handler<JsonObject> handler) {
        withSessionId(req, new Handler<String>() {
            @Override
            public void handle(String sessionId) {
                withSessionData(sessionId, requiredFields, handler);
            }
        });
    }

    /**
     * Gets a single data field from the session storage by using a session id.
     * 
     * @param sessionId
     *            The id of the session.
     * @param requiredField
     *            The wanted field from the session storage.
     * @param handler
     *            A handler for the received data.
     */
    public void withSessionData(String sessionId, String requiredField, final Handler<JsonObject> handler) {
        withSessionData(sessionId, new JsonArray().addString(requiredField), handler);
    }

    /**
     * Gets multiple data fields from the session storage by using a session id.
     * 
     * @param sessionId
     *            The id of the session.
     * @param requiredFields
     *            The wanted fields from the session storage.
     * @param handler
     *            A handler for the received data.
     */
    public void withSessionData(String sessionId, JsonArray requiredFields, final Handler<JsonObject> handler) {
        final JsonObject json = new JsonObject().putString("action", "get").putString("sessionId", sessionId)
                .putArray("fields", requiredFields);
        eventBus.send(smAddress, json, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(event.body());
            };
        });
    }

    /**
     * Puts multiple data fields into the session storage without need of the result.
     * 
     * @param req
     *            The http server request.
     * @param obj
     *            A JsonObject which provides key-value pairs.
     */
    public void putSessionData(HttpServerRequest req, JsonObject obj) {
        sendPutData(req, obj, null);
    }

    /**
     * Puts a JsonArray into the session storage without need of the result.
     * 
     * @param req
     *            The http server request.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The JsonArray to store.
     */
    public void putSessionData(HttpServerRequest req, String key, JsonArray obj) {
        sendPutData(req, new JsonObject().putArray(key, obj), null);
    }

    /**
     * Puts a byte array into the session storage without need of the result.
     * 
     * @param req
     *            The http server request.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The byte array to store.
     */
    public void putSessionData(HttpServerRequest req, String key, byte[] obj) {
        sendPutData(req, new JsonObject().putBinary(key, obj), null);
    }

    /**
     * Puts a boolean into the session storage without need of the result.
     * 
     * @param req
     *            The http server request.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The boolean to store.
     */
    public void putSessionData(HttpServerRequest req, String key, boolean obj) {
        sendPutData(req, new JsonObject().putBoolean(key, obj), null);
    }

    /**
     * Puts a number into the session storage without need of the result.
     * 
     * @param req
     *            The http server request.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The number to store.
     */
    public void putSessionData(HttpServerRequest req, String key, Number obj) {
        sendPutData(req, new JsonObject().putNumber(key, obj), null);
    }

    /**
     * Puts a JsonObject into the session storage without need of the result.
     * 
     * @param req
     *            The http server request.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The JsonObject to store.
     */
    public void putSessionData(HttpServerRequest req, String key, JsonObject obj) {
        sendPutData(req, new JsonObject().putObject(key, obj), null);
    }

    /**
     * Puts a String into the session storage without need of the result.
     * 
     * @param req
     *            The http server request.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The String to store.
     */
    public void putSessionData(HttpServerRequest req, String key, String obj) {
        sendPutData(req, new JsonObject().putString(key, obj), null);
    }

    /**
     * Puts multiple data fields into the session storage and retrieves the result of the storage
     * operation.
     * 
     * @param req
     *            The http server request.
     * @param obj
     *            A JsonObject which includes key-value pairs to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(HttpServerRequest req, JsonObject obj, Handler<JsonObject> doneHandler) {
        sendPutData(req, obj, doneHandler);
    }

    /**
     * Puts a JsonArray into the session storage and retrieves the result of the storage operation.
     * 
     * @param req
     *            The http server request.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The JsonArray to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(HttpServerRequest req, String key, JsonArray obj, Handler<JsonObject> doneHandler) {
        sendPutData(req, new JsonObject().putArray(key, obj), doneHandler);
    }

    /**
     * Puts a byte array into the session storage and retrieves the result of the storage operation.
     * 
     * @param req
     *            The http server request.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The byte array to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(HttpServerRequest req, String key, byte[] obj, Handler<JsonObject> doneHandler) {
        sendPutData(req, new JsonObject().putBinary(key, obj), doneHandler);
    }

    /**
     * Puts a boolean into the session storage and retrieves the result of the storage operation.
     * 
     * @param req
     *            The http server request.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The boolean to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(HttpServerRequest req, String key, boolean obj, Handler<JsonObject> doneHandler) {
        sendPutData(req, new JsonObject().putBoolean(key, obj), doneHandler);
    }

    /**
     * Puts a number into the session storage and retrieves the result of the storage operation.
     * 
     * @param req
     *            The http server request.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The number to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(HttpServerRequest req, String key, Number obj, Handler<JsonObject> doneHandler) {
        sendPutData(req, new JsonObject().putNumber(key, obj), doneHandler);
    }

    /**
     * Puts a JsonObject into the session storage and retrieves the result of the storage operation.
     * 
     * @param req
     *            The http server request.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The JsonObject to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(HttpServerRequest req, String key, JsonObject obj, Handler<JsonObject> doneHandler) {
        sendPutData(req, new JsonObject().putObject(key, obj), doneHandler);
    }

    /**
     * Puts a String into the session storage and retrieves the result of the storage operation.
     * 
     * @param req
     *            The http server request.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The String to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(HttpServerRequest req, String key, String obj, Handler<JsonObject> doneHandler) {
        sendPutData(req, new JsonObject().putString(key, obj), doneHandler);
    }

    /**
     * Puts multiple data fields into the session storage without need of the result.
     * 
     * @param sessionId
     *            The id of the session.
     * @param obj
     *            A JsonObject which provides key-value pairs.
     */
    public void putSessionData(String sessionId, JsonObject obj) {
        sendPutData(sessionId, obj, null);
    }

    /**
     * Puts a JsonArray into the session storage without need of the result.
     * 
     * @param sessionId
     *            The id of the session.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The JsonArray to store.
     */
    public void putSessionData(String sessionId, String key, JsonArray obj) {
        sendPutData(sessionId, new JsonObject().putArray(key, obj), null);
    }

    /**
     * Puts a byte array into the session storage without need of the result.
     * 
     * @param sessionId
     *            The id of the session.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The byte array to store.
     */
    public void putSessionData(String sessionId, String key, byte[] obj) {
        sendPutData(sessionId, new JsonObject().putBinary(key, obj), null);
    }

    /**
     * Puts a boolean into the session storage without need of the result.
     * 
     * @param sessionId
     *            The id of the session.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The JsonArray to store.
     */
    public void putSessionData(String sessionId, String key, boolean obj) {
        sendPutData(sessionId, new JsonObject().putBoolean(key, obj), null);
    }

    /**
     * Puts a number into the session storage without need of the result.
     * 
     * @param sessionId
     *            The id of the session.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The number to store.
     */
    public void putSessionData(String sessionId, String key, Number obj) {
        sendPutData(sessionId, new JsonObject().putNumber(key, obj), null);
    }

    /**
     * Puts a JsonObject into the session storage without need of the result.
     * 
     * @param sessionId
     *            The id of the session.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The JsonObject to store.
     */
    public void putSessionData(String sessionId, String key, JsonObject obj) {
        sendPutData(sessionId, new JsonObject().putObject(key, obj), null);
    }

    /**
     * Puts a String into the session storage without need of the result.
     * 
     * @param sessionId
     *            The id of the session.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The String to store.
     */
    public void putSessionData(String sessionId, String key, String obj) {
        sendPutData(sessionId, new JsonObject().putString(key, obj), null);
    }

    /**
     * Puts multiple data fields into the session storage without need of the result.
     * 
     * @param sessionId
     *            The id of the session.
     * @param obj
     *            A JsonObject which provides key-value pairs.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(String sessionId, JsonObject obj, Handler<JsonObject> doneHandler) {
        sendPutData(sessionId, obj, doneHandler);
    }

    /**
     * Puts a JsonArray into the session storage and retrieves the result of the storage operation.
     * 
     * @param sessionId
     *            The id of the session.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The JsonArray to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(String sessionId, String key, JsonArray obj, Handler<JsonObject> doneHandler) {
        sendPutData(sessionId, new JsonObject().putArray(key, obj), doneHandler);
    }

    /**
     * Puts a byte array into the session storage and retrieves the result of the storage operation.
     * 
     * @param sessionId
     *            The id of the session.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The byte array to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(String sessionId, String key, byte[] obj, Handler<JsonObject> doneHandler) {
        sendPutData(sessionId, new JsonObject().putBinary(key, obj), doneHandler);
    }

    /**
     * Puts a boolean into the session storage and retrieves the result of the storage operation.
     * 
     * @param sessionId
     *            The id of the session.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The JsonArray to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(String sessionId, String key, boolean obj, Handler<JsonObject> doneHandler) {
        sendPutData(sessionId, new JsonObject().putBoolean(key, obj), doneHandler);
    }

    /**
     * Puts a number into the session storage and retrieves the result of the storage operation.
     * 
     * @param sessionId
     *            The id of the session.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The number to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(String sessionId, String key, Number obj, Handler<JsonObject> doneHandler) {
        sendPutData(sessionId, new JsonObject().putNumber(key, obj), doneHandler);
    }

    /**
     * Puts a JsonObject into the session storage and retrieves the result of the storage operation.
     * 
     * @param sessionId
     *            The id of the session.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The JsonObject to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(String sessionId, String key, JsonObject obj, Handler<JsonObject> doneHandler) {
        sendPutData(sessionId, new JsonObject().putObject(key, obj), doneHandler);
    }

    /**
     * Puts a String into the session storage and retrieves the result of the storage operation.
     * 
     * @param sessionId
     *            The id of the session.
     * @param key
     *            The name of the field to put data into.
     * @param obj
     *            The String to store.
     * @param doneHandler
     *            The handler to call after storing finished. It will receive the message sent by
     *            the session manager.
     */
    public void putSessionData(String sessionId, String key, String obj, Handler<JsonObject> doneHandler) {
        sendPutData(sessionId, new JsonObject().putString(key, obj), doneHandler);
    }

    /**
     * Does something with a session id. It will use the session id provided by the client inside
     * the http server request. If it cannot find it, it will create a new session.
     * 
     * @param req
     *            The http server request.
     * @param handler
     *            The handler to call with the created or found session id.
     */
    public void withSessionId(final HttpServerRequest req, final Handler<String> handler) {
        String value = req.headers().get("Cookie");
        if (value != null) {
            Set<Cookie> cookies = CookieDecoder.decode(value);
            for (final Cookie cookie : cookies) {
                if (cookieField.equals(cookie.getName())) {
                    handler.handle(cookie.getValue());
                    return;
                }
            }
        }

        startSession(req, handler);
    }

    public String getSessionId(final HttpServerRequest req) {
        String value = req.headers().get("Cookie");
        if (value != null) {
            Set<Cookie> cookies = CookieDecoder.decode(value);
            for (final Cookie cookie : cookies) {
                if (cookieField.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Creates a new session for the specified request. It will provide the newly created session id
     * as a cookie.
     * 
     * @param req
     *            The http server request, i.e. client, to create a session for.
     * @param handlerWithSessionId
     *            A handler to use the created session id with.
     */
    public void startSession(final HttpServerRequest req, final Handler<String> handlerWithSessionId) {
        // Create a new session and use that
        eventBus.send(smAddress, new JsonObject().putString("action", "start"), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                String sessionId = event.body().getString("sessionId");
                Cookie cookie = new DefaultCookie(cookieField, sessionId);
                cookie.setPath(cookiePath);
                req.response().putHeader("Set-Cookie", ServerCookieEncoder.encode(cookie));
                if (handlerWithSessionId != null) {
                    handlerWithSessionId.handle(sessionId);
                }
            }
        });
    }

    /**
     * Destroys a session and gives the result to a handler.
     * 
     * @param sessionId
     *            The id of the session to destroy.
     * @param doneHandler
     *            The handler to call with the result of the session manager.
     */
    public void destroySession(String sessionId, final Handler<JsonObject> doneHandler) {
        eventBus.send(smAddress, new JsonObject().putString("action", "destroy").putString("sessionId", sessionId),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> arg0) {
                        doneHandler.handle(arg0.body());
                    }
                });
    }

    /**
     * Checks all sessions for a potential match of the specified JsonObject and returns the reply
     * of the session manager ({ matches : true/false, sessions: [...all matching sessions or empty
     * JsonArray...] }).
     * 
     * @param json
     *            The JsonObject to check against all sessions.
     * @param doneHandler
     *            Handles the result of the session manager.
     */
    public void checkAllSessionsForMatch(JsonObject json, final Handler<JsonObject> doneHandler) {
        eventBus.send(smAddress, new JsonObject().putString("action", "status").putString("report", "matches")
                .putObject("data", json), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> arg0) {
                doneHandler.handle(arg0.body());
            };
        });
    }

    /**
     * Gets informations about open sessions and delivers this info to the specified handler.
     * 
     * @param handler
     *            The handler to call after getting the results of the connections report.
     */
    public void withConnectionStats(final Handler<JsonObject> handler) {
        eventBus.send(smAddress, new JsonObject().putString("action", "status").putString("report", "connections"),
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> event) {
                        handler.handle(event.body());
                    }
                });
    }

    private void sendPutData(final HttpServerRequest req, final JsonObject obj, final Handler<JsonObject> doneHandler) {
        withSessionId(req, new Handler<String>() {
            @Override
            public void handle(String sessionId) {
                sendPutData(sessionId, obj, doneHandler);
            }
        });
    }

    private void sendPutData(String sessionId, JsonObject putObject, final Handler<JsonObject> doneHandler) {
        JsonObject json = new JsonObject().putString("action", "put").putString("sessionId", sessionId)
                .putObject("data", putObject);
        if (doneHandler != null) {
            eventBus.send(smAddress, json, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> msg) {
                    doneHandler.handle(msg.body());
                }
            });
        } else {
            eventBus.send(smAddress, json);
        }
    }

}
