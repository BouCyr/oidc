package app.cbo.oidc.java.server.backends.tokens;

import app.cbo.oidc.java.server.json.JsonProcessingException;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public record JWTAccessToken(String iss, String sub, long exp, String aud, Collection<String> scopes) {


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


            Stream.of("iss", "sub", "aud", "scopes", "exp")
                    .filter(k -> !json.contains(k))
                    .findAny().ifPresent(k -> {
                        throw new JsonProcessingException(new IllegalArgumentException("Key '" + k + "' not present"));
                    });

            String iss, sub, aud;
            long exp;
            List<String> scopes;

            {
                var issBegin = json.indexOf("\"iss\":") + "\"iss\":".length();
                var issEnd = Stream.of(json.indexOf(",", issBegin), json.indexOf("}", issBegin)).filter(i -> i != -1).mapToInt(i -> i).min().orElseThrow(() -> new JsonProcessingException(new IllegalArgumentException("no iss key")));
                var issValue = json.substring(issBegin, issEnd).trim();
                iss = issValue.substring(1, issValue.length() - 1);//remove the "'"
            }

            {
                var expBegin = json.indexOf("\"exp\":") + "\"exp\":".length();
                var expEnd = Stream.of(json.indexOf(",", expBegin), json.indexOf("}", expBegin)).filter(i -> i != -1).mapToInt(i -> i).min().orElseThrow(() -> new JsonProcessingException(new IllegalArgumentException("no exp key")));
                var expValue = json.substring(expBegin, expEnd).trim();
                exp = Long.parseLong(expValue);
            }
            {
                var subBegin = json.indexOf("\"sub\":") + "\"sub\":".length();
                var subEnd = Stream.of(json.indexOf(",", subBegin), json.indexOf("}", subBegin)).filter(i -> i != -1).mapToInt(i -> i).min().orElseThrow(() -> new JsonProcessingException(new IllegalArgumentException("no sub key")));
                var subValue = json.substring(subBegin, subEnd).trim();
                sub = subValue.substring(1, subValue.length() - 1);//remove the '"'
            }

            {
                var audBegin = json.indexOf("\"aud\":") + "\"aud\":".length();
                var audEnd = Stream.of(json.indexOf(",", audBegin), json.indexOf("}", audBegin)).filter(i -> i != -1).mapToInt(i -> i).min().orElseThrow(() -> new JsonProcessingException(new IllegalArgumentException("no sub key")));
                var audValue = json.substring(audBegin, audEnd).trim();
                aud = audValue.substring(1, audValue.length() - 1);//remove the '"'
            }

            {
                var scopesBegin = json.indexOf("\"scopes\":") + "\"scopes\":".length();
                var scopesEnd = 1 + json.indexOf("]", scopesBegin);
                var scopesValue = json.substring(scopesBegin, scopesEnd).trim();
                scopesValue = scopesValue.substring(1, scopesValue.length() - 1); //remove the [ and the ]

                scopes = Stream.of(scopesValue.split(","))
                        .map(String::trim)
                        .filter(s -> !Utils.isBlank(s))
                        .map(s -> s.substring(1, s.length() - 1))//remove the '"'
                        .toList();
            }
            return new JWTAccessToken(
                    iss,
                    sub,
                    exp,
                    aud,
                    scopes);
        } catch (Exception e) {
            throw new JsonProcessingException(e);
        }
    }

}

