package org.pac4j.vertx.cas;

import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.test.core.VertxTestBase;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.vertx.VertxProfileManager;
import org.pac4j.vertx.VertxWebContext;
import org.pac4j.vertx.auth.Pac4jUser;
import org.pac4j.vertx.profile.TestOAuth2Profile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.*;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxSharedDataLogoutHandlerTestBase extends VertxTestBase {


    public static final String TEST_USER_ID = "testUserId";
    protected static final String TEST_TICKET = "testTicket"; // Think this is sufficient for our purposes
    private static final String SESSION_USER_HOLDER_KEY = "__vertx.userHolder"; // Where vert.x stores the current vert.x user - we need to be able to clear this out


    protected void simulateLogin(VertxWebContext context) {
        final ProfileManager<CommonProfile> profileManager = new VertxProfileManager(context);
        final TestOAuth2Profile userProfile = new TestOAuth2Profile();
        userProfile.setId(TEST_USER_ID);
        profileManager.save(true, userProfile, false);
        CommonProfile profile = profileManager.get(true).orElse(null);
        assertThat(profile, is(userProfile));
    }

    protected Session getSession(final SessionStore sessionStore) throws Exception {
        final CompletableFuture<Session> sessionFuture = new CompletableFuture<>();
        final Session session =  sessionStore.createSession(10000);
        sessionStore.put(session, res -> {
            if (res.succeeded() && res.result()) {
                sessionFuture.complete(session);
            }
        });

        return sessionFuture.get(1, TimeUnit.SECONDS);
    }

    protected String recordSession(final VertxSharedDataLogoutHandler casLogoutHandler, final SessionStore sessionStore) throws Exception {
        final Session session = getSession(sessionStore);
        final VertxSharedDataLogoutHandler handler = casLogoutHandler;
        final VertxWebContext context = dummyWebContext(session);
        handler.recordSession(context, TEST_TICKET);
        return session.id(); // Return the actual session id for test validation
    }

    /*
    Factory method to stub a vertx web context, so we can focus on the behaviours we care about from the point of view of
    this test. I would prefer not to use a mock for this, but stubbing a full vertx web context just to extract a small
    portion of the behaviour looks even more painful
     */
    protected static VertxWebContext dummyWebContext(final Session session) {
        final VertxWebContext vertxWebContext = mock(VertxWebContext.class);

        when(vertxWebContext.getRequestParameter("logoutRequest")).thenReturn("<samlp:LogoutRequest\n" +
                "    xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
                "    xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
                "    ID=\"[RANDOM ID]\"\n" +
                "    Version=\"2.0\"\n" +
                "    IssueInstant=\"[CURRENT DATE/TIME]\">\n" +
                "    <saml:NameID>@NOT_USED@</saml:NameID>\n" +
                "    <samlp:SessionIndex>testTicket</samlp:SessionIndex>\n" +
                "</samlp:LogoutRequest>");

        when(vertxWebContext.getSessionAttribute(anyString())).thenAnswer(invocation -> {
            final String key = (String) invocation.getArguments()[0];
            return session.get(key);
        });

        doAnswer(invocation -> {
            final String key = (String) invocation.getArguments()[0];
            final Object value = invocation.getArguments()[1];
            session.put(key, value);
            return null;
        }).when(vertxWebContext).setSessionAttribute(anyString(), anyObject());

        when(vertxWebContext.getSessionIdentifier()).thenReturn(session.id());

        // annoyingly we have to replicate vert.x internals to check cas will work ok
        when(vertxWebContext.getVertxUser()).thenAnswer(invocation -> session.get(SESSION_USER_HOLDER_KEY));

        doAnswer(invocation -> {
            final Pac4jUser pac4jUser = (Pac4jUser) invocation.getArguments()[0];
            // annoyingly we have to replicate vert.x internals to check cas will work ok
            session.put(SESSION_USER_HOLDER_KEY, pac4jUser);
            return null;
        }).when(vertxWebContext).setVertxUser(anyObject());

        return vertxWebContext;
    }

}