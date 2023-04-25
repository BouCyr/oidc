package app.cbo.oidc.java.server.json;


import app.cbo.oidc.java.server.jsr305.NotNull;

public class JSON {

    /**
     * Writes an object in JSON form (sufficient impl for my need)
     * <p>
     * will print ALL methods without parameters that does not return void
     * field names will be the method name (with the 'get' suffix removed if present)
     * <p>
     * Numbers and String are OK ; pretty sure other base type will do something weird
     * Collections will be written as a json array
     * Maps will be written as sub objects, with the key as field names. If key is not a string, it will probably crash
     * array are not supported, and will probably crash
     * not tested with java inheritance, but reflection magic may cause crash
     * <p>
     * i.e. do not reuse this code :)
     */
    @NotNull
    public static String jsonify(@NotNull Object o) {
        return JSONWriter.writeIndented(o);
    }
}