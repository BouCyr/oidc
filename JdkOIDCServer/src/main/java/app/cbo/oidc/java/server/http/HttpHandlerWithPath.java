package app.cbo.oidc.java.server.http;

import com.sun.net.httpserver.HttpHandler;

public interface HttpHandlerWithPath extends HttpHandler {

    String path();

}
