package app.cbo.oidc.java.server.oidc.tokens;

import app.cbo.oidc.java.server.json.JsonProcessingException;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.Collection;
import java.util.stream.Stream;

public record AccessOrRefreshToken(String typ, String sub, long exp, Collection<String> scopes) {

    public static final String TYPE_ACCESS = "at";
    public static final String TYPE_REFRESH = "rt";


    /**
     * Reads an AccessOrRefreshToken from a json string
     *
     * @param json the json string
     * @return the parsed AccessOrRefreshToken
     */
    public static AccessOrRefreshToken fromJson(String json) {

        //much quicker to write an ad-hoc parser than a generic json parser.
        //this code is shameful, not reusable, but kind of work
        try {


            Stream.of("typ", "sub", "scopes", "exp")
                    .filter(k -> !json.contains(k))
                    .findAny().ifPresent(k -> {
                throw new JsonProcessingException(new IllegalArgumentException("Key '" + k + "' not present"));
            });

            var expBegin = json.indexOf("\"exp\":") + "\"exp\":".length();
            var expEnd = Stream.of(json.indexOf(",", expBegin), json.indexOf("}", expBegin)).filter(i -> i != -1).mapToInt(i -> i).min().orElseThrow(() -> new JsonProcessingException(new IllegalArgumentException("no exp key")));
            var expValue = json.substring(expBegin, expEnd).trim();
            var exp = Long.parseLong(expValue);

            var subBegin = json.indexOf("\"sub\":") + "\"sub\":".length();
            var subEnd = Stream.of(json.indexOf(",", subBegin), json.indexOf("}", subBegin)).filter(i -> i != -1).mapToInt(i -> i).min().orElseThrow(() -> new JsonProcessingException(new IllegalArgumentException("no sub key")));
            var subValue = json.substring(subBegin, subEnd).trim();
            var sub = subValue.substring(1, subValue.length() - 1);//remove the '"'

            var typBegin = json.indexOf("\"typ\":") + "\"typ\":".length();
            var typEnd = Stream.of(json.indexOf(",", typBegin), json.indexOf("}", typBegin)).filter(i -> i != -1).mapToInt(i -> i).min().orElseThrow(() -> new JsonProcessingException(new IllegalArgumentException("no typ key")));
            var typValue = json.substring(typBegin, typEnd).trim();
            var typ = typValue.substring(1, typValue.length() - 1);//remove the '"'
            // ;
            var scopesBegin = json.indexOf("\"scopes\":") + "\"scopes\":".length();
            var scopesEnd = 1 + json.indexOf("]", scopesBegin);
            var scopesValue = json.substring(scopesBegin, scopesEnd).trim();
            scopesValue = scopesValue.substring(1, scopesValue.length() - 1); //remove the [ and the ]

            var scopes = Stream.of(scopesValue.split(","))
                    .map(String::trim)
                    .filter(s -> !Utils.isBlank(s))
                    .map(s -> s.substring(1, s.length() - 1))//remove the '"'
                    .toList();

            return new AccessOrRefreshToken(
                    typ,
                    sub,
                    exp,
                    scopes);
        } catch (Exception e) {
            throw new JsonProcessingException(e);
        }
    }

}
