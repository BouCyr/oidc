package app.cbo.oidc.java.server.endpoints.authenticate;

import app.cbo.oidc.java.server.endpoints.HTMLInteraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class DisplayLoginFormInteraction extends HTMLInteraction {
    public DisplayLoginFormInteraction(InputStream htmlTemplate, Map<String, String> templatedValues) throws IOException {
        super(htmlTemplate, templatedValues);
    }
}
