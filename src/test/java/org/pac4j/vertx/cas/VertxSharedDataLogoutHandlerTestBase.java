package org.pac4j.vertx.cas;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.test.core.VertxTestBase;
import org.pac4j.core.context.BaseResponseContext;
import org.pac4j.core.context.Cookie;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.vertx.profile.TestOAuth2Profile;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;

/**
 * @author Jeremy Prime
 * @since 2.0.0
 */
public class VertxSharedDataLogoutHandlerTestBase extends VertxTestBase {


    public static final String TEST_USER_ID = "testUserId";
    protected static final String TEST_TICKET = "testTicket"; // Think this is sufficient for our purposes

    protected void simulateLogin(WebContext context) {
        final ProfileManager profileManager = new ProfileManager(context);
        final TestOAuth2Profile userProfile = new TestOAuth2Profile();
        userProfile.setId(TEST_USER_ID);
        profileManager.save(true, userProfile);
        UserProfile profile = profileManager.get(true);
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

    protected String recordSession(final Vertx suppliedVertx, final VertxSharedDataLogoutHandler casLogoutHandler, final SessionStore sessionStore) throws Exception {
        final Session session = getSession(sessionStore);
        final VertxSharedDataLogoutHandler handler = casLogoutHandler;
        final WebContext context = new DummyWebContext(session);
        handler.recordSession(context, TEST_TICKET);
        return session.id(); // Return the actual session id for test validation
    }

    /**
     * Simple dummy context wrapper so we can just focus on the behaviours we care about for a web context, not
     * actually use a full vert.x webcontext or try and invent a full cas stack including server for an integration
     * test
     */
    public static class DummyWebContext extends BaseResponseContext {

        private final Session session;

        protected DummyWebContext(Session session) {
            this.session = session;
        }

        @Override
        public String getRequestParameter(String name) {
            if (name.equals("logoutRequest")) {
                return "<samlp:LogoutRequest\n" +
                        "    xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
                        "    xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
                        "    ID=\"[RANDOM ID]\"\n" +
                        "    Version=\"2.0\"\n" +
                        "    IssueInstant=\"[CURRENT DATE/TIME]\">\n" +
                        "    <saml:NameID>@NOT_USED@</saml:NameID>\n" +
                        "    <samlp:SessionIndex>testTicket</samlp:SessionIndex>\n" +
                        "</samlp:LogoutRequest>";
            }
            return null;
        }

        @Override
        public Map<String, String[]> getRequestParameters() {
            return null;
        }

        @Override
        public Object getRequestAttribute(String name) {
            return null;
        }

        @Override
        public void setRequestAttribute(String name, Object value) {

        }

        @Override
        public String getRequestHeader(String name) {
            return null;
        }

        @Override
        public void setSessionAttribute(String name, Object value) {
            session.put(name, value);
        }

        @Override
        public Object getSessionAttribute(String name) {
            return session.get(name);
        }

        @Override
        public Object getSessionIdentifier() {
            return session.id();
        }

        @Override
        public String getRequestMethod() {
            return null;
        }

        @Override
        public String getRemoteAddr() {
            return null;
        }

        @Override
        public String getServerName() {
            return null;
        }

        @Override
        public int getServerPort() {
            return 0;
        }

        @Override
        public String getScheme() {
            return null;
        }

        @Override
        public String getFullRequestURL() {
            return null;
        }

        @Override
        public Collection<Cookie> getRequestCookies() {
            return null;
        }
    }
}