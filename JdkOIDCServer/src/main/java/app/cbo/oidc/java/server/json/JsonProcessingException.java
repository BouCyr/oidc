package app.cbo.oidc.java.server.json;

public class JsonProcessingException extends RuntimeException {
    public JsonProcessingException(Exception e) {
        super(e.getMessage(), e);
    }

    public JsonProcessingException(String msg) {
        super(msg);
    }
}
