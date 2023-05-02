package app.cbo.oidc.java.server.utils;

public enum HttpCode {

    OK(200),
    CREATED(201),
    FOUND(302),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    SERVER_ERROR(500),
    UNAVAILABLE(503);


    private final int code;

    public int code() {
        return code;
    }

    HttpCode(int code) {
        this.code = code;
    }
}
