package app.cbo.oidc.java.server.utils;

import app.cbo.oidc.java.server.backends.sessions.Sessions;
import app.cbo.oidc.java.server.datastored.SessionId;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

public class Cookies {

    /**
     * A cookie, and its value
     * A COOKIE WITH NO VALUE WILL HAVE THE EMPTY STRING AS VALUE
     */
    public static record Cookie(String name, String value){

        public Cookie(String key) {
            this(key, null);
        }
        public Cookie(String name, String value) {
            this.name = name.trim();
            this.value = value != null ? value.trim():"";
        }

    }

    /**
     * Find the first cookie with name "sessionId" and a filled value in the given list
     * @param cookies a list aof cookie
     * @return the value of the cookie if found, empty if not
     */
    public static Optional<SessionId> findSessionCookie(Collection<Cookie> cookies){
        for(Cookie e : cookies){
            if(Sessions.SESSION_ID_COOKIE_NAME.equals(e.name()) && !Utils.isBlank(e.value())){
                return Optional.of(e::value);
            }
        }
        return Optional.empty();
    }

    /**
     * Read the content of the "Cookie" header and returns its content as a list of cookies
     * @param exchange inbound http request
     * @return list of cookie sent with the request
     */
    public static Collection<Cookie> parseCookies(HttpExchange exchange) {
        return parseCookies( exchange.getRequestHeaders().get("Cookie"));

    }

    /**
     * Transforms the content of the Cookie Header to a list of cookie
     * @param cookieStrings all values of the Cookie Header. I don't think we will ever have more than one...
     * @return list of cookie sent with the request
     */
    static List<Cookie> parseCookies(List<String> cookieStrings) {
        if(cookieStrings == null)
            return Collections.emptyList();

        List<Cookie> all = new ArrayList<>();
        cookieStrings.stream()
                .map(Cookies::parseCookies)
                .forEach(all::addAll);

        return Collections.unmodifiableList(all);
    }


    /**
     * Transforms the content of the Cookie Header to a list of cookie
     * @param cookieString ONE value of the Cookie Header. I don't think we will ever have more than one...
     * @return list of cookie sent with the request
     */
    static Collection<Cookie> parseCookies(String cookieString)  {

        var result = new ArrayList<Cookie>();
        String[] cookiePairs = cookieString.split("; ");
        for (String cookiePair : cookiePairs) {
            String[] cookieValue = cookiePair.split("=");
            if (cookieValue.length == 1)
                result.add(new Cookie(cookieValue[0]));
            else
                result.add(new Cookie(cookieValue[0], cookieValue[1]));
        }
        return Collections.unmodifiableList(result);
    }


}
