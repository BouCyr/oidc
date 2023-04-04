package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.utils.Pair;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Stream;

public record StartupArgs(int port) {

    private final static Set<String> ARGS = Set.of("port");

    public StartupArgs(String port) {
        this(Integer.parseInt(port));
    }

    public static StartupArgs from(String... array) {

        //TODO [15/03/2023] case port=6=5
        //TODO [15/03/2023] print help if invalid args...

        final var asMap = new HashMap<String, String>();
        Stream.of(array)
                .map(s -> s.split("="))
                .map(split -> Pair.of(split[0], split[1]))
                .filter(kv -> ARGS.contains(kv.left()))
                .forEach(kv -> asMap.put(kv.left(), kv.right()));

        return new StartupArgs(asMap.getOrDefault("port", "9451"));


    }

}
