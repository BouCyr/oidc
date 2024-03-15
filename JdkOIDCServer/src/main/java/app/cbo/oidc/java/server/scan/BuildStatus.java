package app.cbo.oidc.java.server.scan;

public enum BuildStatus {
    NONE, //[09/11/2023]should not be used ?
    CANCELLED, //[09/11/2023] Maybe for classes in a disabled profile ?
    BUILDING,
    BUILT
}
