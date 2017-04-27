package org.pac4j.vertx.context.session;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.context.session.ExtendedSessionStore;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.vertx.VertxWebContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.pac4j.vertx.ConstantsKt.TEST_SESSION_KEY;
import static org.pac4j.vertx.ConstantsKt.TEST_SESSION_VALUE;

/**
 *
 */
public class VertxSessionStoreTest {

    private static final String SESSION_ID = UUID.randomUUID().toString();
    private static final String ABSENT = "absent";
    private static final String PRESENT = "present";
    private static final String PRESENT_VALUE = "presentValue";

    private Map<String, Object> sessionContents;
    private VertxWebContext webContext;
    private Session vertxSession;
    private final SessionStore sessionStore = new VertxSessionStore(null);

    @Before
    public void setUp() {

        sessionContents = new HashMap<>();
        webContext = mock(VertxWebContext.class);
        vertxSession = mock(Session.class);
        when(webContext.getVertxSession()).thenReturn(vertxSession);
        when(vertxSession.get(anyString())).thenAnswer(invocation -> {
            final String key = invocation.getArgumentAt(0, String.class);
            return sessionContents.get(key);
        });
        when(vertxSession.id()).thenReturn(SESSION_ID);

        doAnswer(invocation -> {
            final String key = invocation.getArgumentAt(0, String.class);
            final Object value = invocation.getArgumentAt(1, Object.class);
            sessionContents.put(key, value);
            return vertxSession;
        }).when(vertxSession).put(anyString(), anyObject());

    }


    @Test
    public void testGetOrCreateSessionId() throws Exception {
        assertThat(sessionStore.getOrCreateSessionId(webContext), is(SESSION_ID));
    }

    @Test
    public void testGetWhenValuePresent() throws Exception {
        // Let's put something into the session
        sessionContents.put(PRESENT, PRESENT_VALUE);
        assertThat(sessionStore.get(webContext, PRESENT), is(PRESENT_VALUE));
    }

    @Test
    public void testGetWhenValueAbsent() throws Exception {
        assertThat(sessionStore.get(webContext, ABSENT), is(nullValue()));
    }

    @Test
    public void testSet() throws Exception {
        assertThat(sessionContents.get(PRESENT), is(nullValue()));
        sessionStore.set(webContext, PRESENT, PRESENT_VALUE);
        assertThat(sessionContents.get(PRESENT), is(PRESENT_VALUE));
    }

    @Test
    public void testGetSessionById() throws Exception {

        final Vertx vertx = Vertx.vertx();

        // We need to create a pre-programmed vertx session so we can validate
        final LocalSessionStore vertxSessionStore = LocalSessionStore.create(vertx);
        final io.vertx.ext.web.Session vertxSession = vertxSessionStore.createSession(300000);
        final CountDownLatch latch = new CountDownLatch(1);
        vertxSessionStore.put(vertxSession, b -> latch.countDown());
        latch.await(1, TimeUnit.SECONDS);
        vertxSession.put(TEST_SESSION_KEY, TEST_SESSION_VALUE);
        final String sessionId = vertxSession.id();

        // Now let's wrap in a pac4j sessionStore
        final ExtendedSessionStore<VertxWebContext> pac4jSessionStore = new VertxSessionStore(vertxSessionStore);
        final org.pac4j.vertx.context.session.Session pac4jSession = pac4jSessionStore.getSession(sessionId);
        assertThat(pac4jSession, is(notNullValue()));
        assertThat(pac4jSession.get(TEST_SESSION_KEY), is(TEST_SESSION_VALUE));
    }


}