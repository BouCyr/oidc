package app.cbo.oidc.java.server.endpoints.consent;

import app.cbo.oidc.java.server.backends.Sessions;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import app.cbo.oidc.java.server.utils.Cookies;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;

public class ConsentHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Map<String, Collection<String>> raw= extractParams(exchange);
            var params = new ConsentParams(raw);

            var cookies = Cookies.parseCookies(exchange);
            var sessionId = Cookies.findSessionCookie(cookies);
            Optional<Session> session = sessionId.isEmpty()? Optional.empty(): Sessions.getInstance().getSession(sessionId.get());



        } catch (AuthErrorInteraction authError) {
            authError.handle(exchange);
            return;
        }catch(Exception e){
            new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "?").handle(exchange);
            return;
        }
    }
}
