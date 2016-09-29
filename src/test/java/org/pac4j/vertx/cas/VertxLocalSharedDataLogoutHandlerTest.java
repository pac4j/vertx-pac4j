package org.pac4j.vertx.cas;

import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxLocalSharedDataLogoutHandlerTest extends VertxSharedDataLogoutHandlerTestBase {

    @Test
    public void testRecordSession() throws Exception {
        final SessionStore sessionStore = LocalSessionStore.create(vertx);
        final String expectedSessionId = recordSession(new VertxLocalSharedDataLogoutHandler(vertx,
                sessionStore), sessionStore);
        // Validate that the shared data for TEST_TICKET points to the session id for the created session should now be in the shared data?
        LocalMap<String, String> casMap = vertx.sharedData().getLocalMap(VertxSharedDataLogoutHandler.PAC4J_CAS_SHARED_DATA_KEY);
        assertThat(casMap.get(TEST_TICKET), is(expectedSessionId));
        testComplete();
    }

    @Test
    public void testDestroySession() throws Exception {
        // Set up a session
        final SessionStore sessionStore = LocalSessionStore.create(vertx);
        final Session session = getSession(sessionStore);

        // Add key pac4j elements to session, as we'd expect to find, do this via the Dummy Web Context
        final VertxWebContext context = dummyWebContext(session);
        simulateLogin(context);

        final VertxLocalSharedDataLogoutHandler handler = new VertxLocalSharedDataLogoutHandler(vertx, sessionStore);
        // Now we've validated the state of the session before we attempt to destroy it
        handler.recordSession(context, TEST_TICKET);
        assertThat(vertx.sharedData().getLocalMap(VertxSharedDataLogoutHandler.PAC4J_CAS_SHARED_DATA_KEY).get(TEST_TICKET), is(session.id()));

        // Now we've validated the state before we destroy the session, it's time to call destroySession
        handler.destroySession(context);

        // Now there should be no profile and the shared data entry should be empty
        final String sessionIdFromSharedData = (String) vertx.sharedData().getLocalMap(VertxSharedDataLogoutHandler.PAC4J_CAS_SHARED_DATA_KEY)
                .get(TEST_TICKET);
        assertThat(sessionIdFromSharedData, is(nullValue()));
        final CommonProfile userProfileFromSession = new VertxProfileManager(context).get(true).orElse(null);
        assertThat(userProfileFromSession, is(nullValue()));
    }

}