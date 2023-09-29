package app.cbo.oidc.java.server.http.consent;

import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.http.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

public class DisplayConsentFormInteraction implements Interaction {

    private final String payload;

    public DisplayConsentFormInteraction(OngoingAuthId ongoingAuthId,
                                         AuthorizeParams authorizeParams,
                                         Set<String> consentsAlreadyGiven) {


        var br = System.lineSeparator();

        var notYetGiven = authorizeParams.scopes().stream()
                .filter(consent -> !consentsAlreadyGiven.contains(consent))
                .map(consent -> "<li>" + consent + "</li>" + br)
                .collect(Collectors.joining());

        var alreadyGiven = authorizeParams.scopes().stream()
                .filter(consentsAlreadyGiven::contains)
                .map(consent -> "<li>" + consent + "</li>" + br)
                .collect(Collectors.joining());


        var template =
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <title>You okay with this ?</title>
                            <link rel='icon' type='image/x-icon' href='/sc/fav.svg'>
                            <link href='/sc/clean.css' rel='stylesheet'>
                        </head>
                        <body>
                          <div class='FORM'>
                            <form class='container' method='POST' action='%s'> <!--CONSENT_ENDPOINT-->
                              <input type='hidden' name='%s' value='%s' /> <!--ONGOING / getOngoingAuthId -->
                              <input type='hidden' name='%s' value='true' /> <!-- ConsentParams.BACK -->
                              <input type='hidden' name='OK' value='true' />
                              <div class='Title'><h1>Consent required</h1></div>
                              <p>Website <b>%s</b> wants to access this data about you :</p> <!-- client_id -->
                              <ul> <!-- notYetGiven -->
                                %s
                              </ul>
                              <p>Website <b>%s</b> wants to access this data about you :</p> <!-- client_id -->
                              <ul> <!-- given -->
                                %s
                              </ul>
                              <input type='submit' class='submit' value='Ok!'/>
                            </form>
                          </div>
                        </body>
                        </html>
                        """.formatted(ConsentHandler.CONSENT_ENDPOINT,
                        ConsentParams.ONGOING, ongoingAuthId.getOngoingAuthId(),
                        ConsentParams.BACK,
                        authorizeParams.clientId().orElse("?"),
                        notYetGiven,
                        authorizeParams.clientId().orElse("?"),
                        alreadyGiven);


        this.payload = template;


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
