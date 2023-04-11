package app.cbo.oidc.java.server.json;

public class JSON {

    public static String jsonify(Object o) {
        return JSONWriter.write(o);
    }
}
