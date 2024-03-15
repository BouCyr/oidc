package app.cbo.oidc.java.server.scan.props;

import app.cbo.oidc.java.server.utils.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class PropsProviders {

    private PropsProviders() {
    }

    public static List<Pair<String, String>> fromArgs(String... args) {

        return Stream.of(args)
                .filter(arg -> arg.contains("="))
                .map(PropsProviders::toPair)
                .toList();

    }

    public static List<Pair<String, String>> fromFile(Path propertyFile) {

        try (var lines = Files.lines(propertyFile)) {


            return lines
                    .map(String::trim)
                    .filter(line -> !line.startsWith("#"))
                    .filter(line -> line.contains("="))
                    .map(PropsProviders::toPair)
                    .toList();
        } catch (IOException e) {
            throw new IllegalArgumentException("File is not valid", e);
        }
    }

    private static Pair<String, String> toPair(String arg) {
        var parts = arg.split("=");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Not a valid prop arg: '" + parts + "'");
        }
        return Pair.of(parts[0].trim(), parts[1].trim());
    }

}



