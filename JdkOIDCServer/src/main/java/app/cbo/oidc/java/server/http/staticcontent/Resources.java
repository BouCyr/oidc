package app.cbo.oidc.java.server.http.staticcontent;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

class Resources {

    private final static Map<String, Supplier<InputStream>> dataByFilename;

    static {
        dataByFilename = new HashMap<>();
        dataByFilename.put("clean.css", () -> fromString(Data.CSS));
        dataByFilename.put("fav.svg", () -> fromString(Data.FAVICO));
    }

    public static Optional<Supplier<InputStream>> getResource(String fileName) {

        if (dataByFilename.containsKey(fileName)) {
            return Optional.of(dataByFilename.get(fileName));
        } else {
            return Optional.empty();
        }
    }

    public static InputStream fromString(String str) {
        return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
    }
}
