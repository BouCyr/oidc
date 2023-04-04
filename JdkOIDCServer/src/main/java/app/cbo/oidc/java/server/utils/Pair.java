package app.cbo.oidc.java.server.utils;

public record Pair<L, R>(L left, R right) {

    public static Pair<String, String> of(String left, String right) {
        return new Pair<>(left, right);
    }
}
