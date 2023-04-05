package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.User;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.Utils;

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

    @NotNull public Optional<Session> getSession(@NotNull SessionId id) {

        if(id == null || id.getSessionId() == null)
            return Optional.empty();

        var existing =  Optional.ofNullable(this.sessions.get(id.getSessionId()));
        existing.ifPresent(this::refresh);
        return existing;
    }

    public void addAuthentications(@NotNull SessionId id, @NotNull EnumSet<AuthenticationMode> authenticationModes){
        if(id == null || id.getSessionId() == null){
            throw new NullPointerException("session id cannot be null");
        }

        Optional<Session> session = this.getSession(id);
        if(!Utils.isEmpty(authenticationModes) && session.isPresent()){
            session.get().authentications().addAll(authenticationModes);
        }
    }

    private void refresh(@NotNull Session session) {
        if(session == null)
            throw new NullPointerException("session cannot be null");
        var updated= new Session(session);
        this.sessions.put(session.id(), updated);
    }

    @NotNull public SessionId createSession(@NotNull User user, @NotNull EnumSet<AuthenticationMode> authenticationModes){

        //TODO [05/04/2023] ACRs
        var newSession = new Session(user::sub);
        this.sessions.put(newSession.id(), newSession);
        return newSession::id;
    }


}
