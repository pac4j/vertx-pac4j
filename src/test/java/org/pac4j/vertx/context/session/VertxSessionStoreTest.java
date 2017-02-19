package org.pac4j.vertx.context.session;

import io.vertx.ext.web.Session;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.vertx.VertxWebContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

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
    private final SessionStore sessionStore = new VertxSessionStore();

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


}