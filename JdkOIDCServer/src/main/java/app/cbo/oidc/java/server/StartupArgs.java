package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.utils.Pair;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Stream;

public record StartupArgs(int port, boolean fsBackEnd, Path basePath) {

    public static final String PORT_ARGS = "port";
    public static final String BACKEND_ARGS = "backend";
    private final static Set<String> ARGS = Set.of(PORT_ARGS, BACKEND_ARGS);

    public StartupArgs(String port, String backend) {
        this(Integer.parseInt(port), !"mem".equals(backend), Path.of("."));
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

        return new StartupArgs(
                asMap.getOrDefault(PORT_ARGS, "9451"),
                asMap.getOrDefault(BACKEND_ARGS, "file")
        );


    }

}
