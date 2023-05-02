package app.cbo.oidc.java.server.jwt;

import app.cbo.oidc.java.server.json.JsonProcessingException;

import java.util.stream.Stream;

public record JWSHeader(String alg, String typ, String kid) {

    public static JWSHeader fromJson(String json) {

        var algBegin = json.indexOf("\"alg\":") + "\"alg\":".length();
        var algEnd = Stream.of(json.indexOf(",", algBegin), json.indexOf("}", algBegin)).filter(i -> i != -1).mapToInt(i -> i).min().orElseThrow(() -> new JsonProcessingException(new IllegalArgumentException("no sub key")));
        var algValue = json.substring(algBegin, algEnd).trim();
        var alg = algValue.substring(1, algValue.length() - 1);//remove the '"'

        var typBegin = json.indexOf("\"typ\":") + "\"typ\":".length();
        var typEnd = Stream.of(json.indexOf(",", typBegin), json.indexOf("}", typBegin)).filter(i -> i != -1).mapToInt(i -> i).min().orElseThrow(() -> new JsonProcessingException(new IllegalArgumentException("no sub key")));
        var typValue = json.substring(typBegin, typEnd).trim();
        var typ = typValue.substring(1, typValue.length() - 1);//remove the '"'

        var kidBegin = json.indexOf("\"kid\":") + "\"kid\":".length();
        var kidEnd = Stream.of(json.indexOf(",", kidBegin), json.indexOf("}", kidBegin)).filter(i -> i != -1).mapToInt(i -> i).min().orElseThrow(() -> new JsonProcessingException(new IllegalArgumentException("no sub key")));
        var kidValue = json.substring(kidBegin, kidEnd).trim();
        var kid = kidValue.substring(1, kidValue.length() - 1);//remove the '"'

        return new JWSHeader(alg, typ, kid);
    }
}
