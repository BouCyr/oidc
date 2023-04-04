package app.cbo.oidc.java.server.utils;

import app.cbo.oidc.java.server.datastored.SessionId;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

public class Cookies {

    public static record Cookie(String name, String value){

        public Cookie(String key) {
            this(key, null);
        }
        public Cookie(String name, String value) {
            this.name = name.trim();
            this.value = value != null ? value.trim():"";
        }

    }
    public static Collection<Cookie> parseCookies(HttpExchange exchange) {

        var cookieStrings = exchange.getRequestHeaders().get("Cookie");
        if(cookieStrings == null)
            return Collections.emptyList();

        List<Cookie> all = new ArrayList<>();
        cookieStrings.stream()
                .map(Cookies::parseCookies)
                .forEach(all::addAll);

        return Collections.unmodifiableList(all);

    }


    public static Collection<Cookie> parseCookies(String cookieString)  {

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

    public static Optional<SessionId> findSessionCookie(Collection<Cookie> cookies){
        for(Cookie e : cookies){
            if("sessionId".equals(e.name()) && !Utils.isBlank(e.value())){
                return Optional.of(e::value);
            }
        }
        return Optional.empty();

    }
}
