package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.User;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Sessions {

    public final static String SESSION_ID_COOKIE_NAME = "sessionId";

    private static Sessions instance = null;
    private Sessions(){ }
    public static Sessions getInstance(){
        if(instance == null)
            instance = new Sessions();
        return instance;
    }
    
    
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Optional<Session> getSession(SessionId id) {

        var existing =  Optional.ofNullable(this.sessions.get(id.getSessionId()));
        existing.ifPresent(this::refresh);
        return existing;
    }

    private void refresh(Session session) {
        var updated= new Session(session);
        this.sessions.put(session.id(), updated);
    }

    public SessionId createSession(User user, EnumSet<AuthenticationMode> authenticationModes){
        var newSession = new Session(user::sub);
        this.sessions.put(newSession.id(), newSession);
        return newSession::id;
    }


}
