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
        params.forEach(param -> {
            result.computeIfAbsent(param.left(), k -> new ArrayList<>())
                    .add(param.right());
        });
        return result;
    }

    private static Pair<String, String> toKV(String param) {

        int sepPosition = param.indexOf('=');
        if (sepPosition == -1) {
            return new Pair<>(param, "");
        } else {
            return new Pair<>(
                    param.substring(0, sepPosition),
                    param.substring(sepPosition + 1)
            );
        }


    }

    public static String toString(Map<String, Collection<String>> params) {
        var sb = new StringBuilder();
        params.forEach((k, v) -> {
            sb.append(k).append(":");
            if (v.isEmpty() || (v.size() == 1 && v.iterator().next().isEmpty())) {
                sb.append("[empty]").append(System.lineSeparator());
            } else if (v.size() == 1) {
                sb.append(v.iterator().next())
                        .append(System.lineSeparator());
            } else {
                sb.append(System.lineSeparator());
                v.stream().forEach(entry -> {
                    if (entry.isEmpty()) {
                        sb.append("  -[empty]").append(System.lineSeparator());
                    } else {
                        sb.append("  -").append(entry).append(System.lineSeparator());
                    }
                });
            }
        });
        String body = sb.toString();
        if (body.isBlank()) {
            body = "[empty]";
        }
        return body;
    }
}
