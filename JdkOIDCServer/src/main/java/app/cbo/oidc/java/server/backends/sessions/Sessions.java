package app.cbo.oidc.java.server.backends.sessions;

import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic session store. Not a part of OIDC.
 */
@Injectable
public class Sessions implements SessionFinder, SessionSupplier {

    public final static String SESSION_ID_COOKIE_NAME = "sessionId";

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    @NotNull
    public Optional<Session> find(@NotNull SessionId id) {

        if (id == null || id.getSessionId() == null)
            return Optional.empty();

        var existing = Optional.ofNullable(this.sessions.get(id.getSessionId()));
        existing.ifPresent(this::refresh);
        return existing;
    }

    public void addAuthentications(@NotNull SessionId id, @NotNull EnumSet<AuthenticationMode> authenticationModes){
        if(id == null || id.getSessionId() == null){
            throw new NullPointerException("session id cannot be null");
        }

        Optional<Session> session = this.find(id);
        if(!Utils.isEmpty(authenticationModes) && session.isPresent()){
            session.get().authentications().addAll(authenticationModes);
        }
    }

    private void refresh(@NotNull Session session) {
        if(session == null)
            throw new NullPointerException("session cannot be null");
        var updated = Session.refreshed(session);
        this.sessions.put(session.id(), updated);
    }

    @Override
    @NotNull public SessionId createSession(@NotNull User user, @NotNull EnumSet<AuthenticationMode> authenticationModes){

        var newSession = new Session(user::sub, authenticationModes);
        this.sessions.put(newSession.id(), newSession);
        return SessionId.of(newSession.id());
    }


}
