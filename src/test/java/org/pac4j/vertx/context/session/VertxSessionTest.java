package org.pac4j.vertx.context.session;

import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.impl.SessionImpl;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 */
public class VertxSessionTest {

    private static final String TEST_KEY = "testKey";
    public static final String TEST_VALUE = "testValue";

    Session vertxSession;
    VertxSession pac4jSession;

    @Before
    public void setUp() {
        vertxSession = new SessionImpl();
        pac4jSession = new VertxSession(vertxSession);
    }

    @Test
    public void testDestroy() throws Exception {
        pac4jSession.destroy();
        assertThat(vertxSession.isDestroyed(), is(true));
    }

    @Test
    public void set() throws Exception {
        assertThat(vertxSession.get(TEST_KEY), is(nullValue()));
        pac4jSession.set(TEST_KEY, TEST_VALUE);
        assertThat(vertxSession.get(TEST_KEY), is(TEST_VALUE));
    }

}