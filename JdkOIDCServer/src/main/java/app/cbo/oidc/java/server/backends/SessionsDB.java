package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.User;

import java.util.Map;
import java.util.Optional;

public class SessionsDB {

    //TODO [20/03/2023] Cleanup/regular cleanup / session life time
    //TODO [20/03/2023] This should be a sessionsDB, thaht returns users+session data (acr, iat, masLifeTime, etc.)
    private static SessionsDB instance = null;
    private SessionsDB(){ }
    public static SessionsDB getInstance(){
        if(instance == null)
            instance = new SessionsDB();
        return instance;
    }
    
    
    private Map<String, Session> sessions;

    public Optional<Session> getSession(SessionId id) {

        var existing =  Optional.ofNullable(this.sessions.get(id.getSessionId()));
        existing.ifPresent(this::refresh);
        return existing;
    }

    private void refresh(Session session) {
        var updated= new Session(session);
        this.sessions.put(session.id(), updated);
    }

    public SessionId createSession(User user){
        var newSession = new Session(user);
        this.sessions.put(newSession.id(), newSession);
        return newSession::id;
    }


}
