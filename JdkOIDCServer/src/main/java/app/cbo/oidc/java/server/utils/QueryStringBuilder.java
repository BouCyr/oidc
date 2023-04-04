package app.cbo.oidc.java.server.utils;

public class QueryStringBuilder {

    private boolean has = false;
    private final StringBuilder builder=new StringBuilder();

    public QueryStringBuilder add(String fullParam) {
        if(has)
            builder.append("&");
        builder.append(fullParam);
        has=true;

        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
