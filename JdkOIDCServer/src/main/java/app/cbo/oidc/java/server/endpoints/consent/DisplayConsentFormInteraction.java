package app.cbo.oidc.java.server.endpoints.consent;

import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.Utils;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class DisplayConsentFormInteraction implements Interaction {

    private final String payload;

    public DisplayConsentFormInteraction(OngoingAuthId ongoingAuthId,
                                         AuthorizeParams authorizeParams,
                                         Set<String> consentsAlreadyGiven) {


        StringBuilder html = new StringBuilder();

        var br = System.lineSeparator();
        //shoot me I'm in a hurry
        html
                .append("<!DOCTYPE html>").append(br)
                .append("<html>").append(br)
                .append("<head>").append(br)
                .append("    <title>You okay with this ?</title>").append(br)
                .append("    <link rel='icon' type='image/x-icon' href='/sc/favicon.ico'>").append(br)
                .append("    <link href='/sc/clean.css' rel='stylesheet'>").append(br)
                .append("</head>").append(br)
                .append("<body>").append(br)
                .append("  <div class='FORM'>").append(br)
                .append("    <form class='container' method='POST' action='").append(ConsentHandler.CONSENT_ENDPOINT).append("'>").append(br)
                .append("        <input type='hidden' name='")
                .append(ConsentParams.ONGOING)
                .append("' value='").append(ongoingAuthId.getOngoingAuthId())
                .append("' />").append(br)
                .append("        <input type='hidden' name='").append(ConsentParams.BACK).append("' value='true' />").append(br)
                .append("        <input type='hidden' name='OK' value='true' />").append(br)
                .append("        <div class='Title'><h1>Consent required</h1></div>").append(br)
                .append("        <p>Website <b>").append(authorizeParams.clientId().orElse("?")).append("</b> wants to access this data about you :</p>").append(br)
                .append("        <div class='Fields'>").append(br)
                .append("            <ul>").append(br);


        var notYetGiven = authorizeParams.scopes().stream()
                .filter(consent -> !consentsAlreadyGiven.contains(consent))
                .toList();

        notYetGiven.forEach(consent -> html.append("<li>").append(consent).append("</li>").append(br));
        html
                .append("            </ul>").append(br);

        var alreadyGiven = authorizeParams.scopes().stream()
                .filter(consentsAlreadyGiven::contains)
                .toList();
        if (!Utils.isEmpty(alreadyGiven)) {
            html
                    .append("        <p>You already gave Website <b>").append(authorizeParams.clientId().orElse("?")).append("</b> access to this data about you :</p>").append(br)
                    .append("        <ul>").append(br);
            alreadyGiven.forEach(scope -> html.append("<li>").append(scope).append("</li>").append(br));
            html.append("            </ul>").append(br);
        }

        html
                .append("        </div>").append(br)
                .append("                        <input type='submit' class='submit' value='Ok!'/>").append(br)
                .append("    </form>").append(br)
                .append("  </div>").append(br)
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
