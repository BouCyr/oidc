package app.cbo.oidc.java.server.utils;

// [03/10/2024] I think this class is ALWAYS used for properties.
// TODO [03/10/2024] Remove generics data, rename KeyValue or ValuedProperty
public record Pair<L, R>(L left, R right) {

    public static Pair<String, String> of(String left, String right) {
        return new Pair<>(left, right);
    }
}
