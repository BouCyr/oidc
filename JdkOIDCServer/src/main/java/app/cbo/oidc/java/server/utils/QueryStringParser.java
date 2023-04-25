package app.cbo.oidc.java.server.utils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QueryStringParser {

    private QueryStringParser() { /*static method*/}


    public static Map<String, Collection<String>> from(String decodedQueryString) {

        if (decodedQueryString == null || decodedQueryString.isBlank()) {
            return Collections.emptyMap();
        }

        var params = Pattern.compile("&")
                .splitAsStream(decodedQueryString)
                .map(QueryStringParser::toKV)
                .collect(Collectors.toList());

        final Map<String, Collection<String>> result = new HashMap<>();
        params
                .forEach(param -> result.computeIfAbsent(param.left(), k -> new ArrayList<>())
                        .add(param.right()));
        return result;
    }

    private static Pair<String, String> toKV(String param) {

        int sepPosition = param.indexOf('=');
        if (sepPosition == -1) {
            return Pair.of(param, "");
        } else {
            return Pair.of(
                    param.substring(0, sepPosition),
                    param.substring(sepPosition + 1)
            );
        }
    }
}
