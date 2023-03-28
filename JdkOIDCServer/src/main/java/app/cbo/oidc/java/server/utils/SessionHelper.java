package app.cbo.oidc.java.server.utils;

import app.cbo.oidc.java.server.backends.SessionsDB;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.User;
import com.sun.net.httpserver.HttpExchange;

import java.util.Arrays;
import java.util.Optional;

public class SessionHelper {

    public static final String SESSION_ID_COOKIE_NAME = "sessionId";

    public static SessionId createSession(HttpExchange exchange, User user) {
        //response;
        //https://developer.mozilla.org/fr/docs/Web/HTTP/Headers/Set-Cookie
        //https://developer.mozilla.org/fr/docs/Web/HTTP/Headers/Set-Cookie
        //=> Set-Cookie:sessionId=123; Secure; Path=/

        var sessionId = SessionsDB.getInstance().createSession(user);

        exchange.getResponseHeaders().add("Set-Cookie",SESSION_ID_COOKIE_NAME+"="+sessionId.getSessionId()+"; Secure; Path=/");
        return sessionId;
    }

    public static Optional<SessionId> findSessionId(HttpExchange exchange){

        //request;
        //https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cookie
        //Cookie: name=value; name2=value2; name3=value3


        var cookieHeaders = exchange.getRequestHeaders().get("Cookie");
        var sessionIdCookie = cookieHeaders.stream()
                .map(fullString ->{ //foreahc header with name 'Cookie'
                    return Arrays.stream( // "name; sessionId=value2; name3=value3"
                            fullString.split(";")) // {"name"," sessionId=value2", "name3=value3"}
                            .map(s  -> s.split("="))  //{{"name"}, {" sessionId","value2}", {" name3","value3"}}
                            .filter(kv -> kv.length==2) //{{" sessionId","value2}", {" name3","value3"}}
                            .map(kv -> new Pair<>(kv[0].trim(), kv[1].trim())) // { [l:"sessionId"/r:"value2"], [l:"name",r:"value3]}
                            .filter(pair -> SESSION_ID_COOKIE_NAME.equals(pair.left())) //{ [l:"sessionId"/r:"value2"] }
                            .map(Pair::right) // {"value2"}
                            .findAny();
                } )
                .filter(Optional::isPresent)//only the Cookie headers containing a cookie with name "sessionId"
                .map(Optional::get)
                .findFirst();

        if(sessionIdCookie.isPresent()) {
            return Optional.of(sessionIdCookie::get);
        }else {
            return Optional.empty();
        }
    }
}
