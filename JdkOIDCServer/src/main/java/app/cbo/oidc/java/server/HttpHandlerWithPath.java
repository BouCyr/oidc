package app.cbo.oidc.java.server;

import com.sun.net.httpserver.HttpHandler;

public interface HttpHandlerWithPath extends HttpHandler {

    String path();

}
