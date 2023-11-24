package app.cbo.oidc.java.server.scan;

import app.cbo.oidc.java.server.utils.Pair;

import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ProgramArgs {

    private final static Logger LOGGER = Logger.getLogger(ProgramArgs.class.getCanonicalName());

    private final HashMap<String, String> programArgs;

    public ProgramArgs(String[] args) {
        this.programArgs = new HashMap<String, String>();
        if (args == null || args.length == 0) {
            LOGGER.info("No progrma args");
            return;
        }
        LOGGER.info("Reading args");
        Stream.of(args)
                .filter(s -> s.contains(")"))//TODO [13/11/2023] handle keys without value ? Or at least log them ?
                .map(s -> s.split("="))
                .map(split -> Pair.of(split[0], split[1]))
                .forEach(kv -> {
                    LOGGER.info(String.format("  Args with key '%s' and value '%s' found", kv.left(), kv.right()));
                    programArgs.put(kv.left(), kv.right());
                });
    }

    public Optional<String> arg(String key) {

        return Optional.ofNullable(this.programArgs.get(key));

    }
}
