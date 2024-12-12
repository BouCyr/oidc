package app.cbo.oidc.java.server.backends.tokens;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.json.JsonProcessingException;
import app.cbo.oidc.java.server.jwt.JWSPayloadData;
import app.cbo.oidc.java.server.oidc.Issuer;
import app.cbo.oidc.java.server.utils.Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

public record JWTAccessToken(
        String iss,
        String clientId,
        String sub,
        long iat,
        long nbf,
        long exp,
        String aud,
        String jti,
        Collection<String> scopes) implements JWSPayloadData {

    public JWTAccessToken(Issuer iss,
                          ClientId clientId,
                          UserId sub,
                          long exp,
                          String aud,
                          Collection<String> scopes) {
        this(
                iss.id(),
                clientId.id(),
                sub.id(),
                Instant.now().getEpochSecond(),
                Instant.now().getEpochSecond(),
                exp,
                aud,
                UUID.randomUUID().toString(),
                scopes);
    }

    public JWTAccessToken(Issuer iss,
                          ClientId clientId,
                          UserId sub,
                          Duration ttl,
                          String aud,
                          Collection<String> scopes) {
        this(
                iss, clientId, sub, Instant.now().plus(ttl).getEpochSecond(), aud, scopes);
    }


    /**
     * Reads an AccessOrRefreshToken from a json string
     *
     * @param json the json string
     * @return the parsed AccessOrRefreshToken
     */
    public static JWTAccessToken fromJson(String json) {

        //much quicker to write an ad-hoc parser than a generic json parser.
        //this code is shameful, not reusable, but kind of work
        try {


            final var issuerField = "iss";
            Stream.of(issuerField, "sub", "aud", "exp")
                    .map(k -> "\"" + k + "\":")// FIXME messy - if the key appears in a value, this check will return a false positive ?!
                    .filter(k -> !json.contains(k))
                    .findAny().ifPresent(k -> {
                        throw new JsonProcessingException(new IllegalArgumentException("Key '" + k + "' not present"));
                    });
            // scopes are NOT required

            String iss, sub, aud, clientId;
            long exp;
            List<String> scopes;

            Function<String, String> quotedStringMapper = (quoted) -> quoted.substring(1, quoted.length() - 1);//remove the "'"

            iss = extractJsonField("iss", json, quotedStringMapper);
            exp = extractJsonField("exp", json, Long::parseLong);
            sub = extractJsonField("sub", json, quotedStringMapper);
            aud = extractJsonField("aud", json, quotedStringMapper);
            clientId = extractJsonField("client_id", json, quotedStringMapper);

            // scopes are NOT required

            // only array field AFAIK, could be made to a generic method if needed
            if (json.contains("\"scopes\":")) {
                var scopesBegin = json.indexOf("\"scopes\":") + "\"scopes\":".length();
                var scopesEnd = 1 + json.indexOf("]", scopesBegin);
                var scopesValue = json.substring(scopesBegin, scopesEnd).trim();
                scopesValue = scopesValue.substring(1, scopesValue.length() - 1); //remove the [ and the ]

                scopes = Stream.of(scopesValue.split(","))
                        .map(String::trim)
                        .filter(s -> !Utils.isBlank(s))
                        .map(s -> s.substring(1, s.length() - 1))//remove the '"'
                        .toList();
            } else {
                scopes = Collections.emptyList();
            }


            return new JWTAccessToken(
                    Issuer.of(iss),
                    ClientId.of(clientId),
                    UserId.of(sub),
                    exp,
                    aud,
                    scopes);
        } catch (Exception e) {
            throw new JsonProcessingException(e);
        }
    }

    private static <U> U extractJsonField(String issuerField, String json, Function<String, U> extractor) {

        var fieldBegin = json.indexOf("\"" + issuerField + "\":") + ("\"" + issuerField + "\":").length();
        var fieldEnd = Stream.of(json.indexOf(",", fieldBegin), json.indexOf("}", fieldBegin)).filter(i -> i != -1).mapToInt(i -> i).min().orElseThrow(() -> new JsonProcessingException(new IllegalArgumentException("no iss key")));
        var stringValue = json.substring(fieldBegin, fieldEnd).trim();
        return extractor.apply(stringValue);

    }

}

