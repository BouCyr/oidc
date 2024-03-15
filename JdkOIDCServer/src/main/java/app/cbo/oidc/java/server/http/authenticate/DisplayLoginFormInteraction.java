package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DisplayLoginFormInteraction implements Interaction {

    private final String payload;

    public DisplayLoginFormInteraction(String ongoingAuthId) {


        this.payload = """
                <!DOCTYPE html>
                                <html>
                                <head>
                                    <title>PLease don't lie, I did not invest much in security</title>
                                    <link rel="icon" type="image/x-icon" href="/sc/fav.svg">
                                    <link href="/sc/clean.css" rel="stylesheet">
                                </head>
                                <body class="">


                                <div class="FORM ">
                                    <h1>WELCOME</h1>
                                    <form action="/login" class="container" method="POST">
                                        <input name="ongoing" type="hidden" value="%s"/>
                                        <input id="totp" name="totp" type="hidden">
                                        <label autocomplete="off" for="login">Username</label>
                                        <input id="login" name="login"/>
                                        <label for="pwd">Password</label>
                                        <input id="pwd" name="pwd" type="password"/>
                                        <label for="totp1">TOTP</label>

                                        <input id="totp1" max="9" maxlength="1" min="0" name="totp1" oninput="totpinput(this);" step="1" totpIdx="1"
                                               type="number"/>
                                        <input id="totp2" max="9" maxlength="1" min="0" name="totp2" oninput="totpinput(this);" step="1" totpIdx="2"
                       type="number"/>
                <input id="totp3" max="9" maxlength="1" min="0" name="totp3" oninput="totpinput(this);" step="1" totpIdx="3"
                       type="number"/>
                <input id="totp4" max="9" maxlength="1" min="0" name="totp4" oninput="totpinput(this);" step="1" totpIdx="4"
                       type="number"/>
                <input id="totp5" max="9" maxlength="1" min="0" name="totp5" oninput="totpinput(this);" step="1" totpIdx="5"
                       type="number"/>
                <input id="totp6" max="9" maxlength="1" min="0" name="totp6" oninput="totpinput(this);" step="1" totpIdx="6"
                       type="number"/>

                                        <input class="submit" type="submit" value="Go on"/>
                </form>
                </div>

                        <script>

                function totpinput(e){
                    if (e.value && e.value.length > e.maxLength)
                        e.value = e.value.slice(0, e.maxLength);
                    if(e.value && e.value.length >0 && totp(e) < 6){
                    nextNumber(totp(e)+1);
                    } else if((!e.value || e.value.length == 0) && totp(e) > 1) {
                        nextNumber(totp(e)-1);
                    }
                    compute();
                }

                function compute(){
                    const totpfield = document.getElementById("totp");
                    totpfield.value =
                    document.getElementById("totp1").value
                        + document.getElementById("totp2").value
                        + document.getElementById("totp3").value
                        + document.getElementById("totp4").value
                        + document.getElementById("totp5").value
                        + document.getElementById("totp6").value;
                }


                function totp(e) {
                    return Number.parseInt(e.getAttribute("totpIdx"));
                }
                function nextNumber(eltToFocus){
                    const id = "totp"+eltToFocus;
                    document.getElementById(id).focus();
                }
                        </script>
                    </body>
                </html>
                 """
                .formatted(ongoingAuthId);


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
