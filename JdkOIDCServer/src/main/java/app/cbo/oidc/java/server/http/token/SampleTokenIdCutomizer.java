package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.oidc.tokens.IdToken;

import java.util.HashMap;
import java.util.UUID;


/**
 * This class is a sample of how to customize the id token.
 * Put @Injectable("someProfile") and startup the server to see the result.
 */
public class SampleTokenIdCutomizer implements IdTokenCustomizer{


    @Override
    public IdToken customize(IdToken source) {

        var customized = new IdToken(
                UUID.randomUUID().toString(),
                source.iss(),
                source.aud(),
                source.exp(),
                source.iat(),
                source.auth_time(),
                source.nonce(),
                "1",
                source.amr(), //bof
                source.azp(),
                new HashMap<>()
        );

        customized.extranodes().put("nbf", 0);
        customized.extranodes().put("session_state", "149c811f-d1c4-4048-b23d-9f57fe4e31f0");
        customized.extranodes().put("email", "j.c.dow@example.com");
        customized.extranodes().put("preferred_username", "screenName");
        return customized;

    }
}
