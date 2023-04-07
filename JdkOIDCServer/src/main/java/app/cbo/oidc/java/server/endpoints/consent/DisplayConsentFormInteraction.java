package app.cbo.oidc.java.server.endpoints.consent;

import app.cbo.oidc.java.server.backends.OngoingAuths;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class DisplayConsentFormInteraction implements Interaction {

    private final String payload;

    public DisplayConsentFormInteraction(AuthorizeParams authorizeParams,
                                         Set<String> consentsAlreadyGiven) {

        var ongoingAuthId = OngoingAuths.getInstance().store(authorizeParams);
        StringBuilder html = new StringBuilder();

        var br = System.lineSeparator();
        //shoot me I'm in a hurry
        html
                .append("<!DOCTYPE html>").append(br)
                .append("<html>").append(br)
                .append("<head>").append(br)
                .append("    <title>You okay with this ?</title>").append(br)
                .append("    <link rel='icon' type='image/x-icon' href='/sc/favicon.ico'>").append(br)
                .append("    <link href='/sc/pretty.css' rel='stylesheet'>").append(br)
                .append("</head>").append(br)
                .append("<body>").append(br)
                .append("    <form class='container' method='POST' action='").append(ConsentHandler.CONSENT_ENPOINT).append("'>").append(br)
                .append("        <input type='hidden' name='")
                .append(ConsentParams.ONGOING)
                .append("' value='").append(ongoingAuthId.getOngoingAuthId())
                .append("' />").append(br)
                .append("        <input type='hidden' name='").append(ConsentParams.BACK).append("' value='true' />").append(br)
                .append("        <div class='Title'><h1>").append(authorizeParams.clientId()).append(" want to access this data about you :</h1></div>").append(br)
                .append("        <div class='Fields'>").append(br)
                .append("            <table>").append(br);


        authorizeParams.scopes().forEach(consent ->
                html
                        .append("                <tr>").append(br)
                        .append("                    <td><label for='scope_").append(consent).append("' >").append(consent).append("</label></td>")
                        .append(br)
                        .append("                    <td><input type='checkbox' name='scope_").append(consent).append("' id='scope_").append(consent).append("' ></td>")
                        .append(br)
                        .append("                </tr>").append(br)
        );

        html
                .append("                <tr>").append(br)
                .append("                    <td colspan='2' class='submitRow'>").append(br)
                .append("                        <input type='submit' class='submit' value='Go on'/>").append(br)
                .append("                    </td>").append(br)
                .append("                </tr>").append(br)
                .append("            </table>").append(br)
                .append("        </div>").append(br)
                .append("    </form>").append(br)
                .append("</body>").append(br)
                .append("</html>").append(br);

        this.payload = html.toString();


    }

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {

        exchange.sendResponseHeaders(HttpCode.OK.code(), payload.getBytes(StandardCharsets.UTF_8).length);
        try (var os = exchange.getResponseBody()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

    }
}
