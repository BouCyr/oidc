package app.cbo.oidc.java.server.json;


import app.cbo.oidc.java.server.jsr305.NotNull;

public class JSON {


    @NotNull
    public static String jsonify(@NotNull Object o) {
        return JSONWriter.writeIndented(o);
    }

    @NotNull
    public static String jsonifyOneline(@NotNull Object o) {
        return JSONWriter.write(o, () -> "");
    }
}
